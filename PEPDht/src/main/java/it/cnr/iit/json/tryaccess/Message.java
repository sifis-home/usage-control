package it.cnr.iit.json.tryaccess;

public class Message implements it.cnr.iit.json.Message {

    public String request;
    public String policy;

    public Message() {

    }

    public Message(String request, String policy) {
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
