package it.cnr.iit.utility.dht.jsonvolatile.listpolicies;

import it.cnr.iit.ucs.constants.PURPOSE;
import it.cnr.iit.utility.dht.jsonvolatile.MessageContent;

import java.util.List;

public class ListPoliciesResponse implements MessageContent {
    public final String purpose = PURPOSE.LIST_POLICIES_RESPONSE.name();
    private String message_id;
    private List<String> policies;

    public ListPoliciesResponse() {
    }

    public ListPoliciesResponse(String message_id, List<String> policies) {
        this.message_id = message_id;
        this.policies = policies;
    }
    @Override
    public String getMessage_id() {
        return message_id;
    }

    @Override
    public void setMessage_id(String message_id) {
        this.message_id = message_id;
    }

    public List<String> getPolicies() {
        return policies;
    }

    public void setPolicies(List<String> policies) {
        this.policies = policies;
    }

}
