package it.cnr.iit.utility.dht;

import com.google.gson.GsonBuilder;
import com.google.gson.typeadapters.RuntimeTypeAdapterFactory;
import it.cnr.iit.utility.dht.jsonpersistent.*;
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

    /**
     * Type adapter factory which has to be initialized with the right class before
     * calling the constructor. The class registered at this type adapter factory
     * must implement RequestPostTopicUuid.
     */
    private RuntimeTypeAdapterFactory<RequestPostTopicUuid> typeFactory;

    /**
     * Type adapter factory which has to be initialized with the right class before
     * calling the constructor. The class registered at this type adapter factory
     * must implement Persistent.
     */
    private RuntimeTypeAdapterFactory<Persistent> persistentTypeFactory;

    public DHTPersistentMessageClient(String uri) {
        this.uri = uri;
    }

    public DHTPersistentMessageClient(String uri, String topicName, String topicUuid,
                                      RuntimeTypeAdapterFactory<RequestPostTopicUuid> typeFactory,
                                      RuntimeTypeAdapterFactory<Persistent> persistentTypeFactory) {
        if (topicName == null || uri == null) {
            throw new RuntimeException("Topic name and URI cannot be null");
        }
        this.uri = uri;
        this.topicName = topicName;
        this.topicUuid = topicUuid;
        this.typeFactory = typeFactory;
        this.persistentTypeFactory = persistentTypeFactory;
    }

    @OnOpen
    public void onOpen(Session session) {
        this.session = session; // Store the session for later use
//        System.out.println("WebSocket connected");
    }


    /**
     * Try to deserialize the message as a JsonInResponse2RequestGetTopicUuid. If this
     * fails, return false.
     * Otherwise, extract the topicName and topicUuid. If they both match, return true.
     * @param message the json string
     * @return true if, after deserialization, topicName and topicUuid match the local
     * topicName and topicUuid fields. False otherwise.
     */
    private boolean isExpectedResponse2RequestGetTopicUuid(String message) {
        if (this.topicUuid == null) {
            return false;
        }
        try {
            JsonInResponse2RequestGetTopicUuid jsonInResponse = new GsonBuilder()
                    .registerTypeAdapterFactory(typeFactory)
                    .create()
                    .fromJson(message, JsonInResponse2RequestGetTopicUuid.class);

            String topicName = jsonInResponse.getResponse().getValue().getTopic_name();
            String topicUuid = jsonInResponse.getResponse().getValue().getTopic_uuid();
            return this.topicName.equals(topicName) && this.topicUuid.equals(topicUuid);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Try to deserialize the message as a JsonInResponse2RequestGetTopicName. If this
     * fails, return false.
     * Otherwise, extract the topicName. If topicName matches, return true.
     * @param message the json string
     * @return true if, after deserialization, topicName matches the local topicName.
     * False otherwise.
     */
    private boolean isExpectedResponse2RequestGetTopicName(String message) {
        try {
            JsonInResponse2RequestGetTopicName jsonInResponse = new GsonBuilder()
                    .registerTypeAdapterFactory(typeFactory)
                    .create()
                    .fromJson(message, JsonInResponse2RequestGetTopicName.class);

            String topicName = jsonInResponse.getResponse().getValue().get(0).getTopic_name();
            return this.topicName.equals(topicName);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Try to deserialize the message as a jsonInPersistent. If this fails, return false.
     * Otherwise, extract the topicName and topicUuid. If they both match, return true.
     * @param message the json string
     * @return true if, after deserialization, topicName and topicUuid match the local
     * topicName and topicUuid fields. False otherwise.
     */
    private boolean isExpectedPersistent(String message) {
        if (this.topicUuid == null) {
            return false;
        }
        try {
            JsonInPersistent jsonInPersistent = new GsonBuilder()
                    .registerTypeAdapterFactory(persistentTypeFactory)
                    .create()
                    .fromJson(message, JsonInPersistent.class);

            String topicName = jsonInPersistent.getPersistent().getTopic_name();
            String topicUuid = jsonInPersistent.getPersistent().getTopic_uuid();
            return this.topicName.equals(topicName) && this.topicUuid.equals(topicUuid);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     *      This is an empty message, and it could be the right answer
     *      to our request. This happens when no messages for the topic
     *      name and topic uuid are found in the DHT.
     *      However, we cannot know with certainty that this is the answer
     *      to our request.
     *      Therefore, to minimize false positives, we return this string
     *      to the caller, which should make another request equal to the
     *      previous one. If, for a number of attempts, the caller receives
     *      the empty response string, it should assume that the DHT
     *      contains no records for the given topic name and uuid.
     * @param message the json string
     * @return true if the input matches the empty response, false otherwise.
     */
    private boolean isEmptyResponse(String message) {
        return message.equals("{\"Response\":{\"value\":{}}}") || message.equals("{\"Response\":{\"value\":[]}}");
    }

    // todo (?): implement a mechanism that calls the lock.notify() after
    //           receiving too many "wrong" messages
    @OnMessage
    public void onMessage(String message, Session session) {
        String truncatedMessage;
        if (message.length() > 100) {
            truncatedMessage = message.substring(0,99) + "... (remainder omitted for readability)";
        } else {
            truncatedMessage = message;
        }

        if (!(isExpectedResponse2RequestGetTopicUuid(message)
                || isExpectedResponse2RequestGetTopicName(message)
                || isExpectedPersistent(message))
                && !isEmptyResponse(message)) {
            // discard message
//                System.out.println("DHTPersistentMessageClient: message discarded: " + truncatedMessage);
            return;
        }
        else {
            System.out.print("DHTPersistentMessageClient: " +
                    "Received response for topicName: " + topicName);
            if (topicUuid != null) {
                System.out.println(", topicUuid: " + topicUuid);
            } else {
                System.out.println();
            }
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