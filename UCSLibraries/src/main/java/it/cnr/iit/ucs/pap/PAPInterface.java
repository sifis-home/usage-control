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
package it.cnr.iit.ucs.pap;

import it.cnr.iit.ucs.exceptions.PAPException;

import java.util.List;

/**
 * This is the interface to be used to communicate with the Policy
 * Administration Point.
 * <p>
 * The PAP is a storage of Policy, hence all the methods regarding the PAP are
 * methods to add, retrieve, delete, or list the policy it stores.
 *
 * @author Antonio La Marra
 */
public interface PAPInterface {
    /**
     * Deletes the policy identified by that policy id
     *
     * @param policyId the id of the policy we're interested into
     * @return true if the policy is deleted. False otherwise
     */
    boolean deletePolicy(String policyId);

    /**
     * Retrieves the policy identified by that policy id
     *
     * @param policyId the id of the policy we're interested into
     * @return a String that represents the policy
     */
     String retrievePolicy(String policyId);

    /**
     * Add a new policy to the table
     *
     * @param policy the policy to be added
     * @return String id of the policy added if everything goes fine, null otherwise
     */
     String addPolicy(String policy);


    /**
     * Retrieves the path of policy folder storage
     *
     * @return String path of policy folder storage
     */
     String getPath();

    /**
     * List all the policies stored in the table
     *
     * @return the list of policies in String format
     */
     List<String> listPolicies() throws PAPException;
}
