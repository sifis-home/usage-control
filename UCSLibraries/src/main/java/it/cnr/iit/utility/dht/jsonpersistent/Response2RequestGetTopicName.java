package it.cnr.iit.utility.dht.jsonpersistent;

import java.util.List;

public class Response2RequestGetTopicName {
    private List<RequestPostTopicUuid> value;

    public List<RequestPostTopicUuid> getValue() {
        return value;
    }

    public void setValue(List<RequestPostTopicUuid> value) {
        this.value = value;
    }
}
