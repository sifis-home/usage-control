package it.cnr.iit.utility.dht.jsonpersistent;

public interface Persistent extends RequestPostTopicUuid {

    boolean deleted = false;

     boolean isDeleted();

     void setDeleted(boolean deleted);
}
