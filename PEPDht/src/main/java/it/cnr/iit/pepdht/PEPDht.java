package it.cnr.iit.pepdht;

import com.google.gson.JsonSyntaxException;
import it.cnr.iit.pepdht.track.AccessTracker;
import it.cnr.iit.ucs.message.Message;
import it.cnr.iit.ucs.message.reevaluation.ReevaluationResponseMessage;
import it.cnr.iit.utility.dht.DHTClient;
import it.cnr.iit.utility.dht.jsondht.JsonIn;
import it.cnr.iit.utility.dht.jsondht.JsonOut;
import it.cnr.iit.utility.dht.jsondht.MessageContent;
import it.cnr.iit.utility.dht.jsondht.endaccess.EndAccessRequest;
import it.cnr.iit.utility.dht.jsondht.endaccess.EndAccessResponse;
import it.cnr.iit.utility.dht.jsondht.reevaluation.ReevaluationResponse;
import it.cnr.iit.utility.dht.jsondht.startaccess.StartAccessRequest;
import it.cnr.iit.utility.dht.jsondht.startaccess.StartAccessResponse;
import it.cnr.iit.utility.dht.jsondht.tryaccess.TryAccessRequest;
import it.cnr.iit.utility.dht.jsondht.tryaccess.TryAccessResponse;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static it.cnr.iit.utility.dht.DHTUtils.*;

public class PEPDht {

    // map containing the requests sent and for which a response has not been received yet.
    // The messageId is the key and the json object (the request) is the value
    private static final ConcurrentMap<String, JsonOut> unansweredMap = new ConcurrentHashMap<>();

    // object that keeps track of the status of requests and answers.
    // Useful to retrieve the sessionId from a messageId
    private static final AccessTracker accessTracker = new AccessTracker();
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
    private static void tryAccess() {
        // build the json object
        JsonOut jsonOut = buildTryAccessMessage();
        serializeAndSend(jsonOut);
    }


    /**
     * Build a tryAccess request as a Json to send to the DHT
     *
     * @return the Json object to send to the DHT
     */
    private static JsonOut buildTryAccessMessage() {
        TryAccessRequest message =
                new TryAccessRequest("random123-msg_id", "request", "policy");
        //new TryAccessRequest(String.valueOf(UUID.randomUUID()), "request", "policy");

        return buildOutgoingJsonObject(message, PEP_ID, PUB_TOPIC_NAME, PUB_TOPIC_UUID, COMMAND_TYPE);
    }

    /**
     * Publish a startAccess request on the DHT
     *
     * @param sessionId the session identifier
     */
    private static void startAccess(String sessionId) {
        // build the json object
        JsonOut jsonOut = buildStartAccessMessage(sessionId);
        serializeAndSend(jsonOut);
    }


    /**
     * Build a startAccess request as a Json to send to the DHT
     *
     * @param sessionId the session identifier, as extracted from
     *                  a tryAccess response
     * @return the Json to send to the DHT
     */
    private static JsonOut buildStartAccessMessage(String sessionId) {
        StartAccessRequest message =
                new StartAccessRequest("random456-msg_id", sessionId);
        //new StartAccessRequest(String.valueOf(UUID.randomUUID()), session_id);

        return buildOutgoingJsonObject(message, PEP_ID, PUB_TOPIC_NAME, PUB_TOPIC_UUID, COMMAND_TYPE);
    }


    /**
     * Publish an endAccess request on the DHT
     *
     * @param sessionId the session identifier
     */
    private static void endAccess(String sessionId) {
        // build the json object
        JsonOut jsonOut = buildEndAccessMessage(sessionId);
        serializeAndSend(jsonOut);
    }


    /**
     * Build an endAccess request as a Json to send to the DHT
     *
     * @param sessionId the session identifier
     * @return the Json to send to the DHT
     */
    private static JsonOut buildEndAccessMessage(String sessionId) {
        EndAccessRequest message =
                new EndAccessRequest("random789-msg_id", sessionId);
        //new EndAccessRequest(String.valueOf(UUID.randomUUID()), session_id);

        return buildOutgoingJsonObject(message, PEP_ID, PUB_TOPIC_NAME, PUB_TOPIC_UUID, COMMAND_TYPE);
    }


