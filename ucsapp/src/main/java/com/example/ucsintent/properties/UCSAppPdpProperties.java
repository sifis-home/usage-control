package com.example.ucsintent.properties;
import java.util.HashMap;
import java.util.Map;

import ucs.properties.components.PdpProperties;

public class UCSAppPdpProperties extends BaseProperties implements PdpProperties {
    public static final String JSON_OBJECT_NAME = "ucs.policy-decision-point";
    private String journalPath;
    private String journalProtocol;

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
