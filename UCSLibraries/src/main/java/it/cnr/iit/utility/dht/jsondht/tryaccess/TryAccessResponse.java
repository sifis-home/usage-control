package it.cnr.iit.utility.dht.jsondht.tryaccess;

import it.cnr.iit.ucs.constants.PURPOSE;
import it.cnr.iit.utility.dht.jsondht.MessageContent;

public class TryAccessResponse implements MessageContent {

    public String purpose = PURPOSE.TRY_RESPONSE.name();
    private String evaluation;
    private String session_id;

    public TryAccessResponse() {
    }

    public TryAccessResponse(String evaluation, String session_id) {
        this.evaluation = evaluation;
        this.session_id = session_id;
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
