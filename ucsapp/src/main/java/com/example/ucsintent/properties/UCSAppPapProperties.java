package com.example.ucsintent.properties;

import com.fasterxml.jackson.annotation.JsonIgnore;

import ucs.properties.components.PapProperties;

public class UCSAppPapProperties extends BaseProperties implements PapProperties {
    @JsonIgnore
    public static final String JSON_OBJECT_NAME = "ucs.policy-administration-point";
    private String path;

    @Override
    public String getPath() {
        return path;
    }

    public void setPath( String path ) {
        this.path = path;
    }
}
