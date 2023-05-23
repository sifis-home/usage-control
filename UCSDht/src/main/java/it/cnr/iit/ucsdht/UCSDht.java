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
import it.cnr.iit.utility.dht.jsondht.addpolicy.AddPolicyRequest;
import it.cnr.iit.utility.dht.jsondht.addpolicy.AddPolicyResponse;
import it.cnr.iit.utility.dht.jsondht.deletepolicy.DeletePolicyRequest;
import it.cnr.iit.utility.dht.jsondht.deletepolicy.DeletePolicyResponse;
import it.cnr.iit.utility.dht.jsondht.endaccess.EndAccessRequest;
import it.cnr.iit.utility.dht.jsondht.endaccess.EndAccessResponse;
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
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import static it.cnr.iit.utility.dht.DHTUtils.*;

public class UCSDht {

    private static DHTClient dhtClientEndPoint;
    private static final String SUB_COMMAND_TYPE = "ucs-command";
    private static UCSClient ucsClient;

    private static final String COMMAND_TYPE = "ucs-command";
    private static final String PUB_TOPIC_NAME = "topic-name";
    private static final String SUB_TOPIC_UUID = "topic-uuid-the-ucs-is-subscribed-to";

    public static void main(String[] args) {

        initializeUCS();
        try {
            dhtClientEndPoint = new DHTClient(
                    new URI("ws://localhost:3000/ws"));
            dhtClientEndPoint.addMessageHandler(setMessageHandler());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }


    private static void initializeUCS() {
        UCSDhtPipReaderProperties pipReader = new UCSDhtPipReaderProperties();
        List<PipProperties> pipPropertiesList = new ArrayList<>();

        String path = getResourcePath(UCSDht.class);
        System.out.println(path);
        pipReader.addAttribute(
                "urn:oasis:names:tc:xacml:3.0:environment:attribute-1",
                Category.ENVIRONMENT.toString(),
                DataType.STRING.toString(),
                path + File.separator + "sample-attribute.txt");
        pipReader.setRefreshRate(1000L);
        pipPropertiesList.add(pipReader);

        UCSDhtPapProperties papProperties = new UCSDhtPapProperties(path);

        ucsClient = new UCSClient(pipPropertiesList, papProperties);
        System.out.println("UCS initialized");
        //ucsClient.addPep("new-pep", new PEPDht(new UCSDhtPepProperties()));
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
        JsonOut jsonOut = buildTryAccessResponseMessage(jsonIn, response);

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
        //todo: I could catch an exception thrown if no session is found

        // build the json object
        JsonOut jsonOut = buildStartAccessResponseMessage(jsonIn, response);

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
        //todo: I could catch an exception thrown if no session is found

        // build the json object
        JsonOut jsonOut = buildEndAccessResponseMessage(jsonIn, response);

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
        } else {
            // class not recognized. Handle case
            // this should not happen since the deserialization would already have thrown an exception
            System.err.println("class not recognized. It might be a ResponseMessage");
        }
    }

    private static void handleAddPolicyRequest(JsonIn jsonIn) {
        AddPolicyRequest messageIn = (AddPolicyRequest) getMessageFromJson(jsonIn);

        String policy = messageIn.getPolicy();
        //String policy = new String(Base64.getDecoder().decode(messageIn.getPolicy()));

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
                    System.err.println("Error deserializing Json. " + e.getMessage());
                    return;
                }
                switch (jsonIn.getVolatile().getValue().getCommand().getCommand_type()) {
                    case "pep-command":
                        if (!isRegisterRequest(jsonIn) && !isPepRegistered(jsonIn)) {
                            System.err.println("An unregistered PEP tried to make a request. Request discarded.");
                            return;
                        }
                        processPepMessage(jsonIn);
                        break;
                    case "pap-command":
                        processPapMessage(jsonIn);
                        break;
                    default:
                        System.err.println("Wrong command type. Request discarded.");
                }
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


    public static String getResourcePath(Class<?> clazz) {

        URL input = clazz.getProtectionDomain().getCodeSource().getLocation();

        try {
            File myfile = new File(input.toURI());
            File dir = myfile.getParentFile(); // strip off .jar file
            return dir.getAbsolutePath();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}