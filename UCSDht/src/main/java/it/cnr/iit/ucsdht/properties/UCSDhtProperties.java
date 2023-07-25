package it.cnr.iit.ucsdht.properties;

import it.cnr.iit.ucs.properties.UCSProperties;
import it.cnr.iit.ucs.properties.components.*;

import java.util.ArrayList;
import java.util.List;

public class UCSDhtProperties implements UCSProperties {

    private final List<PipProperties> pipPropertiesList;
    private final PapProperties papProperties;
    private final SessionManagerProperties sessionManagerProperties;
    private final List<PepProperties> pepPropertiesList;

    public UCSDhtProperties(List<PipProperties> pipPropertiesList,
                            PapProperties papProperties,
                            SessionManagerProperties sessionManagerProperties,
                            List<PepProperties> pepPropertiesList) {
        this.pipPropertiesList = pipPropertiesList;
        this.papProperties = papProperties;
        this.sessionManagerProperties = sessionManagerProperties;
        this.pepPropertiesList = pepPropertiesList;
    }

    @Override
    public CoreProperties getCore() {
        return new UCSDhtCoreProperties();
    }

    @Override
    public ContextHandlerProperties getContextHandler() {
        return new UCSDhtContextHandlerProperties();
    }

    @Override
    public RequestManagerProperties getRequestManager() {
        return new UCSDhtRequestManagerProperties();
    }

    @Override
    public SessionManagerProperties getSessionManager() {
        return sessionManagerProperties;
    }

    @Override
    public PdpProperties getPolicyDecisionPoint() {
        return new UCSDhtPdpProperties();
    }

    @Override
    public PapProperties getPolicyAdministrationPoint() {
        return this.papProperties;
    }

    @Override
    public ObligationManagerProperties getObligationManager() {
        return new UCSDhtObligationManagerProperties();
    }

    @Override
    public List<PipProperties> getPipList() {
        return this.pipPropertiesList;
    }

    @Override
    public List<PepProperties> getPepList() {
        return this.pepPropertiesList;
    }

}
