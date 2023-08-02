package it.cnr.iit.utility.dht;

import jakarta.websocket.*;

import java.net.URI;

@ClientEndpoint
public class DHTPersistentMessageClient {


    private final Object lock = new Object();
    private String response;
    private Session session;
    private final String uri;
    private String topicName;
    private String topicUuid;

    public DHTPersistentMessageClient(String uri) {
        this.uri = uri;
    }

    public DHTPersistentMessageClient(String uri, String topicName, String topicUuid) {
        this.uri = uri;
        this.topicName = topicName;
        this.topicUuid = topicUuid;
    }

    @OnOpen
    public void onOpen(Session session) {
        this.session = session; // Store the session for later use
//        System.out.println("WebSocket connected");
    }


    private boolean areTopicNameAndTopicUuidInitialized() {
        return topicName != null && topicUuid != null;
    }

    // This is not the ultimate check. We should parse the response as a json
    private boolean messageContainsRightTopicNameAndTopicUuid(String message) {
        return (message.contains("\"topic_name\":\"" + topicName + "\"")
                && message.contains("\"topic_uuid\":\"" + topicUuid + "\""));
    }


    // This is an empty message, and it could be the right answer
    // to our request. This happens when no messages for the topic
    // name and topic uuid are found in the DHT.
    // However, we cannot know with certainty that this is the answer
    // to our request.
    // Therefore, to minimize false positives, we return this string
    // to the caller, which should make another request equal to the
    // previous one. If, for a number of attempts, the caller receives
    // the empty response string, it should assume that the DHT
    // contains no records for the given topic name and uuid.
    private boolean isEmptyResponse(String message) {
        return message.equals("{\"Response\":{\"value\":{}}}");
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        if (areTopicNameAndTopicUuidInitialized()) {
            if (!messageContainsRightTopicNameAndTopicUuid(message) && !isEmptyResponse(message)) {
                // discard message
                System.out.println("DHTPersistentMessageClient: message discarded: " + message);
                return;
            }
            else {
                System.out.println("DHTPersistentMessageClient: " +
                        "Received response for " + topicName + ", " + topicUuid);
            }
        }

        String truncatedMessage;
        if (message.length() > 50) {
            truncatedMessage = message.substring(0,49) + "... (remainder omitted for readability)";
        } else {
            truncatedMessage = message;
        }
        System.out.println("Received response: " + truncatedMessage);
        response = message;
        synchronized (lock) {
            lock.notify(); // Notify the waiting thread that the response has been received.
        }
    }

    @OnClose
    public void onClose(Session session) {
//        System.out.println("Session " + session.getId() + " closed.");
    }

    public void closeConnection() {
        try {
            if (session != null) {
                session.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String sendRequestAndWaitForResponse(String request) {

        try {
            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
            container.connectToServer(this, new URI(this.uri));

            synchronized (lock) {
                this.session.getBasicRemote().sendText(request); // Send the request using the stored session
                lock.wait(); // Wait for the response to be received
            }
            return response;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public String getTopicName() {
        return topicName;
    }

    public void setTopicName(String topicName) {
        this.topicName = topicName;
    }

    public String getTopicUuid() {
        return topicUuid;
    }

    public void setTopicUuid(String topicUuid) {
        this.topicUuid = topicUuid;
    }

}