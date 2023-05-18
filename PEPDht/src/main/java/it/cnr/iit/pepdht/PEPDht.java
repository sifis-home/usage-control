package it.cnr.iit.pepdht;

import com.google.gson.JsonSyntaxException;
import it.cnr.iit.ucs.message.Message;
import it.cnr.iit.ucs.message.reevaluation.ReevaluationResponseMessage;
import it.cnr.iit.ucs.pep.PEPInterface;

import it.cnr.iit.utility.dht.DHTClient;
import it.cnr.iit.utility.dht.jsondht.JsonIn;
import it.cnr.iit.utility.dht.jsondht.MessageContent;
import it.cnr.iit.utility.dht.jsondht.startaccess.StartAccessRequest;
import it.cnr.iit.utility.dht.jsondht.startaccess.StartAccessResponse;
import it.cnr.iit.utility.dht.jsondht.tryaccess.TryAccessRequest;
import it.cnr.iit.utility.dht.jsondht.tryaccess.TryAccessResponse;

import java.net.URI;
import java.net.URISyntaxException;

import static it.cnr.iit.utility.dht.DHTUtils.*;

public class PEPDht implements PEPInterface {

    private static DHTClient dhtClientEndPoint;
    private static final String COMMAND_TYPE = "pep-command";
    private static final String SUB_COMMAND_TYPE = "ucs-command";
    private static final String PEP_ID = "pep-0";
    private static final String PUB_TOPIC_NAME = "topic-name";
    private static final String PUB_TOPIC_UUID = "topic-uuid-the-ucs-is-subscribed-to";
    private static final String SUB_TOPIC_UUID = "topic-uuid-the-pep-is-subscribed-to";




    public static void main(String[] args) {

        try {
            dhtClientEndPoint = new DHTClient(
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
        return serializeOutgoingJson(message, message_id, PEP_ID, PUB_TOPIC_NAME, PUB_TOPIC_UUID, COMMAND_TYPE);
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
        return serializeOutgoingJson(message, message_id, PEP_ID, PUB_TOPIC_NAME, PUB_TOPIC_UUID, COMMAND_TYPE);
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


    private static DHTClient.MessageHandler setMessageHandler() {
        DHTClient.MessageHandler messageHandler = new DHTClient.MessageHandler() {
            public void handleMessage(String message) {
                //System.out.println("Received new message\n");

                MessageContent msg;
                try {
                    // deserialize json
                    JsonIn jsonIn = deserializeIncomingJson(message);

                    // check the topic is the one we are subscribed to
                    if (!isTopicOfInterest(jsonIn, SUB_TOPIC_UUID)) {
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