package it.cnr.iit.ucsdht;

import com.google.gson.JsonSyntaxException;
import it.cnr.iit.ucs.message.endaccess.EndAccessResponseMessage;
import it.cnr.iit.ucs.message.startaccess.StartAccessResponseMessage;
import it.cnr.iit.ucs.message.tryaccess.TryAccessResponseMessage;
import it.cnr.iit.ucs.properties.components.PepProperties;
import it.cnr.iit.ucs.properties.components.PipProperties;
import it.cnr.iit.ucsdht.properties.UCSDhtPapProperties;
import it.cnr.iit.ucsdht.properties.UCSDhtPipReaderProperties;
import it.cnr.iit.utility.dht.DHTClient;
import it.cnr.iit.utility.dht.jsondht.JsonIn;
import it.cnr.iit.utility.dht.jsondht.JsonOut;
import it.cnr.iit.utility.dht.jsondht.MessageContent;
import it.cnr.iit.utility.dht.jsondht.addpip.AddPipRequest;
import it.cnr.iit.utility.dht.jsondht.addpip.AddPipResponse;
import it.cnr.iit.utility.dht.jsondht.addpolicy.AddPolicyRequest;
import it.cnr.iit.utility.dht.jsondht.addpolicy.AddPolicyResponse;
import it.cnr.iit.utility.dht.jsondht.deletepolicy.DeletePolicyRequest;
import it.cnr.iit.utility.dht.jsondht.deletepolicy.DeletePolicyResponse;
import it.cnr.iit.utility.dht.jsondht.endaccess.EndAccessRequest;
import it.cnr.iit.utility.dht.jsondht.endaccess.EndAccessResponse;
import it.cnr.iit.utility.dht.jsondht.error.ErrorResponse;
import it.cnr.iit.utility.dht.jsondht.getpolicy.GetPolicyRequest;
import it.cnr.iit.utility.dht.jsondht.getpolicy.GetPolicyResponse;
import it.cnr.iit.utility.dht.jsondht.listpolicies.ListPoliciesRequest;
import it.cnr.iit.utility.dht.jsondht.listpolicies.ListPoliciesResponse;
import it.cnr.iit.utility.dht.jsondht.registration.RegisterRequest;
import it.cnr.iit.utility.dht.jsondht.registration.RegisterResponse;
import it.cnr.iit.utility.dht.jsondht.startaccess.StartAccessRequest;
import it.cnr.iit.utility.dht.jsondht.startaccess.StartAccessResponse;
import it.cnr.iit.utility.dht.jsondht.tryaccess.TryAccessRequest;
import it.cnr.iit.utility.dht.jsondht.tryaccess.TryAccessResponse;
import it.cnr.iit.xacml.Category;
import it.cnr.iit.xacml.DataType;

import java.io.File;
import java.io.FileWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.io.IOException;

import static it.cnr.iit.utility.dht.DHTUtils.*;

public class UCSDht {

    private static DHTClient dhtClientEndPoint;
    private static String dhtUri = "ws://localhost:3000/ws";
    private static UCSClient ucsClient;

    private static final String SUB_COMMAND_TYPE = "ucs-command";
    private static final String COMMAND_TYPE = "ucs-command";
    private static final String PUB_TOPIC_NAME = "topic-name";
    private static final String SUB_TOPIC_UUID = "topic-uuid-the-ucs-is-subscribed-to";
    private static final File attributesDir = new File(Utils.getResourcePath(UCSDht.class), "attributes");
    private static final File policiesDir = new File(Utils.getResourcePath(UCSDht.class), "policies");


