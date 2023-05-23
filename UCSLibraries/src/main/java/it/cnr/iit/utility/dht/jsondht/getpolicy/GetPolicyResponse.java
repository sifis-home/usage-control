package it.cnr.iit.utility.dht.jsondht.getpolicy;

import it.cnr.iit.ucs.constants.PURPOSE;
import it.cnr.iit.utility.dht.jsondht.MessageContent;

public class GetPolicyResponse implements MessageContent {

    public String purpose = PURPOSE.GET_POLICY_RESPONSE.name();
    private String message_id;
    private String policy;

    public GetPolicyResponse() {
    }

    public GetPolicyResponse(String message_id, String policy) {
        this.message_id = message_id;
        this.policy = policy;
    }

    public String getMessage_id() {
        return message_id;
    }

    public void setMessage_id(String message_id) {
        this.message_id = message_id;
    }

    public String getPolicy() {
        return policy;
    }

    public void setPolicy(String policy) {
        this.policy = policy;
    }
}