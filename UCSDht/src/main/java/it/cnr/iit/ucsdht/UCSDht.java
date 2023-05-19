package it.cnr.iit.ucsdht;

import com.google.gson.JsonSyntaxException;
import it.cnr.iit.ucs.message.startaccess.StartAccessResponseMessage;
import it.cnr.iit.ucs.message.tryaccess.TryAccessResponseMessage;
import it.cnr.iit.ucs.message.endaccess.EndAccessResponseMessage;
import it.cnr.iit.ucs.properties.components.PipProperties;
import it.cnr.iit.ucsdht.properties.UCSDhtPapProperties;
import it.cnr.iit.ucsdht.properties.UCSDhtPipReaderProperties;
import it.cnr.iit.utility.dht.DHTClient;
import it.cnr.iit.utility.dht.jsondht.*;
import it.cnr.iit.utility.dht.jsondht.endaccess.EndAccessRequest;
import it.cnr.iit.utility.dht.jsondht.endaccess.EndAccessResponse;
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
import java.util.List;

import static it.cnr.iit.utility.dht.DHTUtils.*;

public class UCSDht {

    private static DHTClient dhtClientEndPoint;
    private static final String SUB_COMMAND_TYPE = "ucs-command";
    private static UCSClient ucsClient;

    private static final String COMMAND_TYPE = "ucs-command";
    private static final String PUB_TOPIC_NAME = "topic-name";
    private static final String PUB_TOPIC_UUID = "topic-uuid-the-pep-is-subscribed-to";
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