    public static void main(String[] args) {

        if (args.length != 0 && args[0].equals("-d")) {
            URI parsed = null;
            try {
                parsed = new URI(args[1]);
            } catch (URISyntaxException | ArrayIndexOutOfBoundsException e) {
                // No URI indicated
                System.err.println("Invalid URI after -d option");
                return;
            }
            dhtUri = parsed.toString();
        }

        initializeUCS();

        if (!isDhtReachable(dhtUri, 2000, Integer.MAX_VALUE)) {
            return;
        }

        try {
            dhtClientEndPoint = new DHTClient(new URI(dhtUri));
            dhtClientEndPoint.addMessageHandler(setMessageHandler());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        System.out.println("Waiting for commands...");
    }


    private static void initializeUCS() {

        Utils.createDir(attributesDir);
        Utils.createDir(policiesDir);

        List<PipProperties> pipPropertiesList = new ArrayList<>();

        // add sample attribute
        UCSDhtPipReaderProperties pipReader = new UCSDhtPipReaderProperties();
        pipReader.addAttribute(
                "urn:oasis:names:tc:xacml:3.0:environment:attribute-1",
                Category.ENVIRONMENT.toString(),
                DataType.STRING.toString(),
                attributesDir + File.separator + "sample-attribute.txt");
        pipReader.setRefreshRate(1000L);
        pipPropertiesList.add(pipReader);

        setAttributeValue(attributesDir.getAbsolutePath() + File.separator
                + "sample-attribute.txt", "attribute-1-value");

        UCSDhtPapProperties papProperties = new UCSDhtPapProperties(policiesDir.getAbsolutePath());

        ucsClient = new UCSClient(pipPropertiesList, papProperties);

        // add sample policy
        String examplePolicy = Utils.readContent(Utils.accessFile(UCSDht.class, "example-policy.xml"));
        ucsClient.addPolicy(examplePolicy);

        System.out.println("Policies directory: " + policiesDir.getAbsolutePath());
        System.out.println("Attributes directory: " + attributesDir.getAbsolutePath());

        System.out.println("UCS initialization complete");
    }


    private static MessageContent getMessageFromJson(JsonIn jsonIn) {
        return jsonIn.getVolatile().getValue().getCommand().getValue().getMessage();
    }


    private static String getIdFromJson(JsonIn jsonIn) {
        return jsonIn.getVolatile().getValue().getCommand().getValue().getId();
    }


    private static String getMessageIdFromJson(JsonIn jsonIn) {
        return jsonIn.getVolatile().getValue().getCommand().getValue().getMessage().getMessage_id();
    }


    private static void handleTryAccessRequest(JsonIn jsonIn) {
        // construct a TryAccess message compliant with what the UCS accepts
        TryAccessRequest messageIn = (TryAccessRequest) getMessageFromJson(jsonIn);

        // make the actual try access request to the UCS
        String request = new String(Base64.getDecoder().decode(messageIn.getRequest()));
        TryAccessResponseMessage response =
                ucsClient.tryAccess(request, null, getIdFromJson(jsonIn), getMessageIdFromJson(jsonIn));

        // build the json object
        JsonOut jsonOut = response == null ?
                buildErrorResponseMessageForPep(jsonIn, "Error during try access") :
                buildTryAccessResponseMessage(jsonIn, response);

        serializeAndSend(jsonOut);
    }


    private static JsonOut buildTryAccessResponseMessage(JsonIn jsonIn, TryAccessResponseMessage response) {
        MessageContent messageOut = new TryAccessResponse(
                response.getMessageId(), response.getEvaluation().getResult(), response.getSessionId());
        return buildJsonOutForPep(messageOut, getIdFromJson(jsonIn));
    }


    private static void handleStartAccessRequest(JsonIn jsonIn) {
        // construct a StartAccess message compliant with what the UCS accepts
        StartAccessRequest messageIn = (StartAccessRequest) getMessageFromJson(jsonIn);

        // make the actual start access request to the UCS
        StartAccessResponseMessage response =
                ucsClient.startAccess(messageIn.getSession_id(), getIdFromJson(jsonIn), getMessageIdFromJson(jsonIn));

        // build the json object
        JsonOut jsonOut = response == null ?
                buildErrorResponseMessageForPep(jsonIn, "Error during start access") :
                buildStartAccessResponseMessage(jsonIn, response);

        serializeAndSend(jsonOut);
    }


    private static JsonOut buildStartAccessResponseMessage(JsonIn jsonIn, StartAccessResponseMessage response) {
        MessageContent messageOut =
                new StartAccessResponse(
                        response.getMessageId(), response.getEvaluation().getResult());
        return buildJsonOutForPep(messageOut, getIdFromJson(jsonIn));
    }


    private static void handleEndAccessRequest(JsonIn jsonIn) {
        // construct an EndAccess message compliant with what the UCS accepts
        EndAccessRequest messageIn = (EndAccessRequest) getMessageFromJson(jsonIn);

        // make the actual end access request to the UCS
        EndAccessResponseMessage response =
                ucsClient.endAccess(messageIn.getSession_id(), getIdFromJson(jsonIn), getMessageIdFromJson(jsonIn));

        // build the json object
        JsonOut jsonOut = response == null ?
                buildErrorResponseMessageForPep(jsonIn, "Error during end access") :
                buildEndAccessResponseMessage(jsonIn, response);

        serializeAndSend(jsonOut);
    }


    private static JsonOut buildEndAccessResponseMessage(JsonIn jsonIn, EndAccessResponseMessage response) {
        MessageContent messageOut =
                new EndAccessResponse(
                        response.getMessageId(), response.getEvaluation().getResult());
        return buildJsonOutForPep(messageOut, getIdFromJson(jsonIn));
    }


    public static void handleReevaluation(JsonOut jsonOut) {
        serializeAndSend(jsonOut);
    }

    /**
     * Serialize the JsonOut object passed as argument and publish the
     * Json string on the DHT.
     *
     * @param jsonOut the object to serialize and send
     */
    private static void serializeAndSend(JsonOut jsonOut) {
        // serialize the object to a json string
        String msg = serializeOutgoingJson(jsonOut);

        // send the request
        if (!dhtClientEndPoint.sendMessage(msg)) {
            System.err.println("Error sending the message to the DHT");
        }
    }


    // this method returns OK both when the registration went through successfully
    // and when the pep was already registered
    // it returns KO if the pep cannot be registered
    public static void handleRegisterRequest(JsonIn jsonIn) {
        JsonOut jsonOut;
        if (!isPepRegistered(jsonIn)) {
            RegisterRequest messageIn = (RegisterRequest) getMessageFromJson(jsonIn);
            String pepId = getIdFromJson(jsonIn);
            String subTopicName = messageIn.getSub_topic_name();
            String subTopicUuid = messageIn.getSub_topic_uuid();

            if (!ucsClient.addPep(pepId, subTopicName, subTopicUuid)) {
                jsonOut = buildRegisterResponseMessage(jsonIn, "KO");
                serializeAndSend(jsonOut);
                return;
            }
        }
        jsonOut = buildRegisterResponseMessage(jsonIn, "OK");
        serializeAndSend(jsonOut);
    }


    private static JsonOut buildRegisterResponseMessage(JsonIn jsonIn, String code) {
        MessageContent messageOut = new RegisterResponse(getMessageIdFromJson(jsonIn), code);
        return buildJsonOutForPep(messageOut, getIdFromJson(jsonIn));
    }


    private static JsonOut buildJsonOutForPep(MessageContent messageOut, String pepId) {
        PepProperties pepProperties = ucsClient.getPepProperties(pepId);
        return buildOutgoingJsonObject(messageOut, pepId,
                pepProperties.getSubTopicName(), pepProperties.getSubTopicUuid(), COMMAND_TYPE);
    }


    private static JsonOut buildErrorResponseMessageForPep(JsonIn jsonIn, String description) {
        MessageContent messageOut =
                new ErrorResponse(
                        getMessageIdFromJson(jsonIn), description);
        return buildJsonOutForPep(messageOut, getIdFromJson(jsonIn));
    }


    private static void processPepMessage(JsonIn jsonIn) {
        MessageContent message = jsonIn.getVolatile().getValue().getCommand().getValue().getMessage();
        if (message instanceof RegisterRequest) {
            // handle register request
            System.out.println("handle register request");
            handleRegisterRequest(jsonIn);
        } else if (message instanceof TryAccessRequest) {
            // handle try access request
            System.out.println("handle try access request");
            handleTryAccessRequest(jsonIn);
        } else if (message instanceof StartAccessRequest) {
            // handle start access request
            System.out.println("handle start access request");
            handleStartAccessRequest(jsonIn);
        } else if (message instanceof EndAccessRequest) {
            // handle end access request
            System.out.println("handle end access request");
            handleEndAccessRequest(jsonIn);
        } else {
            // class not recognized. Handle case
            // this should not happen since the deserialization would already have thrown an exception
            System.err.println("class not recognized. It might be a ResponseMessage");
        }
    }

    private static void processPapMessage(JsonIn jsonIn) {
        MessageContent message = jsonIn.getVolatile().getValue().getCommand().getValue().getMessage();
        if (message instanceof AddPolicyRequest) {
            System.out.println("handle add policy request");
            handleAddPolicyRequest(jsonIn);
        } else if (message instanceof DeletePolicyRequest) {
            System.out.println("handle delete policy request");
            handleDeletePolicyRequest(jsonIn);
        } else if (message instanceof ListPoliciesRequest) {
            System.out.println("handle list policies request");
            handleListPoliciesRequest(jsonIn);
        } else if (message instanceof GetPolicyRequest) {
            System.out.println("handle get policy request");
            handleGetPolicyRequest(jsonIn);
        } else {
            // class not recognized. Handle case
            // this should not happen since the deserialization would already have thrown an exception
            System.err.println("class not recognized. It might be a ResponseMessage");
        }
    }

    private static void handleAddPolicyRequest(JsonIn jsonIn) {
        AddPolicyRequest messageIn = (AddPolicyRequest) getMessageFromJson(jsonIn);

        //String policy = messageIn.getPolicy();
        String policy = new String(Base64.getDecoder().decode(messageIn.getPolicy()));

        JsonOut jsonOut;
        if (!ucsClient.addPolicy(policy)) {
            jsonOut = buildAddPolicyResponseMessage(jsonIn, "KO");
        } else {
            jsonOut = buildAddPolicyResponseMessage(jsonIn, "OK");
        }
        serializeAndSend(jsonOut);
    }


    private static JsonOut buildAddPolicyResponseMessage(JsonIn jsonIn, String code) {

        MessageContent messageOut = new AddPolicyResponse(getMessageIdFromJson(jsonIn), code);
        return buildOutgoingJsonObject(messageOut, getIdFromJson(jsonIn), "topic-name-pap-is-subscribed-to", "topic-uuid-pap-is-subscribed-to", COMMAND_TYPE);
    }


    private static void handleDeletePolicyRequest(JsonIn jsonIn) {
        DeletePolicyRequest messageIn = (DeletePolicyRequest) getMessageFromJson(jsonIn);

        String policyId = messageIn.getPolicy_id();

        JsonOut jsonOut;
        if (!ucsClient.deletePolicy(policyId)) {
            jsonOut = buildDeletePolicyResponseMessage(jsonIn, "KO");
        } else {
            jsonOut = buildDeletePolicyResponseMessage(jsonIn, "OK");
        }
        serializeAndSend(jsonOut);
    }


    private static JsonOut buildDeletePolicyResponseMessage(JsonIn jsonIn, String code) {

        MessageContent messageOut = new DeletePolicyResponse(getMessageIdFromJson(jsonIn), code);
        return buildOutgoingJsonObject(messageOut, getIdFromJson(jsonIn), "topic-name-pap-is-subscribed-to", "topic-uuid-pap-is-subscribed-to", COMMAND_TYPE);
    }


    private static void handleListPoliciesRequest(JsonIn jsonIn) {
        List<String> policyList = ucsClient.listPolicies();

        JsonOut jsonOut = buildListPoliciesResponseMessage(jsonIn, policyList);
        serializeAndSend(jsonOut);
    }


    private static JsonOut buildListPoliciesResponseMessage(JsonIn jsonIn, List<String> policyList) {

        MessageContent messageOut = new ListPoliciesResponse(getMessageIdFromJson(jsonIn), policyList);
        return buildOutgoingJsonObject(messageOut, getIdFromJson(jsonIn), "topic-name-pap-is-subscribed-to", "topic-uuid-pap-is-subscribed-to", COMMAND_TYPE);
    }


    private static void handleGetPolicyRequest(JsonIn jsonIn) {
        GetPolicyRequest messageIn = (GetPolicyRequest) getMessageFromJson(jsonIn);

        String policyId = messageIn.getPolicy_id();

        String policy = ucsClient.getPolicy(policyId);
        String base64Policy = Base64.getEncoder().encodeToString(policy.getBytes());

        JsonOut jsonOut = buildGetPolicyResponseMessage(jsonIn, base64Policy);

        serializeAndSend(jsonOut);
    }


    private static JsonOut buildGetPolicyResponseMessage(JsonIn jsonIn, String policy) {

        MessageContent messageOut = new GetPolicyResponse(getMessageIdFromJson(jsonIn), policy);
        return buildOutgoingJsonObject(messageOut, getIdFromJson(jsonIn), "topic-name-pap-is-subscribed-to", "topic-uuid-pap-is-subscribed-to", COMMAND_TYPE);
    }


    private static void processPipMessage(JsonIn jsonIn) {
        MessageContent message = jsonIn.getVolatile().getValue().getCommand().getValue().getMessage();
        if (message instanceof AddPipRequest) {
            System.out.println("handle add Pip request");
            handleAddPipRequest(jsonIn);
        } else {
            // class not recognized. Handle case
            // this should not happen since the deserialization would already have thrown an exception
            System.err.println("class not recognized. It might be a ResponseMessage");
        }
    }

    private static void handleAddPipRequest(JsonIn jsonIn) {
        AddPipRequest messageIn = (AddPipRequest) getMessageFromJson(jsonIn);

        String pipType = messageIn.getPip_type();
        String attributeId = messageIn.getAttribute_id();
        String category = messageIn.getCategory();
        String dataType = messageIn.getData_type();
        String attributeValue = messageIn.getAttribute_value();
        String attributePath = attributesDir + File.separator;
        String fileName = messageIn.getFile_name();
        long refreshRate = messageIn.getRefresh_rate();

        JsonOut jsonOut;
        if (!ucsClient.addPip(pipType, attributeId, category, dataType, attributePath, fileName, refreshRate)) {
            jsonOut = buildAddPipResponseMessage(jsonIn, "KO");
        } else {
            setAttributeValue(attributePath + fileName, attributeValue);
            jsonOut = buildAddPipResponseMessage(jsonIn, "OK");
        }
        serializeAndSend(jsonOut);
    }


    private static JsonOut buildAddPipResponseMessage(JsonIn jsonIn, String code) {

        MessageContent messageOut = new AddPipResponse(getMessageIdFromJson(jsonIn), code);
        return buildOutgoingJsonObject(messageOut, getIdFromJson(jsonIn), "topic-name-pip-is-subscribed-to", "topic-uuid-pip-is-subscribed-to", COMMAND_TYPE);
    }

    private static JsonOut buildErrorResponseMessage(JsonIn jsonIn, String description) {
        MessageContent messageOut =
                new ErrorResponse(
                        getMessageIdFromJson(jsonIn), description);
        return buildOutgoingJsonObject(messageOut, getIdFromJson(jsonIn), "topic-name-pap-is-subscribed-to", "topic-uuid-pap-is-subscribed-to", COMMAND_TYPE);
    }

    private static DHTClient.MessageHandler setMessageHandler() {
        DHTClient.MessageHandler messageHandler = new DHTClient.MessageHandler() {
            /**
             * Deserialize the received message and check the topic matches
             * the one the UCS is subscribed to. If so, process the request
             * coming from the DHT
             *
             * @param message the message, a json string, coming from the DHT
             */
            public void handleMessage(String message) {
                JsonIn jsonIn;
                try {
                    // deserialize json
                    jsonIn = deserializeIncomingJson(message);

                    // check the topic is the one we are subscribed to
                    if (!isTopicOfInterest(jsonIn, SUB_TOPIC_UUID)) {
                        return;
                    }
                    System.out.println("Topic matches. Message type: " + jsonIn.getVolatile().getValue().getCommand().getValue().getMessage().getClass().getSimpleName());
                } catch (JsonSyntaxException e) {
                    //System.err.println("Error deserializing Json. Message discarded.");
                    return;
                }
                switch (jsonIn.getVolatile().getValue().getCommand().getCommand_type()) {
                    case "pep-command":
                        if (!isRegisterRequest(jsonIn) && !isPepRegistered(jsonIn)) {
                            System.err.println("An unregistered PEP tried to make a request. Request discarded.");
                            return;
                        }
                        try {
                            processPepMessage(jsonIn);
                        } catch (Exception e) {
                            System.err.println("Error processing PEP request: " + e.getMessage());
                            JsonOut jsonOut =
                                    buildErrorResponseMessageForPep(jsonIn, e.getMessage());
                            serializeAndSend(jsonOut);
                        }
                        break;
                    case "pap-command":
                        try {
                            processPapMessage(jsonIn);
                        } catch (Exception e) {
                            System.err.println("Error processing PAP request: " + e.getMessage());
                            JsonOut jsonOut =
                                    buildErrorResponseMessage(jsonIn, e.getMessage());
                            serializeAndSend(jsonOut);
                        }
                        break;
                    case "pip-command":
                        try {
                            processPipMessage(jsonIn);
                        } catch (Exception e) {
                            System.err.println("Error processing PIP request: " + e.getMessage());
                            JsonOut jsonOut =
                                    buildErrorResponseMessage(jsonIn, e.getMessage());
                            serializeAndSend(jsonOut);
                        }
                        break;
                    default:
                        System.err.println("Wrong command type. Request discarded.");
                }
            }

            @Override
            public void handleError() {
                System.err.println("Websocket error occurred");
            }
        };
        return messageHandler;
    }

    private static boolean isRegisterRequest(JsonIn jsonIn) {
        MessageContent messageIn = jsonIn.getVolatile().getValue().getCommand().getValue().getMessage();
        return messageIn instanceof RegisterRequest;
    }

    private static boolean isPepRegistered(JsonIn jsonIn) {
        return (ucsClient.pepMapHas(getIdFromJson(jsonIn)));
    }


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