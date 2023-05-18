package it.cnr.iit.utility.dht.jsondht.endaccess;

import it.cnr.iit.ucs.constants.PURPOSE;
import it.cnr.iit.utility.dht.jsondht.MessageContent;

public class EndAccessResponse implements MessageContent {

    public String purpose = PURPOSE.END_RESPONSE.name();
    private String evaluation;

    public EndAccessResponse() {

    }

    public EndAccessResponse(String evaluation) {
        this.evaluation = evaluation;
    }

    public String getEvaluation() {
        return evaluation;
    }

    public void setEvaluation(String evaluation) {
        this.evaluation = evaluation;
    }
}