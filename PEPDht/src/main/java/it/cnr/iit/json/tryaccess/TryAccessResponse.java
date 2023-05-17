package it.cnr.iit.json.tryaccess;

public class TryAccessResponse implements it.cnr.iit.json.MessageContent {

    public String type = "try_access_response";
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
