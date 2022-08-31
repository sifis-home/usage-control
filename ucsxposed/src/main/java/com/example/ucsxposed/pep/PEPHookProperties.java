package com.example.ucsxposed.pep;

public class PEPHookProperties {
    private String id;
    private String uri;
    private String revokeType;
    private String policyPath;
    private String requestPath;
    private String apiStatusChanged;

    //Default properties
    public PEPHookProperties(){
        this.setId("1");
        this.setUri("http://localhost:9999");
        this.setRevokeType("HARD");
        this.setPolicyPath("xmls/policy-watch.xml");
        this.setRequestPath("xmls/request.xml");
        this.setApiStatusChanged("onGoingEvaluation");
    }

    public PEPHookProperties(String id, String policyPath, String requestPath){
        this.setId(id);
        this.setUri("http://localhost:9999");
        this.setRevokeType("HARD");
        this.setPolicyPath(policyPath);
        this.setRequestPath(requestPath);
        this.setApiStatusChanged("onGoingEvaluation");
    }

    public String getId() {
        return id;
    }

    public void setId( String id ) {
        this.id = id;
    }

    public String getUri() {
        return uri;
    }

    public void setUri( String uri ) {
        this.uri = uri;
    }

    public String getRevokeType() {
        return revokeType;
    }

    public void setRevokeType( String revokeType ) {
        this.revokeType = revokeType;
    }

    public String getPolicyPath() {
        return policyPath;
    }

    public void setPolicyPath( String policyPath ) {
        this.policyPath = policyPath;
    }

    public String getRequestPath() {
        return requestPath;
    }

    public void setRequestPath( String requestPath ) {
        this.requestPath = requestPath;
    }

    public String getApiStatusChanged() {
        return apiStatusChanged;
    }

    public void setApiStatusChanged( String apiStatusChanged ) {
        this.apiStatusChanged = apiStatusChanged;
    }
}
