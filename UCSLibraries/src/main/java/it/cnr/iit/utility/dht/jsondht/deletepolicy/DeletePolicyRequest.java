package it.cnr.iit.utility.dht.jsondht.deletepolicy;

import it.cnr.iit.ucs.constants.PURPOSE;
import it.cnr.iit.utility.dht.jsondht.MessageContent;

public class DeletePolicyRequest implements MessageContent {

    public final String purpose = PURPOSE.DELETE_POLICY.name();
    private String message_id;
    private String policy;
    private String policy_id;

    public DeletePolicyRequest() {
    }

    public DeletePolicyRequest(String message_id, String policy, String policy_id) {
        this.message_id = message_id;
        this.policy = policy;
        this.policy_id = policy_id;
    }
    @Override
    public String getMessage_id() {
        return message_id;
    }

    @Override
    public void setMessage_id(String message_id) {
        this.message_id = message_id;
    }

    public String getPolicy() {
        return policy;
    }

    public void setPolicy(String policy) {
        this.policy = policy;
    }

    public String getPolicy_id() {
        return policy_id;
    }

    public void setPolicy_id(String policy_id) {
        this.policy_id = policy_id;
    }
}
