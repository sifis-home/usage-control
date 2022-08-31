package com.example.ucsintent.properties;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Map;

import ucs.properties.components.ObligationManagerProperties;

public class UCSAppObligationManagerProperties extends BaseProperties implements ObligationManagerProperties {
    @JsonIgnore
    public static final String JSON_OBJECT_NAME = "ucs.obligation-manager";
}
