package com.example.ucsintent.properties;

import ucs.properties.components.PepProperties;

public class UCSAppPepProperties extends BaseProperties implements PepProperties {
    private String uri;
    private String revokeType;
    private String apiOngoingEvaluation;
    private String apiTryAccessResponse;
    private String apiStartAccessResponse;
    private String apiEndAccessResponse;
    private String policyPath;
    private String requestPath;
    private String apiStatusChanged;

    @Override
    public String getUri() {
        return uri;
    }

    public void setUri( String uri ) {
        this.uri = uri;
    }

    @Override
    public String getRevokeType() {
        return revokeType;
    }

    public void setRevokeType( String revokeType ) {
        this.revokeType = revokeType;
    }

    @Override
    public String getApiOngoingEvaluation() {
        return apiOngoingEvaluation;
    }

    public void setApiOngoingEvaluation( String apiOngoingEvaluation ) {
        this.apiOngoingEvaluation = apiOngoingEvaluation;
    }

    @Override
    public String getApiTryAccessResponse() {
        return apiTryAccessResponse;
    }

    public void setApiTryAccessResponse( String apiTryAccessResponse ) {
        this.apiTryAccessResponse = apiTryAccessResponse;
    }

    @Override
    public String getApiStartAccessResponse() {
        return apiStartAccessResponse;
    }

    public void setApiStartAccessResponse( String apiStartAccessResponse ) {
        this.apiStartAccessResponse = apiStartAccessResponse;
    }

    @Override
    public String getApiEndAccessResponse() {
        return apiEndAccessResponse;
    }

    public void setApiEndAccessResponse( String apiEndAccessResponse ) {
        this.apiEndAccessResponse = apiEndAccessResponse;
    }

    public String getApiStatusChanged() {
        return apiStatusChanged;
    }

    public void setApiStatusChanged(String apiStatusChanged) {
        this.apiStatusChanged = apiStatusChanged;
    }

    public String getPolicyPath() {
        return policyPath;
    }

    public void setPolicyPath(String policyPath) {
        this.policyPath = policyPath;
    }

    public String getRequestPath() {
        return requestPath;
    }

    public void setRequestPath(String requestPath) {
        this.requestPath = requestPath;
    }
}
