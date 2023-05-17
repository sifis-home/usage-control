package it.cnr.iit.json.endaccess;

public class EndAccessRequest implements it.cnr.iit.json.MessageContent {

    public String type = "end_access_request";
    private String session_id;

    public EndAccessRequest() {
    }

    public EndAccessRequest(String session_id) {
        this.session_id = session_id;
    }

    public String getSession_id() {
        return session_id;
    }

    public void setSession_id(String session_id) {
        this.session_id = session_id;
    }
}