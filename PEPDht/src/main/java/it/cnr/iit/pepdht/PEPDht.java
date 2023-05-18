package it.cnr.iit;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.typeadapters.RuntimeTypeAdapterFactory;
import it.cnr.iit.ucs.message.Message;
import it.cnr.iit.ucs.message.reevaluation.ReevaluationResponseMessage;
import it.cnr.iit.ucs.pep.PEPInterface;
import it.cnr.iit.ucs.properties.components.PepProperties;
import it.cnr.iit.utility.dht.DHTClient;
import it.cnr.iit.utility.DHTUtils;
import it.cnr.iit.utility.errorhandling.Reject;
import it.cnr.iit.utility.dht.jsondht.*;
import it.cnr.iit.utility.dht.jsondht.startaccess.StartAccessRequest;
import it.cnr.iit.utility.dht.jsondht.startaccess.StartAccessResponse;
import it.cnr.iit.utility.dht.jsondht.tryaccess.TryAccessRequest;
import it.cnr.iit.utility.dht.jsondht.tryaccess.TryAccessResponse;

import java.net.URI;
import java.net.URISyntaxException;

public class PEPDht implements PEPInterface {

    private static DHTUtils dhtClientEndPoint;
    private static final String SUB_TOPIC_UUID = "ucs_topic_uuid";
    private static final String SUB_COMMAND_TYPE = "ucs-command";

    private static final RuntimeTypeAdapterFactory<MessageContent> typeFactory = RuntimeTypeAdapterFactory
            .of(MessageContent.class, "type")
            .registerSubtype(TryAccessRequest.class, "try_access_request")
            .registerSubtype(TryAccessResponse.class, "try_access_response")
            .registerSubtype(StartAccessRequest.class, "start_access_request")
            .registerSubtype(StartAccessResponse.class, "start_access_response");

