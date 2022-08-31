package com.example.ucsintent.properties;

import com.fasterxml.jackson.annotation.JsonIgnore;

import ucs.properties.components.RequestManagerProperties;

public class UCSAppRequestManagerProperties extends BaseProperties implements RequestManagerProperties {
    @JsonIgnore
    public static final String JSON_OBJECT_NAME = "ucs.request-manager";
    private String apiRemoteResponse;
    private boolean active;

    @Override
    public String getApiRemoteResponse() {
        return apiRemoteResponse;
    }

    public void setApiRemoteResponse( String apiRemoteResponse ) {
        this.apiRemoteResponse = apiRemoteResponse;
    }

    public void setActive( boolean active ) {
        this.active = active;
    }

    @Override
    public boolean isActive() {
        return active;
    }
}
