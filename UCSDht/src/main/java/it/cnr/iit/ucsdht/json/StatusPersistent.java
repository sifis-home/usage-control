package it.cnr.iit.ucsdht.json;

import it.cnr.iit.utility.dht.jsonpersistent.Persistent;

public class StatusPersistent extends StatusRequestPostTopicUuid implements Persistent {

    private boolean deleted;

    public StatusPersistent(Status value, String topic_uuid) {
        super(value, topic_uuid);
    }

    @Override
    public boolean isDeleted() {
        return deleted;
    }
    @Override
    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }
}
