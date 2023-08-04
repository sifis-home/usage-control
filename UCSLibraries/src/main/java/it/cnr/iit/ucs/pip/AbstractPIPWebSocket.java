package it.cnr.iit.ucs.pip;

import com.google.gson.GsonBuilder;
import com.google.gson.typeadapters.RuntimeTypeAdapterFactory;
import it.cnr.iit.ucs.exceptions.PIPException;
import it.cnr.iit.ucs.properties.components.PipProperties;
import it.cnr.iit.utility.dht.DHTPersistentMessageClient;
import it.cnr.iit.utility.dht.jsonpersistent.*;
import it.cnr.iit.utility.errorhandling.Reject;
import it.cnr.iit.xacml.Attribute;

import java.util.Map;

import static it.cnr.iit.utility.dht.DHTUtils.isDhtReachable;

public abstract class AbstractPIPWebSocket extends PIPBase {


    public String dhtUri;
    public String topicName;
    public String topicUuid;
    public DHTPersistentMessageClient client;
    private RuntimeTypeAdapterFactory<RequestPostTopicUuid> typeFactory;
    private RuntimeTypeAdapterFactory<Persistent> persistentTypeFactory;
    private Class<? extends RequestPostTopicUuid> typeFactoryClazz;
    private Class<? extends Persistent> persistentTypeFactoryClazz;


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

            Reject.ifNull(this.typeFactoryClazz, "Class for TypeFactory not set. " +
                    "Call setClassForTypeFactory() method before init()");
            this.typeFactory = RuntimeTypeAdapterFactory
                    .of(RequestPostTopicUuid.class, "topic_name")
                    .registerSubtype(this.typeFactoryClazz, this.topicName);

            // if the PIP is not intended to upload data to the DHT, persistent
            // messages will not be obtained. Therefore, the PIP is allowed not
            // to use this adapter.
            if (persistentTypeFactoryClazz != null) {
                this.persistentTypeFactory = RuntimeTypeAdapterFactory
                        .of(Persistent.class, "topic_name")
                        .registerSubtype(this.persistentTypeFactoryClazz, this.topicName);
            } else {
                persistentTypeFactory = null;
            }

            client = new DHTPersistentMessageClient(
                    dhtUri, topicName, topicUuid, typeFactory, persistentTypeFactory);

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


    public String performRequestGetTopicUuid() throws PIPException {
        // get the status from the dht
        String response = null;
        if (isDhtReachable(dhtUri, 2000, 1)) {
            RequestGetTopicUuid requestGetTopicUuid =
                    new RequestGetTopicUuid(topicName, topicUuid);

            JsonOutRequestGetTopicUuid jsonOut = new JsonOutRequestGetTopicUuid(requestGetTopicUuid);
            String request = new GsonBuilder()
                    .disableHtmlEscaping()
                    .serializeNulls()
                    .create()
                    .toJson(jsonOut);

            response = getResponse(request);
        } else {
            throw new PIPException("Attribute Manager error: " +
                    "Unable to connect to the DHT");
        }
        return response;
    }


    private String getResponse(String request) throws PIPException {
        String response = client.sendRequestAndWaitForResponse(request);
        client.closeConnection();

        if (response.equals("{\"Response\":{\"value\":{}}}")) {
            int attempts = 5;
            for (int i = 1; i <= attempts; i++) {
                response = client.sendRequestAndWaitForResponse(request);
                client.closeConnection();
                if (!response.equals("{\"Response\":{\"value\":{}}}")) {
                    break;
                }
                if (i == attempts) {
                    throw new PIPException("Attribute Manager error: " +
                            "DHT returned an empty response");
                }
            }
        }
        return response;
    }

    public void setClassForTypeFactory(Class<? extends RequestPostTopicUuid> clazz) {
        this.typeFactoryClazz = clazz;
    }

    public void setClassForPersistentTypeFactory(Class<? extends Persistent> clazz) {
        this.persistentTypeFactoryClazz = clazz;
    }

    public RuntimeTypeAdapterFactory<RequestPostTopicUuid> getTypeFactory() {
        return typeFactory;
    }

    public RuntimeTypeAdapterFactory<Persistent> getPersistentTypeFactory() {
        return persistentTypeFactory;
    }
}
