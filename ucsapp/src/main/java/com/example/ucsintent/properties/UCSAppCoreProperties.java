package com.example.ucsintent.properties;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonRootName;

import java.util.HashMap;
import java.util.Map;

import ucs.properties.components.CoreProperties;

public class UCSAppCoreProperties extends BaseProperties implements CoreProperties {
    @JsonIgnore
    public static String JSON_OBJECT_NAME = "ucs.core";

    private String uri;
    private String journalPath;
    private String journalProtocol;

    @Override
    public String getUri() {
        return uri;
    }

    public void setUri( String uri ) {
        this.uri = uri;
    }

    public void setJournalPath( String journalPath ) {
        this.journalPath = journalPath;
    }

    @Override
    public String getJournalPath() {
        return journalPath;
    }

    @Override
    public String getJournalProtocol() {
        return journalProtocol;
    }

    public void setJournalProtocol( String journalProtocol ) {
        this.journalProtocol = journalProtocol;
    }

    @Override
    public Map<String, String> getJournalAdditionalProperties() {
        return new HashMap<>();
    }
}
