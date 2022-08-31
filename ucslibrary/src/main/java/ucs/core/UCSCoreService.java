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

import java.util.HashMap;
import java.util.List;

import ucs.contexthandler.AbstractContextHandler;
import ucs.message.endaccess.EndAccessMessage;
import ucs.message.startaccess.StartAccessMessage;
import ucs.message.tryaccess.TryAccessMessage;
import ucs.obligationmanager.ObligationManagerInterface;
import ucs.pap.PAPInterface;
import ucs.pdp.PDPInterface;
import ucs.pep.PEPInterface;
import ucs.pip.PIPBase;
import ucs.sessionmanager.SessionManagerInterface;
import ucs.requestmanager.AbstractRequestManager;
import ucs.ucs.UCSInterface;

/**
 * This class contains all the components
 *
 * @author Antonio La Marra, Alessandro Rosetti
 */
public class UCSCoreService implements UCSInterface {

    public AbstractContextHandler contextHandler;
    public AbstractRequestManager requestManager;
    ObligationManagerInterface obligationManager;
    SessionManagerInterface sessionManager;
    PDPInterface pdp;
    PAPInterface pap;
    List<PIPBase> pipList;
    HashMap<String, PEPInterface> pepMap;

    @Override
    public Boolean tryAccess( TryAccessMessage tryAccessMessage ) {
        return requestManager.sendMessage( tryAccessMessage );
    }

    @Override
    public Boolean startAccess( StartAccessMessage startAccessMessage ) {
        return requestManager.sendMessage( startAccessMessage );
    }

    @Override
    public Boolean endAccess( EndAccessMessage endAccessMessage ) {
        return requestManager.sendMessage( endAccessMessage );
    }

}
