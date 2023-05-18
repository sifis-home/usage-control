package it.cnr.iit.ucsdht;

import com.google.gson.JsonSyntaxException;
import com.google.gson.typeadapters.RuntimeTypeAdapterFactory;
import it.cnr.iit.ucs.message.startaccess.StartAccessResponseMessage;
import it.cnr.iit.ucs.message.tryaccess.TryAccessResponseMessage;
import it.cnr.iit.ucs.properties.components.PapProperties;
import it.cnr.iit.ucs.properties.components.PipProperties;
import it.cnr.iit.ucsdht.properties.UCSDhtPapProperties;
import it.cnr.iit.ucsdht.properties.UCSDhtPipReaderProperties;
import it.cnr.iit.utility.dht.DHTClient;
import it.cnr.iit.utility.dht.jsondht.*;
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

    private static final RuntimeTypeAdapterFactory<MessageContent> typeFactory = RuntimeTypeAdapterFactory
            .of(MessageContent.class, "type")
            .registerSubtype(TryAccessRequest.class, "try_access_request")
            .registerSubtype(TryAccessResponse.class, "try_access_response")
            .registerSubtype(StartAccessRequest.class, "start_access_request")
            .registerSubtype(StartAccessResponse.class, "start_access_response");
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
//            String msg = "{\n" +
//                    "  \"RequestPubMessage\": {\n" +
//                    "    \"value\": {\n" +
//                    "      \"timestamp\": 1684328290116,\n" +
//                    "      \"command\": {\n" +
//                    "        \"command_type\": \"pep-command\",\n" +
//                    "        \"value\": {\n" +
//                    "          \"message\": {\n" +
//                    "            \"type\": \"try_access_response\",\n" +
//                    "            \"request\": \"request\",\n" +
//                    "            \"policy\": \"policy\"\n" +
//                    "          },\n" +
//                    "          \"pep_id\": \"pep_id\",\n" +
//                    "          \"message_id\": \"random123-msg_id\",\n" +
//                    "          \"topic_name\": \"topic_name_XXX\",\n" +
//                    "          \"topic_uuid\": \"ucs_topic_uuid\"\n" +
//                    "        }\n" +
//                    "      }\n" +
//                    "    }\n" +
//                    "  }\n" +
//                    "}";
//            dhtClientEndPoint.sendMessage(msg);


            // wait for commands

            // when a command arrives, check that the topic matches

            // if it matches, extract the MessageContent and transform it
            // in a Message (ucs) in order to be processed by the UCS


    }

    private static void initializeUCS(){
        UCSDhtPipReaderProperties pipReader = new UCSDhtPipReaderProperties();
        List<PipProperties> pipPropertiesList = new ArrayList<>();

        //new File(Utils.getResourcePath(UCSDht.class), "attributes");
        String path = getResourcePath(UCSDht.class);
        System.out.println(path);
        pipReader.addAttribute(
                "urn:oasis:names:tc:xacml:3.0:environment:attribute-1",
                Category.ENVIRONMENT.toString(),
                DataType.STRING.toString(),
                "./sample-attribute.txt");
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

        // serialize it to a json string
        String msg = serializeOutgoingJson(jsonOut);

        // send the response
        dhtClientEndPoint.sendMessage(msg);
    }


    private static JsonOut buildTryAccessResponseMessage(JsonIn jsonIn, TryAccessResponseMessage response) {
        MessageContent messageOut = new TryAccessResponse(
                response.getMessageId(), response.getEvaluation().getResult(), response.getSessionId());

        return buildOutgoingJsonObject(
                messageOut, getPepIdFromJson(jsonIn), PUB_TOPIC_NAME, PUB_TOPIC_UUID, COMMAND_TYPE);
    }

    private static void handleStartAccessRequest(JsonIn jsonIn) {
        // construct a TryAccess message compliant with what the UCS accepts
        StartAccessRequest messageIn = (StartAccessRequest) getMessageFromJson(jsonIn);

        // make the actual start access request to the UCS
        StartAccessResponseMessage response =
                ucsClient.startAccess(messageIn.getSession_id(), getPepIdFromJson(jsonIn), getMessageIdFromJson(jsonIn));
        //todo: I could catch an exception thrown if no session is found

        // build the json object
        JsonOut jsonOut = buildStartAccessResponseMessage(jsonIn, response);

        // serialize it to a json string
        String msg = serializeOutgoingJson(jsonOut);

        // send the response
        dhtClientEndPoint.sendMessage(msg);
    }


    private static JsonOut buildStartAccessResponseMessage(JsonIn jsonIn, StartAccessResponseMessage response) {
        MessageContent messageOut =
                new StartAccessResponse(
                        response.getMessageId(), response.getEvaluation().getResult());

        return buildOutgoingJsonObject(
                messageOut, getPepIdFromJson(jsonIn), PUB_TOPIC_NAME, PUB_TOPIC_UUID, COMMAND_TYPE);
    }


    private static void processMessage(JsonIn jsonIn) {
        MessageContent message = jsonIn.getVolatile().getValue().getCommand().getValue().getMessage();
        if (message instanceof TryAccessRequest) {
            // handle try access request
            System.out.println("handle try access request");
            handleTryAccessRequest(jsonIn);
            return;
        } else if (message instanceof StartAccessRequest) {
            // handle start access request
            System.out.println("handle start access request");
            handleStartAccessRequest(jsonIn);
            return;
//        } else if (message instanceof EndAccessRequest) {
//            // handle end access request
//            return;
        } else {
            // class not recognized. Handle case
            // this should not happen since the deserialization would already have thrown an exception
            System.out.println("class not recognized. It might be a ResponseMessage");
            return;
        }
    }

    private static DHTClient.MessageHandler setMessageHandler() {
        DHTClient.MessageHandler messageHandler = new DHTClient.MessageHandler() {
            public void handleMessage(String message) {
                //System.out.println("Received new message\n");

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