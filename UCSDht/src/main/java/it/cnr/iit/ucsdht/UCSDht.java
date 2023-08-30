package it.cnr.iit.ucsdht;

import com.google.gson.GsonBuilder;
import com.google.gson.typeadapters.RuntimeTypeAdapterFactory;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import it.cnr.iit.ucs.constants.STATUS;
import it.cnr.iit.ucs.pipreader.PIPReader;
import it.cnr.iit.ucs.properties.components.PepProperties;
import it.cnr.iit.ucs.properties.components.PipProperties;
import it.cnr.iit.ucs.sessionmanager.OnGoingAttribute;
import it.cnr.iit.ucs.sessionmanager.Session;
import it.cnr.iit.ucs.sessionmanager.SessionInterface;
import it.cnr.iit.ucs.sessionmanager.SessionManager;
import it.cnr.iit.ucsdht.json.Status;
import it.cnr.iit.ucsdht.json.StatusPersistent;
import it.cnr.iit.ucsdht.json.StatusRequestPostTopicUuid;
import it.cnr.iit.ucsdht.properties.*;
import it.cnr.iit.utility.JsonUtility;
import it.cnr.iit.utility.dht.DHTClient;
import it.cnr.iit.utility.dht.DHTPersistentMessageClient;
import it.cnr.iit.utility.dht.PersistUtility;
import it.cnr.iit.utility.dht.jsonpersistent.*;
import it.cnr.iit.xacml.Attribute;
import it.cnr.iit.xacml.Category;
import it.cnr.iit.xacml.DataType;
import it.cnr.iit.xacml.PolicyTags;
import it.cnr.iit.xacml.wrappers.PolicyWrapper;
import it.cnr.iit.xacml.wrappers.RequestWrapper;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

import static it.cnr.iit.utility.dht.DHTUtils.isDhtReachable;

public class UCSDht {
    static DHTClient dhtClientEndPoint;
    static UCSClient ucsClient;

    static final String COMMAND_TYPE = "ucs-command";
    static final String UCS_SUB_TOPIC_UUID = "topic-uuid-the-ucs-is-subscribed-to";

    static final String PAP_SUB_TOPIC_NAME = "topic-name-pap-is-subscribed-to";
    static final String PAP_SUB_TOPIC_UUID = "topic-uuid-pap-is-subscribed-to";

    static final String PIP_SUB_TOPIC_NAME = "topic-name-pip-is-subscribed-to";
    static final String PIP_SUB_TOPIC_UUID = "topic-uuid-pip-is-subscribed-to";
    static final File pipsDir = new File(Utils.getResourcePath(UCSDht.class), "pips");
    static final File policiesDir = new File(Utils.getResourcePath(UCSDht.class), "policies");
    static final File pepsDir = new File(Utils.getResourcePath(UCSDht.class), "peps");
    static final File databaseDir = new File(Utils.getResourcePath(UCSDht.class), "database");

    private static String dhtUri = "ws://localhost:3000/ws";
    private static final String dbUri = "jdbc:sqlite:" + databaseDir + File.separator + "database.db";


    static final List<PipProperties> pipPropertiesList = new ArrayList<>();
    static final UCSDhtPapProperties papProperties = new UCSDhtPapProperties(policiesDir.getAbsolutePath());
    static final UCSDhtSessionManagerProperties sessionManagerProperties = new UCSDhtSessionManagerProperties();
    static final List<PepProperties> pepPropertiesList = new ArrayList<>();

    static List<SessionInterface> sessionsWithStatusStartOrRevoke = new ArrayList<>();

    static boolean softReset = false;
    static boolean hardReset = false;

    static String PERSISTENT_TOPIC_NAME_UCS = "SIFIS:UCS";
    static String PERSISTENT_TOPIC_UUID_UCS_STATUS = "status";
    static DHTPersistentMessageClient client;

    private static final RuntimeTypeAdapterFactory<RequestPostTopicUuid> typeFactory = RuntimeTypeAdapterFactory
            .of(RequestPostTopicUuid.class, "topic_name")
            .registerSubtype(StatusRequestPostTopicUuid.class, PERSISTENT_TOPIC_NAME_UCS);

