package it.cnr.iit.utility.dht.jsonvolatile;

public class InnerValue {

    private MessageContent message;
    private String id;
    private String topic_name;
    private String topic_uuid;

    public InnerValue() {
    }

    public InnerValue(MessageContent message, String id, String topic_name, String topic_uuid) {
        this.message = message;
        this.id = id;
        this.topic_name = topic_name;
        this.topic_uuid = topic_uuid;
    }

    public MessageContent getMessage() {
        return message;
    }

    public void setMessage(MessageContent message) {
        this.message = message;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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
