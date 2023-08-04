package it.cnr.iit.ucs.pipwebsocket.json;

import it.cnr.iit.utility.dht.jsonpersistent.RequestPostTopicUuid;

public class DomoLightRequestPostTopicUuid implements RequestPostTopicUuid {
    public final String topic_name = "domo_light";
    private String topic_uuid;
    private DomoLight value;


    public DomoLightRequestPostTopicUuid(DomoLight value, String topic_uuid) {
        this.value = value;
        this.topic_uuid = topic_uuid;
    }

    public DomoLight getValue() {
        return value;
    }

    public void setValue(DomoLight value) {
        this.value = value;
    }

    @Override
    public String getTopic_name() {
        return topic_name;
    }

    @Override
    public String getTopic_uuid() {
        return topic_uuid;
    }

    @Override
    public void setTopic_uuid(String topic_uuid) {
        this.topic_uuid = topic_uuid;
    }
}
