package it.cnr.iit.utility.dht.jsonvolatile.tryaccess;

import it.cnr.iit.ucs.constants.PURPOSE;
import it.cnr.iit.utility.dht.jsonvolatile.EvaluatedMessageContent;
import it.cnr.iit.utility.dht.jsonvolatile.MessageContent;

public class TryAccessResponse implements MessageContent, EvaluatedMessageContent {

    public String purpose = PURPOSE.TRY_RESPONSE.name();
    private String message_id;
    private String evaluation;
    private String session_id;

    public TryAccessResponse() {
    }

    public TryAccessResponse(String message_id, String evaluation, String session_id) {
        this.message_id = message_id;
        this.evaluation = evaluation;
        this.session_id = session_id;
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

    public String getSession_id() {
        return session_id;
    }

    public void setSession_id(String policy) {
        this.session_id = session_id;
    }
}
