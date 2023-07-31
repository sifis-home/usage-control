package it.cnr.iit.utility.dht.jsonvolatile.endaccess;

import it.cnr.iit.ucs.constants.PURPOSE;
import it.cnr.iit.utility.dht.jsonvolatile.EvaluatedMessageContent;
import it.cnr.iit.utility.dht.jsonvolatile.MessageContent;

public class EndAccessResponse implements MessageContent, EvaluatedMessageContent {

    public String purpose = PURPOSE.END_RESPONSE.name();
    private String message_id;
    private String evaluation;

    public EndAccessResponse() {

    }

    public EndAccessResponse(String message_id, String evaluation) {
        this.message_id = message_id;
        this.evaluation = evaluation;
    }

    public String getMessage_id() {
        return message_id;
    }

    public void setMessage_id(String message_id) {
        this.message_id = message_id;
    }

    public String getEvaluation() {
        return evaluation;
    }

    public void setEvaluation(String evaluation) {
        this.evaluation = evaluation;
    }
}