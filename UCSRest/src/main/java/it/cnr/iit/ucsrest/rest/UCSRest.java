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
package it.cnr.iit.ucsrest.rest;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import it.cnr.iit.ucs.contexthandler.AbstractContextHandler;
import it.cnr.iit.ucs.message.endaccess.EndAccessMessage;
import it.cnr.iit.ucs.message.startaccess.StartAccessMessage;
import it.cnr.iit.ucs.message.tryaccess.TryAccessMessage;
import it.cnr.iit.ucs.obligationmanager.ObligationManagerInterface;
import it.cnr.iit.ucs.pep.PEPInterface;
import it.cnr.iit.ucs.pip.PIPBase;
import it.cnr.iit.ucs.pip.PIPCHInterface;
import it.cnr.iit.ucs.pip.PIPOMInterface;
import it.cnr.iit.ucs.properties.UCSProperties;
import it.cnr.iit.ucs.properties.base.PluginProperties;
import it.cnr.iit.ucs.properties.components.PepProperties;
import it.cnr.iit.ucs.properties.components.PipProperties;
import it.cnr.iit.ucs.requestmanager.AbstractRequestManager;
import it.cnr.iit.ucs.ucs.UCSInterface;
import it.cnr.iit.ucsrest.proxies.ProxyPAP;
import it.cnr.iit.ucsrest.proxies.ProxyPDP;
import it.cnr.iit.ucsrest.proxies.ProxyPEP;
import it.cnr.iit.ucsrest.proxies.ProxySessionManager;
import it.cnr.iit.utility.errorhandling.Reject;
import it.cnr.iit.utility.errorhandling.exception.PreconditionException;

/**
 * This class is in charge of instantiating all the components.
 *
 * @author Antonio La Marra, Alessandro Rosetti
 *
 */
@Component
public class UCSRest implements UCSInterface {

    private static final Logger log = Logger.getLogger( UCSRest.class.getName() );

    private AbstractContextHandler contextHandler;
    private AbstractRequestManager requestManager;
    private ObligationManagerInterface obligationManager;
    private List<PIPBase> pipList = new ArrayList<>();

    private HashMap<String, PEPInterface> proxyPEPMap = new HashMap<>();
    private ProxySessionManager proxySessionManager;
    private ProxyPDP proxyPDP;
    private ProxyPAP proxyPAP;

    private boolean initialised = false;

    @Autowired
    private UCSProperties properties;

    @PostConstruct
    private void init() {
        try {
            buildComponents();
            initialised = true;
        } catch( PreconditionException e ) {
            log.severe( e.getLocalizedMessage() );
            Thread.currentThread().interrupt();
        }
    }

    private boolean buildComponents() {
        log.info( "UsageControlFramework init" );

        Optional<AbstractContextHandler> optCH = buildComponent( properties.getContextHandler() );
        Reject.ifAbsent( optCH, "Error in building the context handler" );
        contextHandler = optCH.get(); // NOSONAR

        Optional<AbstractRequestManager> optRM = buildComponent( properties.getRequestManager() );
        Reject.ifAbsent( optRM, "Error in building the request manager" );
        requestManager = optRM.get(); // NOSONAR
        requestManager.startMonitoring();

        Reject.ifFalse( buildProxySM(), "Error in building the session manager" );
        Reject.ifFalse( buildProxyPDP(), "Error in building the pdp" );
        Reject.ifFalse( buildProxyPolicyAdministrationPoint(), "Error in building the pap" );
        Reject.ifFalse( buildProxyPEPList(), "Error in building the pep" );
        Reject.ifFalse( buildPIPList(), "Error in building the pips" );

        Optional<ObligationManagerInterface> optOM = buildComponent( properties.getObligationManager() );
        Reject.ifAbsent( optOM, "Error in building the request manager" );
        obligationManager = optOM.get(); // NOSONAR
        obligationManager.setPIPs( new ArrayList<PIPOMInterface>( pipList ) );

        log.info( "UsageControlFramework building components completed." );

        return setupComponentsConnections();
    }

    private boolean buildProxySM() {
        proxySessionManager = new ProxySessionManager( properties.getSessionManager() );
        proxySessionManager.start();
        return proxySessionManager.isInitialized();
    }

    private boolean buildProxyPDP() {
        proxyPDP = new ProxyPDP( properties.getPolicyDecisionPoint() );
        return proxyPDP.isInitialized();
    }

    private boolean buildProxyPolicyAdministrationPoint() {
        proxyPAP = new ProxyPAP( properties.getPolicyAdministrationPoint() );
        return proxyPAP.isInitialized();
    }

    private boolean buildProxyPEPList() {
        for( PepProperties pep : properties.getPepList() ) {
            ProxyPEP proxyPEP = new ProxyPEP( pep );
            proxyPEP.setRequestManagerInterface( requestManager );
            if( !proxyPEP.isInitialized() ) {
                return false;
            }
            proxyPEPMap.put( pep.getId(), proxyPEP );
        }
        return true;
    }

    private boolean buildPIPList() {
        int failures = 0;

        for( PipProperties pip : properties.getPipList() ) {
            Optional<PIPBase> optPip = buildComponent( pip );

            if( !optPip.isPresent() ) {
                log.severe( "Error building pip" );
                failures++;
                continue;
            }
            initialised = true;

            PIPBase pipBase = optPip.get();
            pipBase.setContextHandler( contextHandler );
            pipList.add( pipBase );
        }
        return failures == 0;
    }

    private boolean setupComponentsConnections() {
        try {
            contextHandler.setSessionManager( proxySessionManager );
            contextHandler.setRequestManager( requestManager );
            contextHandler.setPap( proxyPAP );
            contextHandler.setPdp( proxyPDP );
            contextHandler.setObligationManager( obligationManager );
            contextHandler.setPIPs( new ArrayList<PIPCHInterface>( pipList ) );
            contextHandler.verify();

            contextHandler.startMonitoringThread();
            requestManager.setInterfaces( contextHandler, proxyPEPMap );
            proxyPDP.setInterfaces( proxyPAP );
        } catch( Exception e ) {
            log.severe( "Error starting context handler : " + e.getMessage() );
            return false;
        }

        return true;
    }

    public static <T> Optional<T> buildComponent( PluginProperties properties ) {
        try {
            // TODO UCS-32 NOSONAR
            Class<?> propClass = properties.getClass().getInterfaces()[0];
            Constructor<?> constructor = Class.forName( properties.getClassName() )
                .getConstructor( propClass );
            T obj = (T) constructor.newInstance( properties );
            return Optional.of( obj );
        } catch( Exception e ) {
            log.severe( "build " + properties.getClassName() + " failed : " + e.getMessage() );
            return Optional.empty();
        }
    }

    @Override
    @Async
    public void tryAccess( TryAccessMessage tryAccessMessage ) {
        // TODO check if sent
        requestManager.sendMessageToCH( tryAccessMessage );
    }

    @Override
    @Async
    public void startAccess( StartAccessMessage startAccessMessage ) {
        // TODO check if sent
        requestManager.sendMessageToCH( startAccessMessage );
    }

    @Override
    @Async
    public void endAccess( EndAccessMessage endAccessMessage ) {
        // TODO check if sent
        requestManager.sendMessageToCH( endAccessMessage );
    }

    public boolean isInitialised() {
        return initialised;
    }

}
