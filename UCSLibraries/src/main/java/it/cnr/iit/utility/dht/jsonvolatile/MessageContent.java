package it.cnr.iit.utility.dht.jsonvolatile;

public interface MessageContent {

    String purpose = null;
    String message_id = null;
    String getMessage_id();
    void setMessage_id(String message_id);
}
