package com.example.ucsintent.properties;

import java.util.Map;

import ucs.properties.base.CommonProperties;

public abstract class BaseProperties implements CommonProperties {
    private String name;
    private Map<String, String> additionalProperties;
    private String id;

    @Override
    public String getName() {
        return name;
    }

    public void setName( String name ) {
        this.name = name;
    }

    @Override
    public Map<String, String> getAdditionalProperties() {
        return additionalProperties;
    }

    public void setAdditionalProperties( Map<String, String> additionalProperties ) {
        this.additionalProperties = additionalProperties;
    }

    @Override
    public String getId() {
        return id;
    }

    public void setId( String id ) {
        this.id = id;
    }
}
