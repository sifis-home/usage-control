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
package ucs.message.tryaccess;

import java.io.Serializable;

import ucs.constants.ENTITIES;
import ucs.constants.PURPOSE;
import ucs.exceptions.PolicyException;
import ucs.exceptions.RequestException;
import utility.errorhandling.Reject;
import xacml.wrappers.PolicyWrapper;
import xacml.wrappers.RequestWrapper;
import ucs.message.Message;

/**
 * This is a tryAccess message.
 *
 * @author Antonio La Marra, Alessandro Rosetti
 */
public final class TryAccessMessage extends Message implements Serializable {

    private String pepUri;
    private String policyId;
    private String policy;
    private String request;

    public TryAccessMessage( String source, String destination ) {
        super( source, destination );
        purpose = PURPOSE.TRY;
    }

    public TryAccessMessage() {
        super( ENTITIES.PEP.toString(), ENTITIES.CH.toString() );
        purpose = PURPOSE.TRY;
    }

    public void setPepUri( String pepUri ) {
        Reject.ifBlank( pepUri );
        this.pepUri = pepUri;
    }

    public String getPepUri() {
        return pepUri;
    }

    public void setRequest( String request ) {
        Reject.ifBlank( request );
        try {
            RequestWrapper requestWrapper = RequestWrapper.build( request ); // NOSONAR
        } catch (RequestException e) {
            e.printStackTrace();
        }
        this.request = request;
    }

    public String getRequest() {
        return request;
    }

    public void setPolicy( String policy ) {
    	if (!policy.isEmpty()) {
	    	try {
	            PolicyWrapper policyWrapper = PolicyWrapper.build( policy );
	        } catch( PolicyException e ) {
	            throw new IllegalStateException( "invalid policy" ); // NOSONAR
	        }
    	}
        this.policy = policy;
    }

    public String getPolicy() {
        return policy;
    }

    public void setPolicyId( String policyId ) {
        this.policyId = policyId;
    }

    public String getPolicyId() {
        return policyId;
    }
}
