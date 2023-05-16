package it.cnr.iit.json;

import com.google.gson.annotations.SerializedName;

public class InnerValue {

    public String purpose;
    public Message message;
    public String pep_id;
    public String message_id;
    public String topic_name;
    public String topic_uuid;

    public InnerValue() {
    }

    public InnerValue(String purpose, Message message, String pep_id, String message_id, String topic_name, String topic_uuid) {
        this.purpose = purpose;
        this.message = message;
        this.pep_id = pep_id;
        this.message_id = message_id;
        this.topic_name = topic_name;
        this.topic_uuid = topic_uuid;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public Message getMessage() {
        return message;
    }

    public void setMessage(Message message) {
        this.message = message;
    }

    public String getPep_id() {
        return pep_id;
    }

    public void setPep_id(String pep_id) {
        this.pep_id = pep_id;
    }

    public String getMessage_id() {
        return message_id;
    }

    public void setMessage_id(String message_id) {
        this.message_id = message_id;
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
