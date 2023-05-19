package it.cnr.iit.utility.dht.jsondht.reevaluation;

import it.cnr.iit.ucs.constants.PURPOSE;
import it.cnr.iit.utility.dht.jsondht.EvaluatedMessageContent;
import it.cnr.iit.utility.dht.jsondht.MessageContent;

public class ReevaluationResponse implements MessageContent, EvaluatedMessageContent {

    public String purpose = PURPOSE.REEVALUATION_RESPONSE.name();
    private String message_id;
    private String evaluation;

    public ReevaluationResponse() {
    }

    public ReevaluationResponse(String message_id, String evaluation) {
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
