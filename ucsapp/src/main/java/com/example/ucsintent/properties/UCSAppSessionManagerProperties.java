package com.example.ucsintent.properties;

import com.fasterxml.jackson.annotation.JsonIgnore;

import ucs.properties.components.SessionManagerProperties;

public class UCSAppSessionManagerProperties extends BaseProperties implements SessionManagerProperties {
    @JsonIgnore
    public static final String JSON_OBJECT_NAME = "ucs.session-manager";
    private String dbUri;

    @Override
    public String getDbUri() {
        return dbUri;
    }

    public void setDbUri( String dbUri ) {
        this.dbUri = dbUri;
    }

}
