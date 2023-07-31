package it.cnr.iit.utility.dht.jsonpersistent;

public class JsonOutRequestGetTopicUuid {
    private RequestGetTopicUuid RequestGetTopicUUID;

    public JsonOutRequestGetTopicUuid(RequestGetTopicUuid requestGetTopicUuid) {
        this.RequestGetTopicUUID = requestGetTopicUuid;
    }

    public RequestGetTopicUuid getRequestGetTopicUuid() {
        return RequestGetTopicUUID;
    }
}
