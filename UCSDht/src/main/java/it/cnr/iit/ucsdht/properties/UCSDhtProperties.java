package it.cnr.iit.ucsdht.properties;

import it.cnr.iit.ucs.properties.UCSProperties;
import it.cnr.iit.ucs.properties.components.*;

import java.util.ArrayList;
import java.util.List;

public class UCSDhtProperties implements UCSProperties {

    private List<PipProperties> pipPropertiesList;
    private PapProperties papProperties;

    public UCSDhtProperties(List<PipProperties> pipPropertiesList, PapProperties papProperties) {
        this.pipPropertiesList = pipPropertiesList;
        this.papProperties = papProperties;
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
        return new UCSDhtSessionManagerProperties();
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
        List<PepProperties> res = new ArrayList<>();
        res.add(new UCSDhtPepProperties());
        return res;
    }

}