    /**
     * Serialize the JsonOut object passed as argument and publish the
     * Json string on the DHT. If this succeeds, update the structures
     * to keep track of the access and add this request to the unanswered.
     *
     * @param jsonOut the object to serialize and send
     */
    private static void serializeAndSend(JsonOut jsonOut) {
        // serialize the object to a json string
        String msg = serializeOutgoingJson(jsonOut);

        // send the request
        if (dhtClientEndPoint.sendMessage(msg)) {
            MessageContent message = jsonOut.getRequestPubMessage().getValue().getCommand().getValue().getMessage();
            unansweredMap.put(message.getMessage_id(), jsonOut);
            accessTracker.add(message);
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
        // if Permit:
        //  - grant access
        //  - send a start access request
        startAccess(message.getSession_id());
    }


    /**
     * Check the evaluation. In case is not Permit, denies the access.
     *
     * @param message the startAccess response received from the DHT
     */
    private static void handleStartAccessResponse(StartAccessResponse message) {
        // check the evaluation
        if (!message.getEvaluation().equalsIgnoreCase("Permit")) {
            System.out.println("Access denied. StartAccess evaluated to: " + message.getEvaluation());
            // terminate access
            return;
        }
        // if Permit, keep granting access
        System.out.println("Ongoing access is granted");

        // simulate usage for some time
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        // access terminates naturally
        // send an end access request
        String sessionId = accessTracker.getSessionId(message.getMessage_id());
        endAccess(sessionId);
    }


    /**
     * Print the evaluation. Actually, the evaluation doesn't matter for
     * the access, since it is already terminated.
     *
     * @param message the startAccess response received from the DHT
     */
    private static void handleEndAccessResponse(EndAccessResponse message) {
        // print the evaluation
        System.out.println("EndAccess evaluated to: " + message.getEvaluation());
    }


    /**
     * This method is executed when a message with a valid topic
     * is received from the DHT.
     * It handles the message according to its type
     */
    private static void processMessage(MessageContent message) {

        // check that we are waiting a response for this message_id
        if (!(message instanceof ReevaluationResponse)) {
            if (!unansweredMap.containsKey(message.getMessage_id()
            )) {
                System.out.println("The received message_id is not associated with " +
                        "any unanswered request that we sent.");
                return;
            } else {
                unansweredMap.remove(message.getMessage_id());
            }
        }

        accessTracker.add(message);

        if (message instanceof TryAccessResponse) {
            // handle try access response
            System.out.println("handle try access response");
            handleTryAccessResponse((TryAccessResponse) message);
        } else if (message instanceof StartAccessResponse) {
            // handle start access response
            System.out.println("handle start access response");
            handleStartAccessResponse((StartAccessResponse) message);
        } else if (message instanceof EndAccessResponse) {
            // handle end access response
            handleEndAccessResponse((EndAccessResponse) message);
        } else if (message instanceof ReevaluationResponse) {
            // handle reevaluation response
            System.out.println("handle reevaluation response");
        } else {
            // class not recognized. Handle case
            // this should not happen since the deserialization would already have thrown an exception
            System.err.println("class not recognized. It might be a ResponseMessage");
        }
    }


    /**
     * Message handler that implements the handleMessage method deserializes the received
     *
     * @return the message handler
     */
    private static DHTClient.MessageHandler setMessageHandler() {
        DHTClient.MessageHandler messageHandler = new DHTClient.MessageHandler() {
            /**
             * Deserialize the received message and check the topic matches
             * the one the PEP is subscribed to. If so, process the response
             * coming from the DHT
             *
             * @param message the message, a json string, coming from the DHT
             */
            public void handleMessage(String message) {
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
            }
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