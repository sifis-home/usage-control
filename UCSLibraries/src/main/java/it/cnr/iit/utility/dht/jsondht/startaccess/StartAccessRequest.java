package it.cnr.iit.utility.dht.jsondht.startaccess;

import it.cnr.iit.ucs.constants.PURPOSE;
import it.cnr.iit.utility.dht.jsondht.MessageContent;

public class StartAccessRequest implements MessageContent {

    public String purpose = PURPOSE.START.name();
    private String session_id;

    public StartAccessRequest() {
    }

    public StartAccessRequest(String session_id) {
        this.session_id = session_id;
    }

    public String getSession_id() {
        return session_id;
    }

    public void setSession_id(String session_id) {
        this.session_id = session_id;
    }
}
