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
package it.cnr.iit.ucs.pip;

import java.util.List;

import it.cnr.iit.ucs.exceptions.PIPException;
import it.cnr.iit.ucs.requestmanager.RequestManagerInterface;
import it.cnr.iit.xacml.Attribute;

import oasis.names.tc.xacml.core.schema.wd_17.RequestType;

/**
 * Solution taken from <a href=
 * "http://stackoverflow.com/questions/15995540/arraylist-containing-different-objects-of-the-same-superclass-how-to-access-me">stackoverflow</a>
 * in order to not have anymore a list of Objects that may cause problems
 * Interface offered by the various PIPs to the context handler. The PIPs offer
 * two different interfaces: one to the obligation manager that is described in
 * the PIPOMInterface, the other is this one.
 *
 * @author antonio
 *
 */
public interface PIPCHInterface extends PIPBaseInterface {

    /**
     * This method uses the XACML request passed as input argument to find out
     * the 'filter' to use to retrieve the value of the attribute with the attributeId
     * equal to the one passed as input argument.
     * Suppose that the PIP handles the 'role' attribute and the 'nationality' attribute.
     * If the 'role' attributeId is passed as input argument, the PIP uses the value
     * of the 'subject-id' contained in the request for querying the Attribute Manager.
     * After the 'role' value has been retrieved, the PIP adds the related attribute
     * in XACML format to the request, and then it starts monitoring the value
     * of such an attribute.
     *
     *
     * @param accessRequest an XACML file containing the request, where the
     *                      subject-id, resource-id, or action-id will be retrieved
     * @throws PIPException
     */
    public void subscribe(RequestType accessRequest, String attributeId) throws PIPException;


    /**
     * This method subscribes all the attributes the PIP is monitoring.
     * For each handled attribute, it uses the XACML request passed as input
     * argument to find out the 'filter' to use in order to retrieve the value
     * of that attribute. After the value has been retrieved, the PIP adds the
     * related attribute in XACML format to the request, and then it starts
     * monitoring the value of such an attribute.
     *
     * @param accessRequest an XACML file containing the request, where the
     *                      subject-id, resource-id, or action-id will be retrieved
     * @throws PIPException
     */
    public void subscribe( RequestType accessRequest ) throws PIPException;

    /**
     * This method retrieves all the attributes the PIP is monitoring.
     * For each handled attribute, it uses the XACML request passed as input
     * argument to find out the 'filter' to use in order to retrieve the value
     * of that attribute. After the value has been retrieved, the PIP adds the
     * related attribute in XACML format to the request.
     *
     * @param accessRequest an XACML file containing the request, where the
     *                      subject-id, resource-id, or action-id will be retrieved
     * @throws PIPException
     */
    public void retrieve( RequestType accessRequest ) throws PIPException;

    /**
     * Unsubscribes one or more attributes for from being monitored by the PIP
     *
     * @param attributes the list of attributes to unsubscribe
     * @throws PIPException
     */
    public boolean unsubscribe( List<Attribute> attributes ) throws PIPException;

    //TODO: fix description. At the moment, this method is only invoked locally
    //      by the PIP itself (and by coverage tests)
//    /**
//     * This retrieve function is called by the ContextHandler when it is queried
//     * by a PIP to retrieve a remote attribute. Hence, the context handler queries
//     * the PIP passing to it the attribute id.
//     *
//     * @param attributeRetrievals
//     *          the attribute the context handler is interested to
//     * @return null if the PIP doesn't take care of this attribute id, otherwise a
//     *         String representing the value of the attribute
//     */
    public String retrieve( Attribute attributeRetrievals ) throws PIPException;


    //TODO: fix description. At the moment, this method is only invoked locally
    //      by the PIP itself (and by coverage tests)
//    /**
//     * This function is added in order to handle the case in which the context
//     * handler needs some attributes hosted in remote UCS to perform the correct
//     * decision. The behavior of this function depends on the type of PIP
//     * implementing it.
//     * <ul>
//     * <li>If it is a normal PIP implementing it then the behavior of this
//     * function will be to store somewhere the address of the asking CH and the
//     * characteristics of the attribute that PIP is interested into</li>
//     * <li>If it is a PIP retrieval, then the behavior of this function will be to
//     * get the address of the contexthandler it needs to retrieve the attribute
//     * and perform a subscription to the PIP that handles that attribute</li>
//     * </ul>
//     *
//     * @param attributeRetrieval
//     *          the attributeRetrieval object that contains all the information
//     *          about the attribute the PIPRetrieval is interested into
//     * @return the value of that attribute as String.
//     * @throws PIPException
//     */
    public String subscribe( Attribute attributeRetrieval ) throws PIPException;


    //TODO: fix description. At the moment, this method neither used
    //      nor implemented properly
//    /**
//     * Function to be used when we need to retrieve more than one attribute from a
//     * contexthandler in a single step. This function behaves exactly like the
//     * other retrieve, fattening the request we pass as parameter
//     *
//     * @param request
//     *          the request the PEP has sent
//     * @param attributeRetrievals
//     *          the list of attributes to be retrieved
//     */
    public void retrieve( RequestType request,
            List<Attribute> attributeRetrievals );


    //TODO: fix description. At the moment, this method neither used
    //      nor implemented properly
//    /**
//     * Function to be used when we need to subscribe to more than one attribute
//     * from a contexthandler in a single step. This function behaves exactly like
//     * the other subscribe, fattening the request we pass as parameter
//     *
//     * @param request
//     *          the request the PEP has sent
//     * @param attributeRetrievals
//     *          the list of attributes to be retrieved
//     */
    public void subscribe( RequestType request,
            List<Attribute> attributeRetrieval );

    /**
     * Sets the request manager to which the PIPs have to communicate to
     *
     * @param requestManager the request manager this PIP communicates with
     */
    public void setRequestManager( RequestManagerInterface requestManager );

    public RequestManagerInterface getRequestManager();

    @Override
    public List<String> getAttributeIds();

    @Override
    public List<Attribute> getAttributes();

}
