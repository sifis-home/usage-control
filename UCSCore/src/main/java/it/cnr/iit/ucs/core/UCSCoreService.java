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
package it.cnr.iit.ucs.core;

import it.cnr.iit.ucs.contexthandler.AbstractContextHandler;
import it.cnr.iit.ucs.message.endaccess.EndAccessMessage;
import it.cnr.iit.ucs.message.endaccess.EndAccessResponseMessage;
import it.cnr.iit.ucs.message.startaccess.StartAccessMessage;
import it.cnr.iit.ucs.message.startaccess.StartAccessResponseMessage;
import it.cnr.iit.ucs.message.tryaccess.TryAccessMessage;
import it.cnr.iit.ucs.message.tryaccess.TryAccessResponseMessage;
import it.cnr.iit.ucs.obligationmanager.ObligationManagerInterface;
import it.cnr.iit.ucs.pap.PAPInterface;
import it.cnr.iit.ucs.pdp.PDPInterface;
import it.cnr.iit.ucs.pep.PEPInterface;
import it.cnr.iit.ucs.pip.PIPBase;
import it.cnr.iit.ucs.requestmanager.AbstractRequestManager;
import it.cnr.iit.ucs.sessionmanager.SessionManagerInterface;
import it.cnr.iit.ucs.ucs.UCSInterface;
import it.cnr.iit.utility.errorhandling.Reject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class contains all the components
 *
 * @author Antonio La Marra, Alessandro Rosetti
 */
public class UCSCoreService implements UCSInterface {

    AbstractContextHandler contextHandler;
    AbstractRequestManager requestManager;
    ObligationManagerInterface obligationManager;
    SessionManagerInterface sessionManager;
    PDPInterface pdp;
    PAPInterface pap;
    List<PIPBase> pipList;
    HashMap<String, PEPInterface> pepMap;

    @Override
    public TryAccessResponseMessage tryAccess(TryAccessMessage tryAccessMessage) {
        Reject.ifNull(tryAccessMessage);
        TryAccessResponseMessage response = (TryAccessResponseMessage) requestManager.sendMessage(tryAccessMessage);
        // this is null
//		Reject.ifNull(response);
        return response;
    }

    @Override
    public StartAccessResponseMessage startAccess(StartAccessMessage startAccessMessage) {
        return (StartAccessResponseMessage) requestManager.sendMessage(startAccessMessage);
    }

    @Override
    public EndAccessResponseMessage endAccess(EndAccessMessage endAccessMessage) {
        return (EndAccessResponseMessage) requestManager.sendMessage(endAccessMessage);
    }

    public AbstractContextHandler getContextHandler() {
        return contextHandler;
    }

    public AbstractRequestManager getRequestManager() {
        return requestManager;
    }

    public ObligationManagerInterface getObligationManager() {
        return obligationManager;
    }

    public SessionManagerInterface getSessionManager() {
        return sessionManager;
    }

    public PDPInterface getPdp() {
        return pdp;
    }

    public PAPInterface getPap() {
        return pap;
    }

    public List<PIPBase> getPipList() {
        return pipList;
    }

    public Map<String, PEPInterface> getPEPMap() {
        return this.pepMap;
    }
}
