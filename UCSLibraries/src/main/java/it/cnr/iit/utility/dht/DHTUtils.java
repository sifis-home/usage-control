package it.cnr.iit.utility.dht;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.typeadapters.RuntimeTypeAdapterFactory;
import it.cnr.iit.ucs.constants.PURPOSE;
import it.cnr.iit.utility.dht.jsondht.*;
import it.cnr.iit.utility.dht.jsondht.addpolicy.AddPolicyRequest;
import it.cnr.iit.utility.dht.jsondht.addpolicy.AddPolicyResponse;
import it.cnr.iit.utility.dht.jsondht.deletepolicy.DeletePolicyRequest;
import it.cnr.iit.utility.dht.jsondht.deletepolicy.DeletePolicyResponse;
import it.cnr.iit.utility.dht.jsondht.endaccess.EndAccessRequest;
import it.cnr.iit.utility.dht.jsondht.endaccess.EndAccessResponse;
import it.cnr.iit.utility.dht.jsondht.error.ErrorResponse;
import it.cnr.iit.utility.dht.jsondht.getpolicy.GetPolicyRequest;
import it.cnr.iit.utility.dht.jsondht.getpolicy.GetPolicyResponse;
import it.cnr.iit.utility.dht.jsondht.listpolicies.ListPoliciesRequest;
import it.cnr.iit.utility.dht.jsondht.listpolicies.ListPoliciesResponse;
import it.cnr.iit.utility.dht.jsondht.reevaluation.ReevaluationResponse;
import it.cnr.iit.utility.dht.jsondht.registration.RegisterRequest;
import it.cnr.iit.utility.dht.jsondht.registration.RegisterResponse;
import it.cnr.iit.utility.dht.jsondht.startaccess.StartAccessRequest;
import it.cnr.iit.utility.dht.jsondht.startaccess.StartAccessResponse;
import it.cnr.iit.utility.dht.jsondht.tryaccess.TryAccessRequest;
import it.cnr.iit.utility.dht.jsondht.tryaccess.TryAccessResponse;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;

public class DHTUtils {

    private static final RuntimeTypeAdapterFactory<MessageContent> typeFactory = RuntimeTypeAdapterFactory
            .of(MessageContent.class, "purpose")
            .registerSubtype(RegisterRequest.class, PURPOSE.REGISTER.name())
            .registerSubtype(RegisterResponse.class, PURPOSE.REGISTER_RESPONSE.name())
            .registerSubtype(TryAccessRequest.class, PURPOSE.TRY.name())
            .registerSubtype(TryAccessResponse.class, PURPOSE.TRY_RESPONSE.name())
            .registerSubtype(StartAccessRequest.class, PURPOSE.START.name())
            .registerSubtype(StartAccessResponse.class, PURPOSE.START_RESPONSE.name())
            .registerSubtype(EndAccessRequest.class, PURPOSE.END.name())
            .registerSubtype(EndAccessResponse.class, PURPOSE.END_RESPONSE.name())
            .registerSubtype(ReevaluationResponse.class, PURPOSE.REEVALUATION_RESPONSE.name())
            .registerSubtype(AddPolicyRequest.class, PURPOSE.ADD_POLICY.name())
            .registerSubtype(AddPolicyResponse.class, PURPOSE.ADD_POLICY_RESPONSE.name())
            .registerSubtype(DeletePolicyRequest.class, PURPOSE.DELETE_POLICY.name())
            .registerSubtype(DeletePolicyResponse.class, PURPOSE.DELETE_POLICY_RESPONSE.name())
            .registerSubtype(ListPoliciesRequest.class, PURPOSE.LIST_POLICIES.name())
            .registerSubtype(ListPoliciesResponse.class, PURPOSE.LIST_POLICIES_RESPONSE.name())
            .registerSubtype(GetPolicyRequest.class, PURPOSE.GET_POLICY.name())
            .registerSubtype(GetPolicyResponse.class, PURPOSE.GET_POLICY_RESPONSE.name())
            .registerSubtype(ErrorResponse.class, PURPOSE.ERROR_RESPONSE.name());


    /**
     * Build a JsonOut object
     *
     * @param message     the object containing a request or a response
     * @param id          the name of the entity communicating with the UCS
     * @param topic_name  the name of the topic
     * @param topic_uuid  the unique identifier of the topic
     * @param commandType the type of command
     * @return the object to be then serialized and sent to the DHT
     */
    public static JsonOut buildOutgoingJsonObject(MessageContent message, String id,
                                                  String topic_name, String topic_uuid,
                                                  String commandType) {
        InnerValue innerValue =
                new InnerValue(
                        message,
                        id,
                        topic_name,
                        topic_uuid);

        Command command = new Command(commandType, innerValue);

        // set timestamp
        OuterValue outerValue = new OuterValue(System.currentTimeMillis(), command);

        RequestPubMessage pubMsg = new RequestPubMessage(outerValue);

        return new JsonOut(pubMsg);
    }

    /**
     * Serialize a JsonOut object to a Json string
     * format accepted by the DHT
     *
     * @param jsonOut the object to be serialized
     * @return the Json string to send to the DHT
     */
    public static String serializeOutgoingJson(JsonOut jsonOut) {

        String outgoing = new GsonBuilder()
                .disableHtmlEscaping()
                .serializeNulls()
                .create()
                .toJson(jsonOut);

        String messageClass = jsonOut
                .getRequestPubMessage()
                .getValue()
                .getCommand()
                .getValue()
                .getMessage()
                .getClass()
                .getSimpleName();

        System.out.println("Sending " + messageClass + " message:");
        System.out.println(new GsonBuilder()
                .disableHtmlEscaping()
                .serializeNulls()
                .setPrettyPrinting()
                .create()
                .toJson(jsonOut));
        return outgoing;
    }


    /**
     * Deserialize the Json string coming from the DHT
     *
     * @param message The Json string, as received from the DHT
     * @return the object deserialized from the Json string
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
        try {
            return jsonIn
                    .getVolatile()
                    .getValue()
                    .getCommand()
                    .getValue()
                    .getTopic_uuid()
                    .equals(topic);
        } catch (Exception e) {
            throw new JsonSyntaxException(e.getMessage());
        }
    }


    /**
     * Wait for a connection to the DHT before proceeding
     *
     * @param dhtWebsocketUri the URI of the WebSocket interface for the DHT
     * @return true when the connection succeeds
     */
    public static boolean isDhtReachable(String dhtWebsocketUri, int timeout, int attempts) {

        Socket socket = null;
        URI dhtUri = URI.create(dhtWebsocketUri);

        long connectionTime = System.currentTimeMillis();
        for (int i = 0; i < attempts; i++) {
            try {
                connectionTime = System.currentTimeMillis();
                socket = new Socket();
                socket.connect(new InetSocketAddress(dhtUri.getHost(), dhtUri.getPort()), timeout);
                break;
            } catch (Exception e) {
                System.err.println("Failed: Attempt " + (i + 1) + " of " + attempts + " to reach DHT at: " + dhtWebsocketUri);
                if (i == attempts - 1) {
                    return false;
                }
                try {
                    long currentTime = System.currentTimeMillis();
                    long timeLeftToSleep = connectionTime + timeout - currentTime > 0 ?
                            connectionTime + timeout - currentTime : 0L;

                    Thread.sleep(timeLeftToSleep);
                } catch (InterruptedException ex) {
                    System.err.println("Failed to sleep while waiting for the next attempt");
                }
            }
        }
        try {
            assert socket != null;
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }
}