    public static void main(String[] args) {

        try {
            dhtClientEndPoint = new DHTUtils(
                    new URI("ws://localhost:3000/ws"));
            dhtClientEndPoint.addMessageHandler(setMessageHandler());

        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        tryAccess();

        NewThread t = new NewThread();
        t.start();
    }


    /**
     * Publish a tryAccess request on the DHT
     */
    public static void tryAccess() {
        // compile the request (json)
        String msg = buildTryAccessMessage();

        // send the request
        dhtClientEndPoint.sendMessage(msg);
    }


    /**
     * Build a tryAccess request as a Json to send to the DHT
     *
     * @return the Json to send to the DHT
     */
    private static String buildTryAccessMessage() {
        TryAccessRequest message =
                new TryAccessRequest("request", "policy");
        String message_id = "random123-msg_id";
        //String message_id = String.valueOf(UUID.randomUUID());
        // save message_id in a structure
        return serializeOutgoingJson(message, message_id);
    }


    /**
     * Build a startAccess request as a Json to send to the DHT
     *
     * @param session_id the session identifier, as extracted from
     *                   a tryAccess response
     * @return the Json to send to the DHT
     */
    private static String buildStartAccessMessage(String session_id) {
        StartAccessRequest message =
                new StartAccessRequest(session_id);
        String message_id = "random456-msg_id";
        // save message_id in a structure
        return serializeOutgoingJson(message, message_id);
    }


    /**
     * Serialize a request and the other objects to the Json
     * format accepted by the DHT
     *
     * @param message    the object containing the actual request
     * @param message_id the identifier of this specific request.
     *                   A response received from the DHT containing
     *                   this exact message_id refers to this request
     * @return the Json to send to the DHT
     */
    public static String serializeOutgoingJson(MessageContent message, String message_id) {
        InnerValue innerValue =
                new InnerValue(
                        message,
                        "pep_id",
                        message_id,
                        "topic_name_XXX",
                        "pub_topic_uuid");

        Command command = new Command("pep-command", innerValue);

        // set timestamp
        OuterValue outerValue = new OuterValue(System.currentTimeMillis(), command);

        RequestPubMessage pubMsg = new RequestPubMessage(outerValue);

        JsonOut outgoing = new JsonOut(pubMsg);

        String jsonOut = new GsonBuilder()
                .disableHtmlEscaping()
                .serializeNulls()
                .create()
                .toJson(outgoing);

        System.out.println("Sending " + message.getClass().getSimpleName() + " message:");
        System.out.println(new GsonBuilder()
                .disableHtmlEscaping()
                .serializeNulls()
                .setPrettyPrinting()
                .create()
                .toJson(outgoing));
        return jsonOut;
    }

    //    /**
//     * This method is executed when a command containing a re-evaluation
//     * is received from the dht.
//     */
    @Override
    public Message onGoingEvaluation(ReevaluationResponseMessage message) {
        return null;
    }


    @Override
    public String receiveResponse(Message message) {
        return null;
    }

//    public static String receiveResponse(String message) {
//
//    }


    /**
     * Deserialize the Json coming from the DHT
     *
     * @param message The Json, as received from the DHT
     * @return the object deserialized from the Json
     */
    private static JsonIn deserializeIncomingJson(String message) {
        return new GsonBuilder()
                .registerTypeAdapterFactory(typeFactory)
                .create().fromJson(message, JsonIn.class);
    }


    /**
     * Check if the topic_uuid contained in the deserialized
     * received message is the one this PEP is subscribed to
     *
     * @param jsonIn the JsonIn object, deserialized from the
     *               Json received from the DHT
     * @return true if the topic matches with the one this PEP
     * is subscribed to. False otherwise.
     */
    private static boolean isTopicOfInterest(JsonIn jsonIn) {
        if (!jsonIn
                .getVolatile()
                .getValue()
                .getCommand()
                .getValue()
                .getTopic_uuid()
                .equals(SUB_TOPIC_UUID)) {
            //System.out.println("Topic does not match the one we are subscribed to: " +
            //        "Message discarded.");
            return false;
        } else {
            return true;
        }
    }


    /**
     * Check the evaluation. In case of Permit, (TODO: grants the access and)
     * publishes a startAccess request on the DHT.
     * In any other case, denies the access.
     *
     * @param message the tryAccess response received from the DHT
     */
    private static void handleTryAccessResponse(TryAccessResponse message) {
        // check the evaluation
        if (!message.getEvaluation().equalsIgnoreCase("Permit")) {
            System.out.println("Access denied. TryAccess evaluated to: " + message.getEvaluation());
            return;
        }
        // if Permit, save the session_id, and make a startAccessRequest
        String session_id = message.getSession_id();
        String msg = buildStartAccessMessage(session_id);

        // send the request
        dhtClientEndPoint.sendMessage(msg);
    }


    /**
     * Check the evaluation. In case of Permit, grants the access.
     * In any other case, denies the access.
     *
     * @param message the startAccess response received from the DHT
     */
    private static void handleStartAccessResponse(StartAccessResponse message) {
        // check the evaluation
        if (!message.getEvaluation().equalsIgnoreCase("Permit")) {
            System.out.println("Access denied. StartAccess evaluated to: " + message.getEvaluation());
            return;
        }
        // if Permit, grant access
        System.out.println("Access is granted");
    }


    /**
     * This method is executed when a message with a valid topic
     * is received from the DHT.
     * It handles the message according to its type
     */
    private static void processMessage(MessageContent message) {
        if (message instanceof TryAccessResponse) {
            // handle try access response
            System.out.println("handle try access response");
            handleTryAccessResponse((TryAccessResponse) message);
            return;
        } else if (message instanceof StartAccessResponse) {
            // handle start access response
            System.out.println("handle start access response");
            handleStartAccessResponse((StartAccessResponse) message);
            return;
//        } else if (message instanceof EndAccessResponse) {
//            // handle start access response
//            return;
//        } else if (message instanceof RevokeAccessResponse) {
//            // handle start access response
//            return;
        } else {
            // class not recognized. Handle case
            // this should not happen since the deserialization would already have thrown an exception
            System.out.println("class not recognized. It might be a ResponseMessage");
            return;
        }
    }


    private static DHTUtils.MessageHandler setMessageHandler() {
        DHTUtils.MessageHandler messageHandler = new DHTUtils.MessageHandler() {
            public void handleMessage(String message) {
                //System.out.println("Received new message\n");

                MessageContent msg;
                try {
                    // deserialize json
                    JsonIn jsonIn = deserializeIncomingJson(message);

                    // check the topic is the one we are subscribed to
                    if (!isTopicOfInterest(jsonIn)) {
                        return;
                    }
                    System.out.println("Topic matches. Message type: " + jsonIn.getVolatile().getValue().getCommand().getValue().getMessage().getClass().getSimpleName());
                    msg = jsonIn.getVolatile().getValue().getCommand().getValue().getMessage();
                } catch (JsonSyntaxException e) {
                    System.err.println("Error deserializing Json. " + e.getMessage());
                    return;
                }

                processMessage(msg);

//                receiveResponse(message);
            }

//            private void process() {
//                System.out.println("sw");
//            }
        };
        return messageHandler;
    }


    public static class NewThread extends Thread {
        public void run() {
            while (true) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}