package it.cnr.iit.json.startaccess;

public class StartAccessResponse implements it.cnr.iit.json.MessageContent {

    public String type = "start_access_response";
    private String evaluation;
    private String session_id;

    public StartAccessResponse() {

    }

    public StartAccessResponse(String evaluation, String session_id) {
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
