package it.cnr.iit.ucsdht;

import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import it.cnr.iit.ucs.constants.STATUS;
import it.cnr.iit.ucs.properties.components.PepProperties;
import it.cnr.iit.ucs.properties.components.PipProperties;
import it.cnr.iit.ucs.sessionmanager.OnGoingAttribute;
import it.cnr.iit.ucs.sessionmanager.Session;
import it.cnr.iit.ucs.sessionmanager.SessionInterface;
import it.cnr.iit.ucs.sessionmanager.SessionManager;
import it.cnr.iit.ucsdht.properties.UCSDhtPapProperties;
import it.cnr.iit.ucsdht.properties.UCSDhtPepProperties;
import it.cnr.iit.ucsdht.properties.UCSDhtPipReaderProperties;
import it.cnr.iit.ucsdht.properties.UCSDhtSessionManagerProperties;
import it.cnr.iit.utility.JsonUtility;
import it.cnr.iit.utility.dht.DHTClient;
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

    private static String dhtUri = "ws://localhost:3000/ws";
    private static String dbUri = "jdbc:sqlite:file::memory:?cache=shared";


    static final String COMMAND_TYPE = "ucs-command";
    static final String UCS_SUB_TOPIC_UUID = "topic-uuid-the-ucs-is-subscribed-to";

    static final String PAP_SUB_TOPIC_NAME = "topic-name-pap-is-subscribed-to";
    static final String PAP_SUB_TOPIC_UUID = "topic-uuid-pap-is-subscribed-to";

    static final String PIP_SUB_TOPIC_NAME = "topic-name-pip-is-subscribed-to";
    static final String PIP_SUB_TOPIC_UUID = "topic-uuid-pip-is-subscribed-to";
    static final File pipsDir = new File(Utils.getResourcePath(UCSDht.class), "pips");
    static final File policiesDir = new File(Utils.getResourcePath(UCSDht.class), "policies");
    static final File pepsDir = new File(Utils.getResourcePath(UCSDht.class), "peps");


    static final List<PipProperties> pipPropertiesList = new ArrayList<>();
    static final UCSDhtPapProperties papProperties = new UCSDhtPapProperties(policiesDir.getAbsolutePath());
    static final UCSDhtSessionManagerProperties sessionManagerProperties = new UCSDhtSessionManagerProperties();
    static final List<PepProperties> pepPropertiesList = new ArrayList<>();

    static List<SessionInterface> sessionsWithStatusStartOrRevoke = new ArrayList<>();

    static boolean softReset = false;
    static boolean hardReset = false;

    public static void main(String[] args) {

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--dht":
                    URI parsed = null;
                    try {
                        parsed = new URI(args[i + 1]);
                    } catch (URISyntaxException | ArrayIndexOutOfBoundsException e) {
                        // No URI indicated
                        System.err.println("Invalid URI after --dht option");
                        System.exit(1);
                    }
                    dhtUri = parsed.toString();
                    i = i + 1;
                    break;
                case "--db":
                    try {
                        dbUri = args[i + 1];
                    } catch (ArrayIndexOutOfBoundsException e) {
                        System.err.println("Invalid database URI after --db option");
                        System.exit(1);
                    }
                    if (!dbUri.startsWith("jdbc:")) {
                        System.err.println("Invalid database URL after --db option");
                        System.err.println("Database URL was expected to start with jdbc: but was " + dbUri);
                        System.exit(1);
                    }
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
                    System.err.println("Unknown option");
                    System.exit(1);
            }
        }

        try {
            restoreUCSState();
            initializeUCS();
            restorePIPsSubscriptions();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

        System.out.println();
        System.out.println("UCS initialization complete");
        System.out.println("  Policies directory: " + policiesDir.getAbsolutePath());
        System.out.println("  PIPs directory: " + pipsDir.getAbsolutePath());
        System.out.println("  PEPs directory: " + pepsDir.getAbsolutePath());
        System.out.println();

        if (!isDhtReachable(dhtUri, 2000, Integer.MAX_VALUE)) {
            System.exit(1);
        }

        try {
            dhtClientEndPoint = new DHTClient(new URI(dhtUri));
            dhtClientEndPoint.addMessageHandler(new DhtMessageHandler());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        System.out.println("Waiting for commands...");
        System.out.println();
    }


    /**
     * Load PIPs and PEPs if no hard reset is specified; otherwise remove them.
     * Then, if any kind of reset is specified, wipe the database (drop the tables);
     * otherwise, extract the sessions with status START and REVOKE.
     */
    private static void restoreUCSState() {
        // if --hard-reset option is set, we empty PIPs, PEPs, and policies folders.
        // Else, we create the folders only if they don't exist, thus we preserve
        // the files possibly contained in them. Then, we load PIPs and PEPs by
        // deserializing the related json files.
        if (hardReset) {
            Utils.createDir(pipsDir);
            Utils.createDir(policiesDir);
            Utils.createDir(pepsDir);
        } else {
            Utils.createDirIfNotExists(pipsDir);
            Utils.createDirIfNotExists(policiesDir);
            Utils.createDirIfNotExists(pepsDir);

            loadPips();
            loadPeps();
        }

        // If either --soft-reset or --hard-reset is set, the 'sessions' table and
        // the 'on_going_attributes' table are deleted from the database.
        // Otherwise, the sessions with status START and REVOKE are retrieved from
        // the 'sessions' table. This is needed to restore the PIPs' subscriptions
        // and has to be done after the UCS is initialized.
        if (softReset || hardReset) {
            try (ConnectionSource connectionSource = new JdbcConnectionSource(dbUri)) {
                TableUtils.dropTable(connectionSource, Session.class, true);
                TableUtils.dropTable(connectionSource, OnGoingAttribute.class, true);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            UCSDhtSessionManagerProperties smProp = new UCSDhtSessionManagerProperties();
            smProp.setDbUri(dbUri);
            SessionManager sessionManager = new SessionManager(smProp);
            sessionManager.start();
            sessionsWithStatusStartOrRevoke.addAll(sessionManager.getSessionsForStatus(STATUS.START.toString()));
            sessionsWithStatusStartOrRevoke.addAll(sessionManager.getSessionsForStatus(STATUS.REVOKE.toString()));
            sessionManager.stop();
        }
    }

    /**
     * Add a sample PIP monitoring an environment attribute; then, initialize
     * the UCS; and, finally, add a sample policy.
     */
    private static void initializeUCS() {

        // to be correctly initialized, the UCS needs at least one PIP to be present
        addSamplePip("sample-pip", "urn:oasis:names:tc:xacml:3.0:environment:attribute-1",
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
        if (!(softReset || hardReset)) {
            // restore PIPs' subscriptions
            for (SessionInterface session : sessionsWithStatusStartOrRevoke) {
                try {
                    PolicyWrapper policy = PolicyWrapper.build(session.getPolicySet());
                    List<Attribute> attributes = policy.getAttributesForCondition(PolicyTags.getCondition(STATUS.START));
                    RequestWrapper request = RequestWrapper.build(session.getOriginalRequest(), ucsClient.getPipRegistry());
                    ucsClient.getPipRegistry().subscribe(request.getRequestType(), attributes);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            System.out.println("PIPs' subscriptions restored.");
        }
    }


    /**
     * Load the PIPs (only PIPReader is supported) from json files. Each PIP
     * is in a subfolder of the pipsDir folder. The subfolder includes a
     * json file containing the properties, and a file containing the
     * attribute value.
     * |__ pipsDir
     * |__ pip-id
     * |__ pip-id.json
     * |__ attribute-value.txt
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
                        System.out.print("[PIPs] Loading PIP '" + targetFile.getName() + "' ...");
                        Optional<UCSDhtPipReaderProperties> properties = Optional.empty();
                        try {
                            properties = JsonUtility.loadObjectFromJsonFile(targetFile, UCSDhtPipReaderProperties.class);
                        } catch (NoSuchElementException e) {
                            System.err.println(e.getMessage());
                        }
                        if (properties.isPresent()) {
                            for (Map<String, String> attribute : properties.get().getAttributes()) {
                                attribute.put("FILE_PATH", pipsDir + File.separator +
                                        properties.get().getId() + File.separator + attribute.get("FILE_PATH"));
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
                    System.out.print("[PEPs] Loading PEP '" + targetFile.getName() + "' ...");
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
    }

    /**
     * Add a pip programmatically and save its json serialization and attribute file
     */
    public static void addSamplePip(String pipId, String attributeId, String category, String dataType,
                                    String fileName, long refreshRate, String attributeValue) {

        UCSDhtPipReaderProperties pipReader = new UCSDhtPipReaderProperties();
        pipReader.addAttribute(attributeId, category, dataType,
                pipsDir + File.separator + pipId + File.separator + fileName);
        pipReader.setRefreshRate(refreshRate);
        pipReader.setJournalPath("/tmp/ucf");
        pipReader.setJournalProtocol("file");
        pipPropertiesList.add(pipReader);

        Utils.createDir(new File(pipsDir.getAbsolutePath() + File.separator + pipId));

        setAttributeValue(pipsDir.getAbsolutePath() + File.separator + pipId
                + File.separator + fileName, attributeValue);

        PIPMessageManager.serializePipToFile(pipId, attributeId, category,
                dataType, fileName, refreshRate, attributeValue);
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