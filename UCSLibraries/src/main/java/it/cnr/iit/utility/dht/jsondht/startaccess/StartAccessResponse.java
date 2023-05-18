package it.cnr.iit.utility.dht.jsondht.startaccess;

import it.cnr.iit.ucs.constants.PURPOSE;


import it.cnr.iit.utility.dht.jsondht.MessageContent;

public class StartAccessResponse implements MessageContent {

    public String purpose = PURPOSE.START_RESPONSE.name();
    private String evaluation;

    public StartAccessResponse() {
    }

    public StartAccessResponse(String evaluation) {
        this.evaluation = evaluation;
    }

    public String getEvaluation() {
        return evaluation;
    }

    public void setEvaluation(String evaluation) {
        this.evaluation = evaluation;
    }
}
