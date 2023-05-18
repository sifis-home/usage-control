package it.cnr.iit.utility.dht;

import com.google.gson.GsonBuilder;
import com.google.gson.typeadapters.RuntimeTypeAdapterFactory;
import it.cnr.iit.ucs.constants.PURPOSE;
import it.cnr.iit.utility.dht.jsondht.*;
import it.cnr.iit.utility.dht.jsondht.startaccess.StartAccessRequest;
import it.cnr.iit.utility.dht.jsondht.startaccess.StartAccessResponse;
import it.cnr.iit.utility.dht.jsondht.tryaccess.TryAccessRequest;
import it.cnr.iit.utility.dht.jsondht.tryaccess.TryAccessResponse;

public class DHTUtils {

    private static final RuntimeTypeAdapterFactory<MessageContent> typeFactory = RuntimeTypeAdapterFactory
            .of(MessageContent.class, "purpose")
            .registerSubtype(TryAccessRequest.class, PURPOSE.TRY.name())
            .registerSubtype(TryAccessResponse.class, PURPOSE.TRY_RESPONSE.name())
            .registerSubtype(StartAccessRequest.class, PURPOSE.START.name())
            .registerSubtype(StartAccessResponse.class, PURPOSE.START_RESPONSE.name());


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
    public static String serializeOutgoingJson(MessageContent message, String message_id,
                                               String pep_id, String topic_name, String topic_uuid,
                                               String commandType) {
        InnerValue innerValue =
                new InnerValue(
                        message,
                        pep_id,
                        message_id,
                        topic_name,
                        topic_uuid);

        Command command = new Command(commandType, innerValue);

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


    /**
     * Deserialize the Json coming from the DHT
     *
     * @param message The Json, as received from the DHT
     * @return the object deserialized from the Json
     */
    public static JsonIn deserializeIncomingJson(String message) {
        return new GsonBuilder()
                .registerTypeAdapterFactory(typeFactory)
                .create().fromJson(message, JsonIn.class);
    }

    /**
     * Check if the topic_uuid contained in the deserialized
     * received message is the one this entity is subscribed to,
     * as specified in the 'topic' argument
     *
     * @param jsonIn the JsonIn object, deserialized from the
     *               Json received from the DHT
     * @param topic  the topic this entity is subscribed to
     * @return true if the topic matches with the one thi
     * is subscribed to. False otherwise.
     */
    public static boolean isTopicOfInterest(JsonIn jsonIn, String topic) {
        //System.out.println("Topic does not match the one we are subscribed to: " +
        //        "Message discarded.");
        return jsonIn
                .getVolatile()
                .getValue()
                .getCommand()
                .getValue()
                .getTopic_uuid()
                .equals(topic);
    }
}
