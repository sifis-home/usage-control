package it.cnr.iit.ucsdht.json;

import it.cnr.iit.utility.dht.jsonpersistent.RequestPostTopicUuid;

public class StatusRequestPostTopicUuid implements RequestPostTopicUuid {
    public final String topic_name = "SIFIS:UCS";
    private String topic_uuid = "status";
    private Status value;


    public StatusRequestPostTopicUuid(Status value, String topic_uuid) {
        this.value = value;
        this.topic_uuid = topic_uuid;
    }

    public Status getValue() {
        return value;
    }

    public void setValue(Status value) {
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
