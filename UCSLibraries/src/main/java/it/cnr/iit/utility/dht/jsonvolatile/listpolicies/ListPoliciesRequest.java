package it.cnr.iit.utility.dht.jsonvolatile.listpolicies;

import it.cnr.iit.ucs.constants.PURPOSE;
import it.cnr.iit.utility.dht.jsonvolatile.MessageContent;

public class ListPoliciesRequest implements MessageContent {
    public final String purpose = PURPOSE.LIST_POLICIES.name();
    private String message_id;

    public ListPoliciesRequest() {
    }

    public ListPoliciesRequest(String message_id) {
        this.message_id = message_id;
    }
    @Override
    public String getMessage_id() {
        return message_id;
    }

    @Override
    public void setMessage_id(String message_id) {
        this.message_id = message_id;
    }
}
