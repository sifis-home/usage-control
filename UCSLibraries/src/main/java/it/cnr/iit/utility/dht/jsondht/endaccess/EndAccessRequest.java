package it.cnr.iit.utility.dht.jsondht.endaccess;

import it.cnr.iit.ucs.constants.PURPOSE;
import it.cnr.iit.utility.dht.jsondht.MessageContent;

public class EndAccessRequest implements MessageContent {

    public String purpose = PURPOSE.END.name();
    private String session_id;

    public EndAccessRequest() {
    }

    public EndAccessRequest(String session_id) {
        this.session_id = session_id;
    }

    public String getSession_id() {
        return session_id;
    }

    public void setSession_id(String session_id) {
        this.session_id = session_id;
    }
}