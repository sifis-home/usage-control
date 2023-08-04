package it.cnr.iit.utility.dht;

import com.google.gson.Gson;
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


    private boolean areTopicNameAndTopicUuidInitialized() {
        return topicName != null && topicUuid != null;
    }


    /**
     * Try to deserialize the message as a jsonInResponse. If this fails, try to
     * deserialize it as a jsonInPersistent message. If both the attempt fail
     * we cannot deserialize the json and return false.
     * If one attempt is successful, extract the topicName and topicUuid. If they
     * both match, return true
     * @param message the json string
     * @return true if, after deserialization, topicName and topicUuid match.
     * False otherwise.
     */
    private boolean messageContainsRightTopicNameAndTopicUuid(String message) {
        try {
            JsonInResponse jsonInResponse = new GsonBuilder()
                    .registerTypeAdapterFactory(typeFactory)
                    .create()
                    .fromJson(message, JsonInResponse.class);

            String topicName = jsonInResponse.getResponse().getValue().getTopic_name();
            String topicUuid = jsonInResponse.getResponse().getValue().getTopic_uuid();
            return this.topicName.equals(topicName) && this.topicUuid.equals(topicUuid);
        } catch (Exception e) {
            try {
                JsonInPersistent jsonInPersistent = new GsonBuilder()
                        .registerTypeAdapterFactory(persistentTypeFactory)
                        .create()
                        .fromJson(message, JsonInPersistent.class);

                String topicName = jsonInPersistent.getPersistent().getTopic_name();
                String topicUuid = jsonInPersistent.getPersistent().getTopic_uuid();
                return this.topicName.equals(topicName) && this.topicUuid.equals(topicUuid);
            } catch (Exception e2) {
                return false;
            }
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
        // todo: OR it with the empty response for the RequestGetTopicName,
        //       which returns an empty array
        return message.equals("{\"Response\":{\"value\":{}}}");
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

        if (areTopicNameAndTopicUuidInitialized()) {
            if (!messageContainsRightTopicNameAndTopicUuid(message) && !isEmptyResponse(message)) {
                // discard message
//                System.out.println("DHTPersistentMessageClient: message discarded: " + truncatedMessage);
                return;
            }
            else {
                System.out.println("DHTPersistentMessageClient: " +
                        "Received response for " + topicName + ", " + topicUuid);
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