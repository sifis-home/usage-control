package it.cnr.iit.utility.dht.jsonpersistent;

public interface RequestPostTopicUuid {
    String topic_name = null;
    String topic_uuid = null;

    String getTopic_name();

    String getTopic_uuid();
    void setTopic_uuid(String topic_uuid);
}
