package it.cnr.iit.utility.dht.jsonvolatile.error;

import it.cnr.iit.ucs.constants.PURPOSE;
import it.cnr.iit.utility.dht.jsonvolatile.MessageContent;

public class ErrorResponse implements MessageContent {

    public String purpose = PURPOSE.ERROR_RESPONSE.name();
    private String message_id;
    private String description;

    public ErrorResponse() {

    }

    public ErrorResponse(String message_id, String description) {
        this.message_id = message_id;
        this.description = description;
    }

    public String getMessage_id() {
        return message_id;
    }

    public void setMessage_id(String message_id) {
        this.message_id = message_id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}