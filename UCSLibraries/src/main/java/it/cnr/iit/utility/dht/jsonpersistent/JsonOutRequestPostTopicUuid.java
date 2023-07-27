package it.cnr.iit.utility.dht.jsonpersistent;

public class JsonOutRequestPostTopicUuid {
    private RequestPostTopicUuid RequestPostTopicUUID;

    public JsonOutRequestPostTopicUuid(RequestPostTopicUuid requestPostTopicUUID) {
        RequestPostTopicUUID = requestPostTopicUUID;
    }

    public RequestPostTopicUuid getRequestPostTopicUuid() {
        return RequestPostTopicUUID;
    }
}
