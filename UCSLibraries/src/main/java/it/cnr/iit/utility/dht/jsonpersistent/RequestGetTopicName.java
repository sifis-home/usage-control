package it.cnr.iit.utility.dht.jsonpersistent;

public class RequestGetTopicName {
    private String topic_name;

    public RequestGetTopicName(String topic_name) {
        this.topic_name = topic_name;
    }

    public String getTopic_name() {
        return topic_name;
    }

    public void setTopic_name(String topic_name) {
        this.topic_name = topic_name;
    }

}
