package it.cnr.iit.ucs.pip;

import com.google.gson.GsonBuilder;
import it.cnr.iit.ucs.properties.components.PipProperties;
import it.cnr.iit.utility.dht.DHTPersistentMessageClient;
import it.cnr.iit.utility.dht.jsonpersistent.JsonOutRequestGetTopicUuid;
import it.cnr.iit.utility.dht.jsonpersistent.RequestGetTopicUuid;
import it.cnr.iit.utility.errorhandling.Reject;
import it.cnr.iit.xacml.Attribute;

import java.util.Map;

import static it.cnr.iit.utility.dht.DHTUtils.isDhtReachable;

public abstract class AbstractPIPWebSocket extends PIPBase {


    public String dhtUri;
    public String topicName;
    public String topicUuid;
    public DHTPersistentMessageClient client;


    public AbstractPIPWebSocket(PipProperties properties) {
        super(properties);
    }

    @Override
    public boolean init(PipProperties properties) {
        try {
            dhtUri = properties.getAdditionalProperties().get("dhtUri");
            Reject.ifNull(dhtUri, "DHT URI not specified");
            topicName = properties.getAdditionalProperties().get("topicName");
            Reject.ifNull(topicName, "Topic name not specified");
            topicUuid = properties.getAdditionalProperties().get("topicUuid");
            Reject.ifNull(topicUuid, "Topic UUID not specified");

            client = new DHTPersistentMessageClient(dhtUri);

            for (Map<String, String> attributeMap : properties.getAttributes()) {

                Attribute attribute = new Attribute();
                buildAttribute(attribute, attributeMap);

                addAttribute(attribute);

                // timer for polling the value of the attribute
                PIPSubscriberTimer subscriberTimer = new PIPSubscriberTimer(this);
                subscriberTimer.setRate(properties.getRefreshRate());
                subscriberTimer.start();
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }


    public String performRequestGetTopicUuid() {
        // get the status from the dht
        String response = null;
        if (isDhtReachable(dhtUri, 2000, Integer.MAX_VALUE)) {
            RequestGetTopicUuid requestGetTopicUuid =
                    new RequestGetTopicUuid(topicName, topicUuid);

            JsonOutRequestGetTopicUuid jsonOut = new JsonOutRequestGetTopicUuid(requestGetTopicUuid);
            String request = new GsonBuilder()
                    .disableHtmlEscaping()
                    .serializeNulls()
                    .create()
                    .toJson(jsonOut);

            response = client.sendRequestAndWaitForResponse(request);
            client.closeConnection();
        } else {
            System.err.println("Unable to connect to the DHT");
        }
        return response;
    }

}
