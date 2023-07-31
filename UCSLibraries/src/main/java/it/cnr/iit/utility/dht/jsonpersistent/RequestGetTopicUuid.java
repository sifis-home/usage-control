package it.cnr.iit.utility.dht.jsonpersistent;

public class RequestGetTopicUuid {
    private String topic_name;
    private String topic_uuid;

    public RequestGetTopicUuid(String topic_name, String topic_uuid) {
        this.topic_name = topic_name;
        this.topic_uuid = topic_uuid;
    }

    public String getTopic_name() {
        return topic_name;
    }

    public void setTopic_name(String topic_name) {
        this.topic_name = topic_name;
    }

    public String getTopic_uuid() {
        return topic_uuid;
    }

    public void setTopic_uuid(String topic_uuid) {
        this.topic_uuid = topic_uuid;
    }

}
