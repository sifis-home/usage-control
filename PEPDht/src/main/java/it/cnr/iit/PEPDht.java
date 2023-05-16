package it.cnr.iit;

import it.cnr.iit.json.*;
import it.cnr.iit.ucs.message.Message;
import it.cnr.iit.ucs.message.reevaluation.ReevaluationResponseMessage;
import it.cnr.iit.ucs.pep.PEPInterface;
import it.cnr.iit.utility.DHTUtils;

import com.google.gson.GsonBuilder;

import jakarta.websocket.OnMessage;
import jakarta.websocket.Session;

public class PEPDht extends DHTUtils implements PEPInterface {

    public static void main(String[] args) {

        setLogging(true);
        establishConnection("ws://localhost:3000/ws");
        tryAccess();

        NewThread t = new NewThread();
        t.start();
    }


    /**
     * This method is used to send a tryAcces request.
     * The tryAccess request is published as a JSON request on the dht.
     */
    public static void tryAccess() {
        // compile the request (json)
        String msg = buildTryAccessMessage();

        // send the request
        sendMessage(msg);
    }

    private static String buildTryAccessMessage() {

        it.cnr.iit.json.tryaccess.Message message =
                new it.cnr.iit.json.tryaccess.Message("request", "policy");
//        it.cnr.iit.json.tryaccess.Message message = new it.cnr.iit.json.tryaccess.Message();
//        message.setRequest("xacml-request");
//        message.setPolicy(null);

        InnerValue innerValue =
                new InnerValue(
                        "try",
                        message,
                        "pep_id",
                        "msg_id",
                        "topic_name_XXX",
                        "topic_uuid_YYY");
//        InnerValue innerValue = new InnerValue();
//        innerValue.setPurpose("try");
//        innerValue.setMessage(message);
//        innerValue.setPep_id("pep-id");
//        innerValue.setMessage_id("msg_id");
//        innerValue.setTopic_name("topic_name_XXX");
//        innerValue.setTopic_uuid("topic_uuid_YYY");

        Command command = new Command("pep-command", innerValue);
//        Command command = new Command();
//        command.setCommand_type("pep-command");
//        command.setValue(innerValue);

        OuterValue outerValue = new OuterValue(System.currentTimeMillis(), command);
//        OuterValue outerValue = new OuterValue();
//        outerValue.setTimestamp(System.currentTimeMillis());
//        outerValue.setCommand(command);

        RequestPubMessage pubMsg = new RequestPubMessage(outerValue);
//        RequestPubMessage pubMsg = new RequestPubMessage();
//        pubMsg.setValue(outerValue);

        JsonOut outgoing = new JsonOut(pubMsg);
//        JsonOut outgoing = new JsonOut();
//        outgoing.setRequestPubMessage(pubMsg);

        String jsonOut = new GsonBuilder()
                .disableHtmlEscaping()
                .serializeNulls()
                .create()
                .toJson(outgoing);

        System.out.println(new GsonBuilder()
                .disableHtmlEscaping()
                .serializeNulls()
                .setPrettyPrinting()
                .create()
                .toJson(outgoing));
        return jsonOut;
    }

    /**
     * This method is executed when a command containing a re-evaluation
     * is received from the dht.
     */
    @Override
    public Message onGoingEvaluation(ReevaluationResponseMessage message) {
        return null;
    }

    /**
     * This method is executed when a try, start, or end request
     * is received from the dht
     * @param message
     * @return
     */
    @Override
    public String receiveResponse( Message message ) {
        System.out.println("New request received from the DHT");
        return null;
    }

    @Override
    @OnMessage
    public String onMessage(String message, Session session) {
        receiveResponse(null);
        return null;
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