    private static final RuntimeTypeAdapterFactory<Persistent> persistentTypeFactory = RuntimeTypeAdapterFactory
            .of(Persistent.class, "topic_name")
            .registerSubtype(StatusPersistent.class, PERSISTENT_TOPIC_NAME_UCS);

    public static void main(String[] args) {

        System.out.println("Time: " + System.currentTimeMillis());

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--help":
                    printUsage();
                    System.exit(0);
                case "--dht":
                    URI parsed = null;
                    try {
                        parsed = new URI(args[i + 1]);
                    } catch (URISyntaxException | ArrayIndexOutOfBoundsException e) {
                        // No URI indicated
                        System.err.println("Invalid URI after --dht option\n");
                        printUsage();
                        System.exit(1);
                    }
                    dhtUri = parsed.toString();
                    i = i + 1;
                    break;
                case "--soft-reset":
                    // reset the SQL database only
                    softReset = true;
                    break;
                case "--hard-reset":
                    // reset the SQL database and remove all PIPs, PEPs, and policies
                    hardReset = true;
                    break;
                default:
                    System.err.println("Unknown option " + args[i] + "\n");
                    printUsage();
                    System.exit(1);
            }
        }

        client = new DHTPersistentMessageClient(dhtUri, PERSISTENT_TOPIC_NAME_UCS,
                PERSISTENT_TOPIC_UUID_UCS_STATUS, typeFactory, persistentTypeFactory);

        if (hardReset) {
            performHardReset();
        } else if (softReset) {
            performSoftReset();
        } else {
            reloadState();
        }

        try {
            initializeUCS();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

        System.out.println();
        System.out.println("UCS initialization complete");
        System.out.println("  Database directory: " + databaseDir.getAbsolutePath());
        System.out.println("  PIPs directory: " + pipsDir.getAbsolutePath());
        System.out.println("  PEPs directory: " + pepsDir.getAbsolutePath());
        System.out.println("  Policies directory: " + policiesDir.getAbsolutePath());
        System.out.println();

        try {
            dhtClientEndPoint = new DHTClient(new URI(dhtUri));
            dhtClientEndPoint.addMessageHandler(new DhtMessageHandler());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        StatusWatcher watcher = null;
        try {
            watcher = new StatusWatcher(Arrays.asList(
                    databaseDir.toPath(), pipsDir.toPath(), pepsDir.toPath(), policiesDir.toPath()));
            watcher.startMonitoring();
        } catch (IOException e) {
            if (watcher != null) {
                watcher.stopMonitoring();
            }
            throw new RuntimeException(e);
        }

        try {
            restorePIPsSubscriptions();
            reevaluateSessions();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

        System.out.println("Waiting for commands...");
        System.out.println();
    }

    public static void printUsage() {
        System.out.println(
                "Usage Control Engine\n" +
                "\n" +
                "Options:\n" +
                "--dht <dhtUri> The websocket URI of the DHT  \n" +
                "               (default: ws://localhost:3000/ws)\n" +
                "\n" +
                "--hard-reset   The UCS is initialized with a new database,\n" +
                "               all PIPs, PEPs, and policies are deleted.\n" +
                "               The new state is saved on the DHT right after \n" +
                "               the initialization.\n" +
                "\n" +
                "--soft-reset   The UCS is initialized with a new database,\n" +
                "               but all PIPs, PEPs, and policies are preserved.\n" +
                "               The new state is saved on the DHT right after \n" +
                "               the initialization.\n" +
                "\n" +
                "               If both --hard-reset and --soft-reset are specified,\n" +
                "               --hard-reset prevails.");

    }

    public static void performHardReset() {
        System.out.println("Performing hard reset...");
        // reset everything
        initializeDb();
        Utils.createDir(pipsDir);
        Utils.createDir(pepsDir);
        Utils.createDir(policiesDir);
        System.out.println("... hard reset performed");

        // save the new state to the dht
        uploadStatus();
    }

    public static void performSoftReset() {
        System.out.println("Performing soft reset...");
        // get the state from the dht
        Status status = downloadStatus();
        if (status == null) {
            System.out.print("No status found: ");
            // if there is no state
            //   perform hard reset
            performHardReset();
        } else {
            initializeDb();
            restorePips(status.getPips());
            restorePeps(status.getPeps());
            restorePolicies(status.getPolicies());
            System.out.println("... soft reset performed");

            // save the new state to the dht
            uploadStatus();
        }
    }

    public static void reloadState() {
        System.out.println("Reloading state...");
        // get the state from the dht
        Status status = downloadStatus();
        if (status == null) {
            System.out.print("No status found: ");
            // if there is no state
            //   perform hard reset
            performHardReset();
        } else {
            restoreDb(status.getDatabase());
            restorePips(status.getPips());
            restorePeps(status.getPeps());
            restorePolicies(status.getPolicies());
            System.out.println("... status reloaded");

            // do not save the new state to the dht since nothing changed
        }
    }


    /**
     * Create a new database file with empty tables
     */
    public static void initializeDb() {
        Utils.createDir(databaseDir);
        try (ConnectionSource connectionSource = new JdbcConnectionSource(dbUri)) {
            TableUtils.dropTable(connectionSource, Session.class, true);
            TableUtils.dropTable(connectionSource, OnGoingAttribute.class, true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * Create the database file using the string obtained from the dht.
     * Then, extract the sessions with status START and REVOKED to be used after
     * the UCS is initialized in order to restore pips subscriptions.
     * @param dbString the base64 string representing the database file
     */
    public static void restoreDb(String dbString) {
        // write the database to file
        Utils.createDir(databaseDir);
        PersistUtility.base64StringToFile(dbString, databaseDir + File.separator + "database.db");
        System.out.println("[DATABASE] Database correctly retrieved from the DHT and saved to file");

        // save the sessions with status START and REVOKE
        UCSDhtSessionManagerProperties smProp = new UCSDhtSessionManagerProperties();
        smProp.setDbUri(dbUri);
        SessionManager sessionManager = new SessionManager(smProp);
        sessionManager.start();
        sessionsWithStatusStartOrRevoke.addAll(sessionManager.getSessionsForStatus(STATUS.START.toString()));
        sessionsWithStatusStartOrRevoke.addAll(sessionManager.getSessionsForStatus(STATUS.REVOKE.toString()));
        sessionManager.stop();
        System.out.println("[DATABASE] Sessions with status START and REVOKED gathered. "
                + sessionsWithStatusStartOrRevoke.size() + " sessions found.");
        System.out.println();
    }


    /**
     * Create the pip properties files using the strings obtained from the dht.
     * Then, load the pip properties in memory to be used during the UCS initialization.
     * @param pipsString the list of base64 strings representing the pip properties files
     */
    public static void restorePips(List<String> pipsString) {
        // write pip folders and files
        Utils.createDir(pipsDir);
        for (String pipString : pipsString) {
            // extract pip id
            PipProperties pipProperties =
                    PersistUtility.getPipPropertiesFromBase64String(pipString, UCSDhtPipProperties.class);
            assert pipProperties != null;
            String pipId = pipProperties.getId();

            // create pip properties json file
            Utils.createDir(new File(pipsDir.getAbsolutePath(), pipId));
            PersistUtility.base64StringToFile(pipString, pipsDir + File.separator + pipId + File.separator + pipId + ".json");
            System.out.println("[PIPs] PIP '" + pipId + "' correctly retrieved from the DHT and saved to file");

            // for PIPReader, create the file(s) with the attribute value
            if (pipProperties.getName().equals(PIPReader.class.getName())) {
                for (Map<String, String> attribute : pipProperties.getAttributes()) {
                    String attributeFilePath = pipProperties.getAdditionalProperties().get(attribute.get("ATTRIBUTE_ID"));
                    String attributeValue = pipProperties.getAdditionalProperties().get(attributeFilePath);
                    setAttributeValue(pipsDir.getAbsolutePath() + File.separator + pipId + File.separator + attributeFilePath, attributeValue);
                    System.out.println("        - Attribute value for '" + attribute.get("ATTRIBUTE_ID")
                            + "' correctly saved to file '" + attributeFilePath + "'");
                }
            }
        }
        System.out.println();

        // load the pips' properties in memory from the json files
        loadPips();
    }


    /**
     * Create the pep properties files using the strings obtained from the dht.
     * Then, load the pep properties in memory to be used during the UCS initialization.
     * @param pepsString the list of base64 strings representing the pep properties files
     */
    public static void restorePeps(List<String> pepsString) {
        // write pep files
        Utils.createDir(pepsDir);
        for (String pepString : pepsString) {
            // extract pep id
            PepProperties pepProperties = PersistUtility.getPepPropertiesFromBase64String(pepString, UCSDhtPepProperties.class);
            assert pepProperties != null;
            String pepId = pepProperties.getId();

            // create pep properties json file
            PersistUtility.base64StringToFile(pepString, pepsDir + File.separator + pepId + ".json");
            System.out.println("[PEPs] PEP '" + pepId + "' correctly retrieved from the DHT and saved to file");
        }
        System.out.println();

        // load the peps' properties in memory from the json files
        loadPeps();
    }


    /**
     * Create the policies files using the strings obtained from the dht.
     * @param policiesString the list of base64 strings representing the policy files
     */
    public static void restorePolicies(List<String> policiesString) {
        // write policies files
        Utils.createDir(policiesDir);
        for (String policyString : policiesString) {
            // extract policy id
            String policyId = PersistUtility.getPolicyIdFromBase64String(policyString);

            // create policy file
            PersistUtility.base64StringToFile(policyString, policiesDir + File.separator + policyId + ".xml");
            System.out.println("[Policies] Policy '" + policyId + "' correctly retrieved from the DHT and saved to file");
        }
        System.out.println();
    }


    /**
     * Transform the database file into a string in order to be stored in the dht.
     * @return a base64 string representing the database, or null if the file does
     * not exist
     */
    public static String saveDbToString() {
        File targetFile = new File(databaseDir, "database.db");
        if (targetFile.exists() && targetFile.isFile()) {
            return PersistUtility.fileToBase64String(databaseDir + File.separator + "database.db");
        } else {
            return null;
        }
    }

    /**
     * Transform the pip properties json files into strings in order to be stored in the dht
     * @return a list of base64 strings representing the pip properties files
     */
    public static List<String> savePipsToString() {

        List<String> pipStrings = new ArrayList<>();

        if (pipsDir.exists() && pipsDir.isDirectory()) {
            File[] subDirs = pipsDir.listFiles(File::isDirectory);
            if (subDirs != null) {
                for (File subDir : subDirs) {
                    File targetFile = new File(subDir, subDir.getName() + ".json");
                    if (targetFile.exists() && targetFile.isFile()) {
                        pipStrings.add(PersistUtility.fileToBase64String(
                                subDir.getAbsolutePath() + File.separator + subDir.getName() + ".json"));
                    } else {
                        System.out.println("Target file not found in subfolder: " + subDir.getAbsolutePath());
                    }
                }
            }
        } else {
            System.out.println("PIPs folder not found or is not a directory.");
        }
        return pipStrings;
    }


    /**
     * Transform the pep properties json files into strings in order to be stored in the dht
     * @return a list of base64 strings representing the pep properties files
     */
    public static List<String> savePepsToString() {

        List<String> pepStrings = new ArrayList<>();

        if (pepsDir.exists() && pepsDir.isDirectory()) {
            File[] files = pepsDir.listFiles(File::isFile);
            if (files != null) {
                for (File targetFile : files) {
                    pepStrings.add(PersistUtility.fileToBase64String(targetFile.getAbsolutePath()));
                }
            }
        } else {
            System.out.println("PEPs folder not found or is not a directory.");
        }
        return pepStrings;
    }


    /**
     * Transform the policies into strings in order to be stored in the dht
     * @return a list of base64 strings representing the policy files
     */
    public static List<String> savePoliciesToString() {

        List<String> policiesStrings = new ArrayList<>();

        if (policiesDir.exists() && policiesDir.isDirectory()) {
            File[] files = policiesDir.listFiles(File::isFile);
            if (files != null) {
                for (File targetFile : files) {
                    policiesStrings.add(PersistUtility.fileToBase64String(targetFile.getAbsolutePath()));
                }
            }
        } else {
            System.out.println("Policies folder not found or is not a directory.");
        }
        return policiesStrings;
    }

    /**
     * Obtain the UCS status from the dht.
     * @return the inner value field of the received response containing the base64 string
     * representations of the database, pips, peps, and policies
     */
    public static Status downloadStatus() {
        boolean isStatusExistent = true;

        System.out.println("Downloading status ...");
        // get the status from the dht
        String response = null;
        if (isDhtReachable(dhtUri, 2000, Integer.MAX_VALUE)) {
            RequestGetTopicUuid requestGetTopicUuid =
                    new RequestGetTopicUuid(PERSISTENT_TOPIC_NAME_UCS, PERSISTENT_TOPIC_UUID_UCS_STATUS);

            JsonOutRequestGetTopicUuid jsonOut = new JsonOutRequestGetTopicUuid(requestGetTopicUuid);
            String request = new GsonBuilder()
                    .disableHtmlEscaping()
                    .serializeNulls()
                    .create()
                    .toJson(jsonOut);

            response = client.sendRequestAndWaitForResponse(request);
            client.closeConnection();

            // If we get an empty response, perform the request
            // again to be sure that the empty response is actually
            // the response to our request.
            // If a response different from the empty response is
            // retrieved, exit the loop.
            if (response.equals("{\"Response\":{\"value\":{}}}")) {
                int attempts = 5;
                for (int i = 1; i <= attempts; i++) {
                    response = client.sendRequestAndWaitForResponse(request);
                    client.closeConnection();
                    if (!response.equals("{\"Response\":{\"value\":{}}}")) {
                        break;
                    }
                }
                isStatusExistent = false;
            }
        } else {
            System.exit(1);
        }

        JsonInResponse jsonInResponse = new GsonBuilder()
                .registerTypeAdapterFactory(typeFactory)
                .create().fromJson(response, JsonInResponse.class);

        StatusRequestPostTopicUuid statusRequestPostTopicUuid =
                (StatusRequestPostTopicUuid) jsonInResponse.getResponse().getValue();
        return statusRequestPostTopicUuid.getValue();
        //return jsonInResponse.getResponse().getValue().getValue();
    }


    /**
     * Save the UCS status to the dht
     */
    public static void uploadStatus() {
    System.out.println("Uploading status ...");
        Status value = new Status(
                saveDbToString(),
                savePipsToString(),
                savePepsToString(),
                savePoliciesToString());
        StatusRequestPostTopicUuid requestPostTopicUuid =
                new StatusRequestPostTopicUuid(value, PERSISTENT_TOPIC_UUID_UCS_STATUS);
        JsonOutRequestPostTopicUuid jsonOut = new JsonOutRequestPostTopicUuid(requestPostTopicUuid);
        String request = new GsonBuilder()
                .disableHtmlEscaping()
                .serializeNulls()
                .create()
                .toJson(jsonOut);

        String response = null;
        if (isDhtReachable(dhtUri, 2000, Integer.MAX_VALUE)) {
            response = client.sendRequestAndWaitForResponse(request);
            client.closeConnection();
        } else {
            System.exit(1);
        }

        try {
            JsonInPersistent jsonInPersistent = new GsonBuilder()
                    .registerTypeAdapterFactory(persistentTypeFactory)
                    .create().fromJson(response, JsonInPersistent.class);
        } catch (Exception e) {
            throw new RuntimeException("Unable to parse received Persistent message");
        }
//        // fixme: sort of check to assess if the received response is invalid. Does it make sense?
//        if (jsonInPersistent.getPersistent().isDeleted()) {
//            System.err.println("The received response is marked as deleted");
//            System.exit(1);
//        } else {
            System.out.println("... status uploaded");
//        }
    }


    /**
     * Add a sample PIP monitoring an environment attribute; then, initialize
     * the UCS; and, finally, add a sample policy.
     */
    private static void initializeUCS() {

        // to be correctly initialized, the UCS needs at least one PIP to be present
        addSamplePip("it.cnr.iit.ucs.pipreader.PIPReader", "sample-pip",
                "urn:oasis:names:tc:xacml:3.0:environment:attribute-1",
                Category.ENVIRONMENT.toString(), DataType.STRING.toString(), "sample-attribute-1.txt",
                1000L, "attribute-1-value");

        sessionManagerProperties.setDbUri(dbUri);

        // start the UCS
        ucsClient = new UCSClient(pipPropertiesList, papProperties, sessionManagerProperties, pepPropertiesList);

        // add sample policy
        String examplePolicy = Utils.readContent(Utils.accessFile(UCSDht.class, "example-policy.xml"));
        if (!ucsClient.addPolicy(examplePolicy)) {
            System.err.println("Failed to add policy");
        }
    }

    /**
     * Take the sessions with status START and REVOKE, and for each of them
     * get the list of attributes in the ongoing condition and the original
     * request. Then, call the subscribe method. This guarantees that a PIP
     * sets the correct additionalInformation in the attribute it subscribes
     * to.
     */
    public static void restorePIPsSubscriptions() {
        // restore PIPs' subscriptions
        for (SessionInterface session : sessionsWithStatusStartOrRevoke) {
            try {
                PolicyWrapper policy = PolicyWrapper.build(session.getPolicySet());
                List<Attribute> attributes = policy.getAttributesForCondition(PolicyTags.getCondition(STATUS.START));
                RequestWrapper request = RequestWrapper.build(session.getOriginalRequest(), ucsClient.getPipRegistry());
                ucsClient.getPipRegistry().subscribe(request.getRequestType(), attributes);
            } catch (Exception e) {
                System.err.println("Error restoring PIPs' subscriptions");
                throw new RuntimeException(e);
            }
        }
        System.out.println("[PIPs] PIPs' subscriptions restored");
    }

    /**
     * Reevaluate the sessions with status START and REVOKE. This has to be
     * done because the value of mutable attributes that led either to the
     * status START or REVOKE might have changed while the UCS was down, i.e.,
     * since the last status was saved to (and now loaded from) the dht and
     * the current time.
     * Possibly, some reevaluation will lead to a change of its status. If
     * this happens, a REEVALUATION message is sent to the interested pep.
     */
    public static void reevaluateSessions() {
        for (SessionInterface session : sessionsWithStatusStartOrRevoke) {
            try {
                ucsClient.getContextHandler().reevaluate(session);
            } catch (Exception e) {
                System.err.println("Error reevaluating sessions");
                throw new RuntimeException(e);
            }
        }
    }


    /**
     * Load the PIPs from json files. Each PIP is in a subfolder of the pipsDir
     * folder. The subfolder includes a json file containing the properties, and,
     * for PIPs of type PIPReader only, a file containing the attribute value.
     * |_ pipsDir
     *     |_ pip-id
     *         |_ pip-id.json
     *         |_ attribute-value.txt (only for PIPReader)
     * The json files contain the PipProperties that are added to the list
     * used to initialize the UCS.
     */
    public static void loadPips() {
        if (pipsDir.exists() && pipsDir.isDirectory()) {
            File[] subDirs = pipsDir.listFiles(File::isDirectory);
            if (subDirs != null) {
                for (File subDir : subDirs) {
                    File targetFile = new File(subDir, subDir.getName() + ".json");
                    if (targetFile.exists() && targetFile.isFile()) {
                        System.out.print("[PIPs] Loading PIP '" + targetFile.getName() + "' in memory ...");
                        Optional<UCSDhtPipProperties> properties = Optional.empty();
                        try {
                            properties = JsonUtility.loadObjectFromJsonFile(targetFile, UCSDhtPipProperties.class);
                        } catch (NoSuchElementException e) {
                            System.err.println(e.getMessage());
                        }
                        if (properties.isPresent()) {
                            if (properties.get().getName().equals(PIPReader.class.getName())) {
                                for (Map<String, String> attribute : properties.get().getAttributes()) {
                                    String attributeId = attribute.get("ATTRIBUTE_ID");
                                    String fileName = properties.get().getAdditionalProperties().get(attributeId);
                                    properties.get().getAdditionalProperties().put(attributeId,
                                            pipsDir + File.separator +
                                                    properties.get().getId() + File.separator + fileName);
                                }
                            }
                            pipPropertiesList.add(properties.get());
                        }
                        System.out.println(" [LOADED]");
                    } else {
                        System.out.println("Target file not found in subfolder: " + subDir.getAbsolutePath());
                    }
                }
            }
        } else {
            System.out.println("PIPs folder not found or is not a directory.");
        }
        System.out.println();
    }

    /**
     * Load the PEPs from json files within the pepsDir folder.
     * The json files contain the PepProperties that are added to the list
     * used to initialize the UCS.
     */
    public static void loadPeps() {
        if (pepsDir.exists() && pepsDir.isDirectory()) {
            File[] files = pepsDir.listFiles(File::isFile);
            if (files != null) {
                for (File targetFile : files) {
                    System.out.print("[PEPs] Loading PEP '" + targetFile.getName() + "' in memory ...");
                    Optional<UCSDhtPepProperties> properties = Optional.empty();
                    try {
                        properties = JsonUtility.loadObjectFromJsonFile(targetFile, UCSDhtPepProperties.class);
                    } catch (NoSuchElementException e) {
                        System.err.println(e.getMessage());
                    }
                    properties.ifPresent(pepPropertiesList::add);
                    System.out.println(" [LOADED]");
                }
            }
        } else {
            System.out.println("PEPs folder not found or is not a directory.");
        }
        System.out.println();
    }

    /**
     * Add a pip programmatically and save its json serialization and attribute file
     */
    public static void addSamplePip(String name, String pipId, String attributeId, String category, String dataType,
                                    String fileName, long refreshRate, String attributeValue) {

        UCSDhtPipProperties pipReader = new UCSDhtPipProperties();
        pipReader.setName(name);
        pipReader.addAttribute(attributeId, category, dataType);
        pipReader.setRefreshRate(refreshRate);
        pipReader.setJournalPath("/tmp/ucf");
        pipReader.setJournalProtocol("file");
        Map<String, String> additionalProperties = new HashMap<>();
        additionalProperties.put(attributeId, pipsDir + File.separator + pipId + File.separator + fileName);
        additionalProperties.put(pipsDir + File.separator + pipId + File.separator + fileName, attributeValue);
        pipReader.setAdditionalProperties(additionalProperties);
        pipPropertiesList.add(pipReader);

        Utils.createDir(new File(pipsDir.getAbsolutePath() + File.separator + pipId));

        setAttributeValue(pipsDir.getAbsolutePath() + File.separator + pipId
                + File.separator + fileName, attributeValue);

        // when serializing, we specify only the file name, not the entire path
        Map<String, String> additionalPropertiesToSerialize = new HashMap<>();
        additionalPropertiesToSerialize.put(attributeId, fileName);
        additionalPropertiesToSerialize.put(fileName, attributeValue);

        PIPMessageManager.serializePipToFile(name, pipId, attributeId, category,
                dataType, refreshRate, additionalPropertiesToSerialize);
    }

    /**
     * Write a value in a file
     *
     * @param fileName the name of the file
     * @param value    the value to write in the file
     */
    public static void setAttributeValue(String fileName, String value) {

        File file = new File(fileName);
        FileWriter fw = null;
        try {
            fw = new FileWriter(file);
            fw.write(value);
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}