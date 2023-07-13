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
package it.cnr.iit.ucs.pipreader;

import it.cnr.iit.ucs.constants.ENTITIES;
import it.cnr.iit.ucs.exceptions.PIPException;
import it.cnr.iit.ucs.journaling.JournalBuilder;
import it.cnr.iit.ucs.journaling.JournalingInterface;
import it.cnr.iit.ucs.message.attributechange.AttributeChangeMessage;
import it.cnr.iit.ucs.obligationmanager.ObligationInterface;
import it.cnr.iit.ucs.pip.PIPBase;
import it.cnr.iit.ucs.pip.PIPKeywords;
import it.cnr.iit.ucs.properties.components.PipProperties;
import it.cnr.iit.utility.FileUtility;
import it.cnr.iit.utility.errorhandling.Reject;
import it.cnr.iit.xacml.Attribute;
import it.cnr.iit.xacml.Category;
import it.cnr.iit.xacml.DataType;
import oasis.names.tc.xacml.core.schema.wd_17.RequestType;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The task this PIP is to read data from a file when requested.
 * The Path to reach the file is passed as parameter to the pip.
 *
 * @author Antonio La Marra, Alessandro Rosetti
 */
public final class PIPReader extends PIPBase {

    private static final Logger log = Logger.getLogger(PIPReader.class.getName());
    private JournalingInterface journal;

    // list that stores the attributes on which a 'subscribe' has been performed
    private final BlockingQueue<Attribute> subscriptions = new LinkedBlockingQueue<>();

    /**
     * Whenever a PIP has to retrieve some information related to an attribute
     * that is stored inside the request, it has to know in advance all the
     * information to retrieve that attribute. E.g. if this PIP has to retrieve
     * information about the subject, it has to know in advance which is the
     * attribute id qualifying the subject, its category and the data-type used,
     * otherwise it is not able to retrieve the value of that attribute, hence it
     * would not be able to communicate with the AM properly
     */
    /**
     * Map having attributeId as key and the expected category as value
     */
    private final Map<String, Category> expectedCategoryMap = new HashMap<>();

    public static final String FILE_PATH_KEYWORD = "FILE_PATH";

    /**
     * Map having attributeId as key and the file path as value
     */
    private final Map<String, String> filePathMap = new HashMap<>();

    public PIPReader(PipProperties properties) {
        super(properties);
        Reject.ifFalse(init(properties), "Error initialising pip : " + properties.getId());
    }

