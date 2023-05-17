package it.cnr.iit.json.tryaccess;

public class TryAccessRequest implements it.cnr.iit.json.MessageContent {

    public final String type = "try_access_request";
    private String request;
    private String policy;

    public TryAccessRequest() {
    }

    public TryAccessRequest(String request, String policy) {
        this.request = request;
        this.policy = policy;
    }

    public String getRequest() {
        return request;
    }

    public void setRequest(String request) {
        this.request = request;
    }

    public String getPolicy() {
        return policy;
    }

    public void setPolicy(String policy) {
        this.policy = policy;
    }
}
