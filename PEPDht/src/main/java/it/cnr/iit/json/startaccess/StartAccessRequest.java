package it.cnr.iit.json.startaccess;

public class StartAccessRequest implements it.cnr.iit.json.MessageContent {

    public String type = "start_access_request";
    private String session_id;

    public StartAccessRequest() {
    }

    public StartAccessRequest(String session_id) {
        this.session_id = session_id;
    }

    public String getSession_id() {
        return session_id;
    }

    public void setSession_id(String session_id) {
        this.session_id = session_id;
    }
}
