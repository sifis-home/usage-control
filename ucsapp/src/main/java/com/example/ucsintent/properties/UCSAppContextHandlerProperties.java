package com.example.ucsintent.properties;


import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Map;

import ucs.properties.components.ContextHandlerProperties;

public class UCSAppContextHandlerProperties extends BaseProperties implements ContextHandlerProperties {
    @JsonIgnore
    public static final String JSON_OBJECT_NAME = "ucs.context-handler";
    private String uri;

    @Override
    public String getUri() {
        return uri;
    }

    public void setUri( String uri ) {
        this.uri = uri;
    }

    @Override
    public Map<String, String> getAdditionalProperties() {
        return null;
    }
}