    private static void initializeUCS(){
        UCSDhtPipReaderProperties pipReader = new UCSDhtPipReaderProperties();
        List<PipProperties> pipPropertiesList = new ArrayList<>();

        String path = getResourcePath(UCSDht.class);
        System.out.println(path);
        pipReader.addAttribute(
                "urn:oasis:names:tc:xacml:3.0:environment:attribute-1",
                Category.ENVIRONMENT.toString(),
                DataType.STRING.toString(),
                path  + File.separator + "sample-attribute.txt");
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


    private static String getPepIdFromJson(JsonIn jsonIn) {
        return jsonIn.getVolatile().getValue().getCommand().getValue().getPep_id();
    }


    private static String getMessageIdFromJson(JsonIn jsonIn) {
        return jsonIn.getVolatile().getValue().getCommand().getValue().getMessage().getMessage_id();
    }


    private static void handleTryAccessRequest(JsonIn jsonIn) {
        // construct a TryAccess message compliant with what the UCS accepts
        TryAccessRequest messageIn = (TryAccessRequest) getMessageFromJson(jsonIn);

        // make the actual try access request to the UCS
        //TryAccessResponseMessage response = ucsClient.tryAccess(messageIn.getRequest(), null, pepId, messageId);
        TryAccessResponseMessage response =
                ucsClient.tryAccess(exampleRequest, null, getPepIdFromJson(jsonIn), getMessageIdFromJson(jsonIn));

        // build the json object
        JsonOut jsonOut = buildTryAccessResponseMessage(jsonIn, response);

        serializeAndSend(jsonOut);
    }


    private static JsonOut buildTryAccessResponseMessage(JsonIn jsonIn, TryAccessResponseMessage response) {
        MessageContent messageOut = new TryAccessResponse(
                response.getMessageId(), response.getEvaluation().getResult(), response.getSessionId());

        return buildOutgoingJsonObject(
                messageOut, getPepIdFromJson(jsonIn), PUB_TOPIC_NAME, PUB_TOPIC_UUID, COMMAND_TYPE);
    }


    private static void handleStartAccessRequest(JsonIn jsonIn) {
        // construct a StartAccess message compliant with what the UCS accepts
        StartAccessRequest messageIn = (StartAccessRequest) getMessageFromJson(jsonIn);

        // make the actual start access request to the UCS
        StartAccessResponseMessage response =
                ucsClient.startAccess(messageIn.getSession_id(), getPepIdFromJson(jsonIn), getMessageIdFromJson(jsonIn));
        //todo: I could catch an exception thrown if no session is found

        // build the json object
        JsonOut jsonOut = buildStartAccessResponseMessage(jsonIn, response);

        serializeAndSend(jsonOut);
    }


    private static JsonOut buildStartAccessResponseMessage(JsonIn jsonIn, StartAccessResponseMessage response) {
        MessageContent messageOut =
                new StartAccessResponse(
                        response.getMessageId(), response.getEvaluation().getResult());

        return buildOutgoingJsonObject(
                messageOut, getPepIdFromJson(jsonIn), PUB_TOPIC_NAME, PUB_TOPIC_UUID, COMMAND_TYPE);
    }


    private static void handleEndAccessRequest(JsonIn jsonIn) {
        // construct an EndAccess message compliant with what the UCS accepts
        EndAccessRequest messageIn = (EndAccessRequest) getMessageFromJson(jsonIn);

        // make the actual end access request to the UCS
        EndAccessResponseMessage response =
                ucsClient.endAccess(messageIn.getSession_id(), getPepIdFromJson(jsonIn), getMessageIdFromJson(jsonIn));
        //todo: I could catch an exception thrown if no session is found

        // build the json object
        JsonOut jsonOut = buildEndAccessResponseMessage(jsonIn, response);

        serializeAndSend(jsonOut);
    }


    private static JsonOut buildEndAccessResponseMessage(JsonIn jsonIn, EndAccessResponseMessage response) {
        MessageContent messageOut =
                new EndAccessResponse(
                        response.getMessageId(), response.getEvaluation().getResult());

        return buildOutgoingJsonObject(
                messageOut, getPepIdFromJson(jsonIn), PUB_TOPIC_NAME, PUB_TOPIC_UUID, COMMAND_TYPE);
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

    private static void processMessage(JsonIn jsonIn) {
        MessageContent message = jsonIn.getVolatile().getValue().getCommand().getValue().getMessage();
        if (message instanceof TryAccessRequest) {
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


    private static DHTClient.MessageHandler setMessageHandler() {
        DHTClient.MessageHandler messageHandler = new DHTClient.MessageHandler() {
            /**
             * Deserialize the received message and check the topic matches
             * the one the PEP is subscribed to. If so, process the request
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
                processMessage(jsonIn);
            }
        };
        return messageHandler;
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


private static final String exampleRequest = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
        "<Request ReturnPolicyIdList=\"false\" CombinedDecision=\"false\"\n" +
        "  xmlns=\"urn:oasis:names:tc:xacml:3.0:core:schema:wd-17\">\n" +
        "  <Attributes Category=\"urn:oasis:names:tc:xacml:1.0:subject-category:access-subject\">\n" +
        "    <Attribute AttributeId=\"urn:oasis:names:tc:xacml:1.0:subject:subject-id\" IncludeInResult=\"false\">\n" +
        "      <AttributeValue DataType=\"http://www.w3.org/2001/XMLSchema#string\">C</AttributeValue>\n" +
        "    </Attribute>\n" +
        "  </Attributes>\n" +
        "  <Attributes Category=\"urn:oasis:names:tc:xacml:3.0:attribute-category:resource\">\n" +
        "    <Attribute AttributeId=\"urn:oasis:names:tc:xacml:1.0:resource:resource-id\" IncludeInResult=\"false\">\n" +
        "      <AttributeValue DataType=\"http://www.w3.org/2001/XMLSchema#string\">RES</AttributeValue>\n" +
        "    </Attribute>\n" +
        "    <Attribute AttributeId=\"urn:oasis:names:tc:xacml:1.0:resource:resource-server\" IncludeInResult=\"false\">\n" +
        "      <AttributeValue DataType=\"http://www.w3.org/2001/XMLSchema#string\">AUD</AttributeValue>\n" +
        "    </Attribute>\n" +
        "  </Attributes>\n" +
        "  <Attributes Category=\"urn:oasis:names:tc:xacml:3.0:attribute-category:action\">\n" +
        "    <Attribute AttributeId=\"urn:oasis:names:tc:xacml:1.0:action:action-id\" IncludeInResult=\"false\">\n" +
        "      <AttributeValue DataType=\"http://www.w3.org/2001/XMLSchema#string\">OP</AttributeValue>\n" +
        "    </Attribute>\n" +
        "  </Attributes>\n" +
        "</Request>\n";
}