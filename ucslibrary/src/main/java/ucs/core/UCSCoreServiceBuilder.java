/*******************************************************************************
 * Copyright 2018 IIT-CNR
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package ucs.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;
import java.util.logging.Logger;

import ucs.contexthandler.AbstractContextHandler;
import ucs.obligationmanager.ObligationManagerInterface;
import ucs.pap.PAPInterface;
import ucs.pdp.PDPInterface;
import ucs.pep.PEPInterface;
import ucs.pip.PIPBase;
import ucs.pip.PIPCHInterface;
import ucs.pip.PIPOMInterface;
import ucs.properties.UCSProperties;
import ucs.properties.base.CommonProperties;
import ucs.properties.components.PepProperties;
import ucs.properties.components.PipProperties;
import ucs.requestmanager.AbstractRequestManager;
import ucs.sessionmanager.SessionManagerInterface;
import utility.ReflectionsUtility;
import utility.errorhandling.Reject;
import utility.errorhandling.exception.PreconditionException;

/**
 * This class is in charge of instantiating all the components.
 *
 * @author Antonio La Marra, Alessandro Rosetti
 */
public class UCSCoreServiceBuilder {

    private static final Logger log = Logger.getLogger( UCSCoreServiceBuilder.class.getName() );

    private UCSCoreService ucsCore;
    private UCSProperties properties;

    public UCSCoreServiceBuilder() {
        ucsCore = new UCSCoreService();
        ucsCore.pipList = new ArrayList<>();
        ucsCore.pepMap = new HashMap<>();
    }

    public UCSCoreService build() {
        try {
            log.info( "[INIT] usage control initialisation ..." );
            buildComponents();
            setupConnections();
            log.info( "[DONE] building components completed" );
        } catch( PreconditionException e ) {
            log.severe( "[ERROR] " + e.getMessage() );
            Thread.currentThread().interrupt();
        }
        return ucsCore;
    }

    public UCSCoreServiceBuilder setProperties(UCSProperties properties ) {
        this.properties = properties;
        return this;
    }

    private UCSCoreServiceBuilder buildComponents() {
        buildContextHandler();
        buildRequestManager();
        buildSessionManager();
        buildPolicyAdministrationPoint();
        buildPEPList();
        buildPIPList();
        buildObligationManager();
        buildPolicyDecisionPoint();
        return this;
    }

    private UCSCoreServiceBuilder setupConnections() {
        ucsCore.contextHandler.setSessionManager( ucsCore.sessionManager );
        ucsCore.contextHandler.setRequestManager( ucsCore.requestManager );
        ucsCore.contextHandler.setPap( ucsCore.pap );
        ucsCore.contextHandler.setPdp( ucsCore.pdp );
        ucsCore.contextHandler.setObligationManager( ucsCore.obligationManager );
        ucsCore.contextHandler.setPIPs( new ArrayList<PIPCHInterface>( ucsCore.pipList ) );
        ucsCore.requestManager.setContextHandler( ucsCore.contextHandler );
        if(ucsCore.requestManager.getContextHandler() == null) {
            log.info("SETUP CONNECTION ERRRO Context handler in request manager is null");
        }

        ucsCore.requestManager.setPEPMap( ucsCore.pepMap );
        for( PIPBase pip : ucsCore.pipList ) {
            pip.setRequestManager( ucsCore.requestManager );
        }
        ucsCore.pdp.setPap( ucsCore.pap );
        ucsCore.pdp.setObligationManager( ucsCore.obligationManager );
        ucsCore.obligationManager.setPIPs( new ArrayList<PIPOMInterface>( ucsCore.pipList ) );
        return this;
    }

    private void buildContextHandler() {
        ucsCore.contextHandler = buildComponent( properties.getContextHandler(), AbstractContextHandler.class ).get(); // NOSONAR
    }

    private void buildRequestManager() {
        ucsCore.requestManager = buildComponent( properties.getRequestManager(), AbstractRequestManager.class ).get(); // NOSONAR
        ucsCore.requestManager.startMonitoring();
    }

    private void buildSessionManager() {
        ucsCore.sessionManager = buildComponent( properties.getSessionManager(), SessionManagerInterface.class ).get(); // NOSONAR
        ucsCore.sessionManager.start();
    }

    private void buildPolicyDecisionPoint() {
        ucsCore.pdp = buildComponent( properties.getPolicyDecisionPoint(), PDPInterface.class ).get(); // NOSONAR
    }

    private void buildPolicyAdministrationPoint() {
        ucsCore.pap = buildComponent( properties.getPolicyAdministrationPoint(), PAPInterface.class ).get(); // NOSONAR
    }

    private void buildObligationManager() {
        ucsCore.obligationManager = buildComponent( properties.getObligationManager(), ObligationManagerInterface.class ).get(); // NOSONAR
    }

    private void buildPEPList() {
        for( PepProperties pepProp : properties.getPepList() ) {
            Optional<PEPInterface> pep = buildComponent( pepProp, PEPInterface.class ); // NOSONAR
            ucsCore.pepMap.put( pepProp.getUri(), pep.get() ); // NOSONAR
        }
    }

    private void buildPIPList() {
        for( PipProperties pipProp : properties.getPipList() ) {
            Optional<PIPBase> pip = buildComponent( pipProp, PIPBase.class );
            ucsCore.pipList.add( pip.get() ); // NOSONAR
        }
    }

    private <T> Optional<T> buildComponent( CommonProperties property, Class<T> clazz ) {
        log.info( "[BUILD] " + property.getName() );
        Optional<T> component = ReflectionsUtility.buildComponent( property, clazz );
        Reject.ifAbsent( component, "Error building " + property.getName() );
        return component;
    }

}
