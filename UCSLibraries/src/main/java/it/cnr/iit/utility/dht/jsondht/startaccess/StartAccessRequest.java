package it.cnr.iit.utility.dht.jsondht.startaccess;

import it.cnr.iit.ucs.constants.PURPOSE;
import it.cnr.iit.utility.dht.jsondht.MessageContent;

public class StartAccessRequest implements MessageContent {

    public String purpose = PURPOSE.START.name();
    private String message_id;
    private String session_id;

    public StartAccessRequest() {
    }

    public StartAccessRequest(String message_id, String session_id) {
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
