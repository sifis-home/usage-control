package it.cnr.iit.ucsdht;

import it.cnr.iit.utility.dht.jsonvolatile.JsonIn;
import it.cnr.iit.utility.dht.jsonvolatile.JsonOut;
import it.cnr.iit.utility.dht.jsonvolatile.MessageContent;

import static it.cnr.iit.ucsdht.UCSDht.dhtClientEndPoint;
import static it.cnr.iit.utility.dht.DHTUtils.serializeOutgoingJson;

public class ManagerUtils {
    /**
     * Serialize the JsonOut object passed as argument and publish the
     * Json string on the DHT.
     *
     * @param jsonOut the object to serialize and send
     */
    protected static void serializeAndSend(JsonOut jsonOut) {
        // serialize the object to a json string
        String msg = serializeOutgoingJson(jsonOut);

        // send the request
        if (!dhtClientEndPoint.sendMessage(msg)) {
            System.err.println("Error sending the message to the DHT");
        }
    }

    static MessageContent getMessageFromJson(JsonIn jsonIn) {
        return jsonIn.getVolatile().getValue().getCommand().getValue().getMessage();
    }

    static String getIdFromJson(JsonIn jsonIn) {
        return jsonIn.getVolatile().getValue().getCommand().getValue().getId();
    }

    static String getMessageIdFromJson(JsonIn jsonIn) {
        return jsonIn.getVolatile().getValue().getCommand().getValue().getMessage().getMessage_id();
    }


}
