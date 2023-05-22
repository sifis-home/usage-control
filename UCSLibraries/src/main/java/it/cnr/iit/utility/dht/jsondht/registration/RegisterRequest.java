package it.cnr.iit.utility.dht.jsondht.registration;

import it.cnr.iit.ucs.constants.PURPOSE;
import it.cnr.iit.utility.dht.jsondht.MessageContent;

public class RegisterRequest implements MessageContent {

    public String purpose = PURPOSE.REGISTER.name();
    private String message_id;
    private String sub_topic_name;
    private String sub_topic_uuid;

    public RegisterRequest() {
    }

    public RegisterRequest(String message_id, String sub_topic_name, String sub_topic_uuid) {
        this.message_id = message_id;
        this.sub_topic_name = sub_topic_name;
        this.sub_topic_uuid = sub_topic_uuid;
    }

    @Override
    public String getMessage_id() {
        return this.message_id;
    }

    @Override
    public void setMessage_id(String message_id) {
        this.message_id = message_id;
    }

    public String getSub_topic_name() {
        return sub_topic_name;
    }

    public void setSub_topic_name(String sub_topic_name) {
        this.sub_topic_name = sub_topic_name;
    }

    public String getSub_topic_uuid() {
        return sub_topic_uuid;
    }

    public void setSub_topic_uuid(String sub_topic_uuid) {
        this.sub_topic_uuid = sub_topic_uuid;
    }
}
