package it.cnr.iit.utility.dht.jsondht.tryaccess;

import it.cnr.iit.ucs.constants.PURPOSE;
import it.cnr.iit.utility.dht.jsondht.MessageContent;

public class TryAccessRequest implements MessageContent {

    public final String purpose = PURPOSE.TRY.name();
    private String message_id;
    private String request;
    private String policy;

    public TryAccessRequest() {
    }

    public TryAccessRequest(String message_id, String request, String policy) {
        this.message_id = message_id;
        this.request = request;
        this.policy = policy;
    }

    public String getMessage_id() {
        return message_id;
    }

    public void setMessage_id(String message_id) {
        this.message_id = message_id;
    }
    public String getRequest() {
        return request;
    }

    public void setRequest(String request) {
        this.request = request;
    }

    public String getPolicy() {
        return policy;
    }

    public void setPolicy(String policy) {
        this.policy = policy;
    }
}
