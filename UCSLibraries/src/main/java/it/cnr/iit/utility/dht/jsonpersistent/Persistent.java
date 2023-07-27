package it.cnr.iit.utility.dht.jsonpersistent;

public class Persistent {
    private Value value;
    private String topic_name;
    private String topic_uuid;
    private boolean deleted;

    public Value getValue() {
        return value;
    }

    public void setValue(Value value) {
        this.value = value;
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

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

}
