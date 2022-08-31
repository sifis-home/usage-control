package com.example.ucsintent.properties;


import com.example.ucsintent.UCSApplication;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import ucs.properties.UCSProperties;
import ucs.properties.components.PepProperties;
import ucs.properties.components.PipProperties;
import utility.AndroidFileUtility;

public class UCSAppProperties implements UCSProperties {

    private String CONFIG_ASSETS_PATH = "configs/config_with_sensor.json";
    private JSONObject configJSONWrapper;
    private ObjectMapper objectMapper;
    private UCSAppCoreProperties core;
    private UCSAppContextHandlerProperties contextHandler;
    private UCSAppRequestManagerProperties requestManager;
    private UCSAppSessionManagerProperties sessionManager;
    private UCSAppPapProperties policyAdministrationPoint;
    private UCSAppPdpProperties policyDecisionPoint;
    private UCSAppObligationManagerProperties obligationManager;
    private List<UCSAppPipProperties> pipList;
    private List<UCSAppPepProperties> pepList;

    public UCSAppProperties() throws JSONException, JsonProcessingException {
        String jsonFileContent = AndroidFileUtility.readAssetFileAsString(this.CONFIG_ASSETS_PATH, UCSApplication.getContext());
        configJSONWrapper = new JSONObject(jsonFileContent);
        objectMapper = new ObjectMapper();
        setCore(buildCoreProperties());
        setContextHandler(buildContextHandlerProperties());
        setRequestManager(buildRequestManagerProperties());
        setSessionManager(buildSessionManagerProperties());
        setPolicyDecisionPoint(buildPdpProperties());
        setPolicyAdministrationPoint(buildPapProperties());
        setObligationManager(buildObligationManager());
        setPepList(buildPepList());
        setPipList(buildPipList());
    }

    private UCSAppCoreProperties buildCoreProperties() throws JSONException, JsonProcessingException {
        core = objectMapper.readValue(configJSONWrapper.getString(UCSAppCoreProperties.JSON_OBJECT_NAME),
                    UCSAppCoreProperties.class);
        return core;
    }

    private UCSAppRequestManagerProperties buildRequestManagerProperties() throws JSONException, JsonProcessingException {
        requestManager = objectMapper.readValue(configJSONWrapper.getString(UCSAppRequestManagerProperties.JSON_OBJECT_NAME),
                UCSAppRequestManagerProperties.class);
        return requestManager;
    }

    private UCSAppContextHandlerProperties buildContextHandlerProperties() throws JSONException, JsonProcessingException {
        contextHandler = objectMapper.readValue(configJSONWrapper.getString(UCSAppContextHandlerProperties.JSON_OBJECT_NAME),
                UCSAppContextHandlerProperties.class);
        return contextHandler;
    }

    private UCSAppSessionManagerProperties buildSessionManagerProperties() throws JSONException, JsonProcessingException {
        sessionManager = objectMapper.readValue(configJSONWrapper.getString(UCSAppSessionManagerProperties.JSON_OBJECT_NAME),
                UCSAppSessionManagerProperties.class);
        return sessionManager;
    }

    private UCSAppPdpProperties buildPdpProperties() throws JSONException, JsonProcessingException {
        policyDecisionPoint = objectMapper.readValue(configJSONWrapper.getString(UCSAppPdpProperties.JSON_OBJECT_NAME),
                UCSAppPdpProperties.class);
        return policyDecisionPoint;
    }

    private UCSAppPapProperties buildPapProperties() throws JSONException, JsonProcessingException {
        policyAdministrationPoint = objectMapper.readValue(configJSONWrapper.getString(UCSAppPapProperties.JSON_OBJECT_NAME),
                UCSAppPapProperties.class);
        return policyAdministrationPoint;
    }

    private UCSAppObligationManagerProperties buildObligationManager() throws JSONException, JsonProcessingException {
        obligationManager = objectMapper.readValue(configJSONWrapper.getString(UCSAppObligationManagerProperties.JSON_OBJECT_NAME),
                UCSAppObligationManagerProperties.class);
        return obligationManager;
    }

    private List<UCSAppPepProperties> buildPepList() throws JSONException, JsonProcessingException {
        pepList = new ArrayList<>();
        JSONArray pepListArray = configJSONWrapper.getJSONArray("ucs.pep-list");
        for(int i=0; i<pepListArray.length(); i++){
            JSONObject pepObject = (JSONObject) pepListArray.get(i);
            UCSAppPepProperties tempPepProp = objectMapper.readValue(pepObject.toString(), UCSAppPepProperties.class);
            pepList.add(tempPepProp);
        }
        return pepList;
    }

    private List<UCSAppPipProperties> buildPipList() throws JSONException, JsonProcessingException {
        pipList = new ArrayList<>();
        JSONArray pipListArray = configJSONWrapper.getJSONArray("ucs.pip-list");
        for(int i=0; i<pipListArray.length(); i++){
            JSONObject pipObject = (JSONObject) pipListArray.get(i);
            UCSAppPipProperties tempPipProp = objectMapper.readValue(pipObject.toString(), UCSAppPipProperties.class);
//            System.out.println("tempPIP,  file_path:" + tempPipProp.getAttributes().get(0).get("FILE_PATH") );
            pipList.add(tempPipProp);
        }
        return pipList;
    }
    
    @Override
    public UCSAppContextHandlerProperties getContextHandler() {
        return contextHandler;
    }

    public void setContextHandler( UCSAppContextHandlerProperties contextHandler ) {
        this.contextHandler = contextHandler;
    }

    @Override
    public UCSAppRequestManagerProperties getRequestManager() {
        return requestManager;
    }

    public void setRequestManager( UCSAppRequestManagerProperties requestManager ) {
        this.requestManager = requestManager;
    }

    @Override
    public UCSAppCoreProperties getCore() {
        return core;
    }

    public void setCore( UCSAppCoreProperties core ) {
        this.core = core;
    }

    @Override
    public UCSAppSessionManagerProperties getSessionManager() {
        return sessionManager;
    }

    public void setSessionManager( UCSAppSessionManagerProperties sessionManager ) {
        this.sessionManager = sessionManager;
    }

    @Override
    public UCSAppPapProperties getPolicyAdministrationPoint() {
        return policyAdministrationPoint;
    }

    public void setPolicyAdministrationPoint( UCSAppPapProperties policyAdministrationPoint ) {
        this.policyAdministrationPoint = policyAdministrationPoint;
    }

    @Override
    public UCSAppPdpProperties getPolicyDecisionPoint() {
        return policyDecisionPoint;
    }

    public void setPolicyDecisionPoint( UCSAppPdpProperties policyDecisionPoint ) {
        this.policyDecisionPoint = policyDecisionPoint;
    }

    @Override
    public UCSAppObligationManagerProperties getObligationManager() {
        return obligationManager;
    }

    public void setObligationManager( UCSAppObligationManagerProperties obligationManager ) {
        this.obligationManager = obligationManager;
    }

    @Override
    public List<PipProperties> getPipList() {
        return new ArrayList<>( pipList );
    }

    public void setPipList( List<UCSAppPipProperties> pipList ) {
        this.pipList = pipList;
    }

    @Override
    public List<PepProperties> getPepList() {
        return new ArrayList<>( pepList );
    }

    public void setPepList( List<UCSAppPepProperties> pepList ) {
        this.pepList = pepList;
    }
}
