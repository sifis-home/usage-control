package it.cnr.iit.ucsdht;

import com.google.gson.JsonSyntaxException;
import it.cnr.iit.utility.dht.DHTClient;
import it.cnr.iit.utility.dht.jsondht.JsonIn;
import it.cnr.iit.utility.dht.jsondht.JsonOut;

import static it.cnr.iit.ucsdht.ManagerUtils.serializeAndSend;
import static it.cnr.iit.ucsdht.PEPMessageManager.isPepRegistered;
import static it.cnr.iit.ucsdht.PEPMessageManager.isRegisterRequest;
import static it.cnr.iit.ucsdht.UCSDht.UCS_SUB_TOPIC_UUID;
import static it.cnr.iit.utility.dht.DHTUtils.deserializeIncomingJson;
import static it.cnr.iit.utility.dht.DHTUtils.isTopicOfInterest;

public class DhtMessageHandler implements DHTClient.MessageHandler {


    /**
     * Deserialize the received message and check the topic matches
     * the one the UCS is subscribed to. If so, process the request
     * coming from the DHT
     *
     * @param message the message, a json string, coming from the DHT
     */
    public void handleMessage(String message) {
        JsonIn jsonIn;
        try {
            // deserialize json
            jsonIn = deserializeIncomingJson(message);

            // check the topic is the one we are subscribed to
            if (!isTopicOfInterest(jsonIn, UCS_SUB_TOPIC_UUID)) {
                return;
            }
            System.out.println("Topic matches. Message type: " + jsonIn.getVolatile().getValue().getCommand().getValue().getMessage().getClass().getSimpleName());
        } catch (JsonSyntaxException e) {
            //System.err.println("Error deserializing Json. Message discarded.");
            return;
        }
        switch (jsonIn.getVolatile().getValue().getCommand().getCommand_type()) {
            case "pep-command":
                if (!isRegisterRequest(jsonIn) && !isPepRegistered(jsonIn)) {
                    System.err.println("An unregistered PEP tried to make a request. Request discarded.");
                    return;
                }
                try {
                    PEPMessageManager.processMessage(jsonIn);
                } catch (Exception e) {
                    System.err.println("Error processing PEP request: " + e.getMessage());
                    JsonOut jsonOut =
                            PEPMessageManager.buildErrorResponseMessage(jsonIn, e.getMessage());
                    serializeAndSend(jsonOut);
                }
                break;
            case "pap-command":
                try {
                    PAPMessageManager.processMessage(jsonIn);
                } catch (Exception e) {
                    System.err.println("Error processing PAP request: " + e.getMessage());
                    JsonOut jsonOut =
                            PAPMessageManager.buildErrorResponseMessage(jsonIn, e.getMessage());
                    serializeAndSend(jsonOut);
                }
                break;
            case "pip-command":
                try {
                    PIPMessageManager.processMessage(jsonIn);
                } catch (Exception e) {
                    System.err.println("Error processing PIP request: " + e.getMessage());
                    JsonOut jsonOut =
                            PIPMessageManager.buildErrorResponseMessage(jsonIn, e.getMessage());
                    serializeAndSend(jsonOut);
                }
                break;
            default:
                System.err.println("Wrong command type. Request discarded.");
        }
    }

    @Override
    public void handleError() {
        System.err.println("Websocket error occurred");
    }

}
