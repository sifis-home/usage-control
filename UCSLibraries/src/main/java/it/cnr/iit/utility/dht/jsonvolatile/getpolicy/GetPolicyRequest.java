package it.cnr.iit.utility.dht.jsonvolatile.getpolicy;

import it.cnr.iit.ucs.constants.PURPOSE;
import it.cnr.iit.utility.dht.jsonvolatile.MessageContent;

public class GetPolicyRequest implements MessageContent {

    public String purpose = PURPOSE.GET_POLICY.name();
    private String message_id;
    private String policy_id;

    public GetPolicyRequest() {
    }

    public GetPolicyRequest(String message_id, String policy_id) {
        this.message_id = message_id;
        this.policy_id = policy_id;
    }

    public String getMessage_id() {
        return message_id;
    }

    public void setMessage_id(String message_id) {
        this.message_id = message_id;
    }

    public String getPolicy_id() {
        return policy_id;
    }

    public void setPolicy(String policy_id) {
        this.policy_id = policy_id;
    }
}