    private boolean init(PipProperties properties) {
        try {
            for (Map<String, String> attributeMap : properties.getAttributes()) {

                Attribute attribute = new Attribute();

                String attributeId = attributeMap.get(PIPKeywords.ATTRIBUTE_ID);
                attribute.setAttributeId(attributeId);

                Category category = Category.toCATEGORY(attributeMap.get(PIPKeywords.CATEGORY));
                attribute.setCategory(category);

                DataType dataType = DataType.toDATATYPE(attributeMap.get(PIPKeywords.DATA_TYPE));
                attribute.setDataType(dataType);

                if (attribute.getCategory() != Category.ENVIRONMENT) {
                    Category expectedCategory = Category.toCATEGORY(attributeMap.get(PIPKeywords.EXPECTED_CATEGORY));
                    Reject.ifNull(expectedCategory, "missing expected category");
                    expectedCategoryMap.put(attribute.getAttributeId(), expectedCategory);
                }

                Reject.ifTrue(!attributeMap.containsKey(FILE_PATH_KEYWORD) || attributeMap.get(FILE_PATH_KEYWORD).isEmpty(), "missing file path");
                setFilePath(attribute.getAttributeId(), attributeMap.get(FILE_PATH_KEYWORD));
                addAttribute(attribute);
                journal = JournalBuilder.build(properties);

                PIPReaderSubscriberTimer subscriberTimer = new PIPReaderSubscriberTimer(this);
                subscriberTimer.setRate(properties.getRefreshRate());
                subscriberTimer.start();
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Performs the retrieve operation.
     * The retrieve operation is a very basic operation in which the PIP simply
     * asks the AttributeManager the value in which it is interested into. Once
     * that value has been retrieved, the PIP will fatten the request.
     *
     * @param request this is an in/out parameter
     */
    @Override
    public void retrieve(RequestType request) throws PIPException {
        Reject.ifNull(request);

        for (Attribute attribute : getAttributes()) {
            if (!isEnvironmentCategory(attribute)) {
                addAdditionalInformation(request, attribute);
            }
            try {
                String value = retrieve(attribute);
                request.addAttribute(attribute, value);
            } catch (Exception e) {
                //TODO: handle exception
                System.err.println(e.getMessage());
            }
        }
    }

    /**
     * Retrieve the value of the attribute passed as argument.
     * If the value is not of type environment, this method uses the additionalInformation
     * (which has to be set beforehand) to select the right value from the file.
     * Then, it sets the value within the attribute, and finally returns a String
     * containing such a value.
     */
    @Override
    public String retrieve(Attribute attribute) throws PIPException {

        String filePath = this.filePathMap.get(attribute.getAttributeId());
        String value;
        if (isEnvironmentCategory(attribute)) {
            value = read(filePath);
        } else {
            value = read(filePath, attribute.getAdditionalInformations());
        }
        attribute.setValue(attribute.getDataType(), value);
        return value;
    }


    /**
     * Performs the subscribe operation. The request passed as input is used
     * to retrieve the information related to either the subject, resource, or action.
     * The attributeId is used to select the right attribute from those this PIP
     * is monitoring.
     *
     * @param request     the XACML request containing at least subject-id, resource-id and action-id
     * @param attributeId the attributeId of the attribute we want this PIP to subscribe to.
     */
    @Override
    public void subscribe(RequestType request, String attributeId) {
        Reject.ifNull(request);

        Attribute attribute = getAttributes().stream()
                .filter(attr -> attr.getAttributeId().equals(attributeId)).findAny().orElse(null);
        Reject.ifNull(attribute);
        if (!isEnvironmentCategory(attribute)) {
            addAdditionalInformation(request, attribute);
        }
        try {
            String value = subscribe(attribute);
            request.addAttribute(attribute, value);
        } catch (Exception e) {
            //TODO: handle exception
            System.err.println(e.getMessage());
        }
    }


    /**
     * Performs the subscribe operation. This operation is very similar to the
     * retrieve operation. The only difference is that in this case we have to
     * signal to the thread in charge of performing the polling that it has to
     * poll a new attribute
     *
     * @param request IN/OUT parameter
     */
    @Override
    public void subscribe(RequestType request) throws PIPException {
        Reject.ifNull(request);

        for (Attribute attribute : getAttributes()) {
            if (!isEnvironmentCategory(attribute)) {
                addAdditionalInformation(request, attribute);
            }
            try {
                String value = subscribe(attribute);
                request.addAttribute(attribute, value);
            } catch (Exception e) {
                //TODO: handle exception
                System.err.println(e.getMessage());
            }
        }
    }

    /**
     * This is the function called by the context handler whenever we have a
     * remote retrieve request
     */
    @Override
    public String subscribe(Attribute attribute) throws PIPException {
        Reject.ifNull(attribute);

        String value = retrieve(attribute);
        addSubscription(attribute);

        return value;
    }

    /**
     * Given a list of attributes as input, remove from the subscriptions list the attributes
     * that match.
     * For the environment attributes, only the attributeId must match in order for an attribute
     * to be removed from the subscriptions list.
     * For attributes of other categories, both the attributeId and the additionalInformation must
     * match in order for an attribute to be removed from the subscriptions list.
     * This is because, for example, the PIP could be monitoring the attributeId 'subject-role'
     * for the subject with subject-id 'User1' and for the subject with subject-id 'User2'.
     * The information 'User1' or 'User2' is contained within the additionalInformation field.
     *
     * @param unsubAttributes the list of attributes that must be unsubscribed
     */
    @Override
    public boolean unsubscribe(List<Attribute> unsubAttributes) throws PIPException {
        Reject.ifEmpty(unsubAttributes);

        boolean atLeastOneAttributeRemoved = false;

        // for each attribute to unsubscribe
        for (Attribute attributeToUnsubscribe : unsubAttributes) {

            // for each attribute the PIP is subscribed to
            for (Attribute subscribedAttribute : subscriptions) {

                // if the attributeId matches one of the attributes the PIP has an active subscription
                if (subscribedAttribute.getAttributeId().equals(attributeToUnsubscribe.getAttributeId())) {
                    // if the attribute is of category Environment,
                    // or if the additionalInformation matches
                    if (subscribedAttribute.getCategory() == Category.ENVIRONMENT ||
                            subscribedAttribute.getAdditionalInformations()
                                    .equals(attributeToUnsubscribe.getAdditionalInformations())) {
                        atLeastOneAttributeRemoved = removeAttribute(subscribedAttribute);
                        break;
                    }
                }
            }
        }
        return atLeastOneAttributeRemoved;
    }

    private boolean removeAttribute(Attribute subscribedAttribute) {
        if (!subscriptions.remove(subscribedAttribute)) {
            throw new IllegalStateException("Unable to remove attribute from list");
        }
        return true;
    }

    private void addAdditionalInformation(RequestType request, Attribute attribute) {
        String filter = request.getAttributeValue(expectedCategoryMap.get(attribute.getAttributeId()));
        attribute.setAdditionalInformations(filter);
    }

    public boolean isEnvironmentCategory(Attribute attribute) {
        return attribute.getCategory() == Category.ENVIRONMENT;
    }

    /**
     * Effective retrieval of the monitored value.
     *
     * @return the requested value
     * @throws PIPException
     */
    private String read(String filePath) throws PIPException {
        try {
            Path path = Paths.get(filePath);
            // TODO UCS-33 NOSONAR
            String value = new String(Files.readAllBytes(path));
            journal.logString(formatJournaling(value));
            return value;
        } catch (IOException e) {
            throw new PIPException("Attribute Manager error : " + e.getMessage());
        }
    }

    /**
     * Effective retrieval of the monitored value looking for the line containing a filter.
     * Note that each line of the file MUST be composed of two fields separated
     * by either one or more spaces or tab, e.g.:
     * filter attribute
     * filter   attribute
     * filter\tattribute.
     *
     * @param filter the string to be used to search for the item we're interested into
     * @return the requested value
     * @throws PIPException
     */
    private String read(String filePath, String filter) throws PIPException {
        // TODO UCS-33 NOSONAR
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            for (String line; (line = br.readLine()) != null; ) {
                String key = line.split("\\s+")[0];
                if (key.equals(filter)) {
                    String value = line.split("\\s+")[1];
                    journal.logString(formatJournaling(value, filter));
                    return value;
                }
            }
        } catch (Exception e) {
            throw new PIPException("Attribute Manager error: " + e.getMessage());
        }
        throw new PIPException("Attribute Manager: unable to retrieve a value for filter: \"" + filter + "\"\n");
    }

    private final void setFilePath(String attributeId, String filePath) {
        String absFilePath = FileUtility.findFileAbsPathUsingClassLoader(filePath);
        if (absFilePath != null) {
            this.filePathMap.put(attributeId, absFilePath);
        } else {
            this.filePathMap.put(attributeId, filePath);
        }
        Reject.ifNull(this.filePathMap.get(attributeId));
    }

    private String formatJournaling(String... strings) {
        StringBuilder logStringBuilder = new StringBuilder();
        logStringBuilder.append("VALUE READ: ").append(strings[0]);

        if (strings.length > 1) {
            logStringBuilder.append(" FOR FILTER: ").append(strings[1]);
        }
        return logStringBuilder.toString();
    }

    @Override
    public void update(String data) throws PIPException {
// fixme: who is supposed to call this?
//        try {
//            Path path = Paths.get( filePath );
//            Files.write( path, data.getBytes() );
//        } catch( IOException e ) {
//            log.severe( "Error updating attribute : " + e.getMessage() );
//        }
        System.err.println("update() method not implemented");
    }

    @Override
    public void retrieve(RequestType request,
                         List<Attribute> attributeRetrievals) {
        log.severe("Multiple retrieve is unimplemented");
    }

    @Override
    public void subscribe(RequestType request,
                          List<Attribute> attributeRetrieval) {
        log.severe("Multiple subscribe is unimplemented");
    }

    @Override
    public void performObligation(ObligationInterface obligation) {
        if (obligation != null) {
            log.severe("Perform obligation is unimplemented");
        }
    }

    public void addSubscription(Attribute attribute) {
        if (!subscriptions.contains(attribute)) {
            subscriptions.add(attribute);
        }
    }

    /**
     * Retrieve the value of the
     */
    public void checkSubscriptions() {
        for (Attribute attribute : subscriptions) {

            // first, get the old value as memorized in the attribute
            String oldValue = attribute.getAttributeValues(attribute.getDataType()).get(0);
            //get(0) assumes that the attribute has only one value

            String value;
//            log.log( Level.INFO, "Polling on value of the attribute " + attribute.getAttributeId() + " for change." );

            // then, retrieve the new value.
            //   Note that the retrieve method updates the attribute with the value just retrieved
            try {
                value = retrieve(attribute);
            } catch (PIPException e) {
                log.log(Level.WARNING, "Error reading attribute " + attribute.getAttributeId());
                return;
            }

            if (!oldValue.equals(value)) { // if the attribute has changed
                log.log(Level.INFO,
                        "Attribute {0}={1}:{2} changed at {3}",
                        new Object[]{attribute.getAttributeId(), value,
                                attribute.getAdditionalInformations(),
                                System.currentTimeMillis()});
                attribute.setValue(attribute.getDataType(), value);
                notifyRequestManager(attribute);
            }
        }
    }

    public void notifyRequestManager(Attribute attribute) {
        AttributeChangeMessage attrChangeMessage = new AttributeChangeMessage(ENTITIES.PIP.toString(), ENTITIES.CH.toString());
        ArrayList<Attribute> attrList = new ArrayList<>(Arrays.asList(attribute));
        attrChangeMessage.setAttributes(attrList);
        getRequestManager().sendMessage(attrChangeMessage);
    }
}
