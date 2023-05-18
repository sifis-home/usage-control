package it.cnr.iit.utility.dht.jsondht.tryaccess;

import it.cnr.iit.ucs.constants.PURPOSE;
import it.cnr.iit.utility.dht.jsondht.MessageContent;

public class TryAccessRequest implements MessageContent {

    public final String purpose = PURPOSE.TRY.name();
    private String request;
    private String policy;

    public TryAccessRequest() {
    }

    public TryAccessRequest(String request, String policy) {
        this.request = request;
        this.policy = policy;
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
