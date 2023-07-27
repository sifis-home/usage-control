package it.cnr.iit.utility.dht.jsonvolatile.endaccess;

import it.cnr.iit.ucs.constants.PURPOSE;
import it.cnr.iit.utility.dht.jsonvolatile.MessageContent;

public class EndAccessRequest implements MessageContent {

    public String purpose = PURPOSE.END.name();
    private String message_id;
    private String session_id;

    public EndAccessRequest() {
    }

    public EndAccessRequest(String message_id, String session_id) {
        this.message_id = message_id;
        this.session_id = session_id;
    }

    public String getMessage_id() {
        return message_id;
    }

    public void setMessage_id(String message_id) {
        this.message_id = message_id;
    }

    public String getSession_id() {
        return session_id;
    }

    public void setSession_id(String session_id) {
        this.session_id = session_id;
    }
}