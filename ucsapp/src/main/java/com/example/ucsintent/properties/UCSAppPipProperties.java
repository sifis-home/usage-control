package com.example.ucsintent.properties;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ucs.properties.components.PipProperties;

public class UCSAppPipProperties extends BaseProperties implements PipProperties {
    private String journalPath;
    private String journalProtocol;
    private List<Map<String, String>> attributes;

    public void setJournalPath( String journalPath ) {
        this.journalPath = journalPath;
    }

    @Override
    public boolean isMultiAttribute() {
        return attributes != null && attributes.size() > 1;
    }

    @Override
    public List<Map<String, String>> getAttributes() {
        return attributes;
    }

    public void setAttributes( List<Map<String, String>> attributes ) {
        this.attributes = attributes;
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
