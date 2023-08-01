/*
 * CNR - IIT (2015-2016)
 *
 * @authors Fabio Bindi and Filippo Lauria
 */
package it.cnr.iit.ucs.pip;

import it.cnr.iit.ucs.constants.ENTITIES;
import it.cnr.iit.ucs.exceptions.PIPException;
import it.cnr.iit.ucs.message.attributechange.AttributeChangeMessage;
import it.cnr.iit.ucs.properties.components.PipProperties;
import it.cnr.iit.ucs.requestmanager.RequestManagerInterface;
import it.cnr.iit.utility.errorhandling.Reject;
import it.cnr.iit.xacml.Attribute;
import it.cnr.iit.xacml.Category;
import it.cnr.iit.xacml.DataType;
import oasis.names.tc.xacml.core.schema.wd_17.RequestType;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * General PIP abstract class
 *
 * @author Fabio Bindi and Filippo Lauria and Antonio La Marra and Alessandro Rosetti
 */
public abstract class PIPBase implements PIPCHInterface, PIPOMInterface {

    private RequestManagerInterface requestManager;

    /**
     * Map having the attributeId as key and an Attribute object as value
     */
    private final HashMap<String, Attribute> attributesMap = new HashMap<>();

    /**
     * List that stores the attributes on which a 'subscribe' has been performed
     */
    public final BlockingQueue<Attribute> subscriptions = new LinkedBlockingQueue<>();

    // Whenever a PIP has to retrieve some information related to an attribute
    // that is stored inside the request, it has to know in advance all the
    // information to retrieve that attribute. E.g. if this PIP has to retrieve
    // information about the subject, it has to know in advance which is the
    // attribute id qualifying the subject, its category and the data-type used,
    // otherwise it is not able to retrieve the value of that attribute, hence it
    // would not be able to communicate with the AM properly
    /**
     * Map having attributeId as key and the expected category as value
     */
    public final Map<String, Category> expectedCategoryMap = new HashMap<>();

    private final PipProperties properties;

    public PIPBase(PipProperties properties) {
        Reject.ifNull(properties);
        this.properties = properties;
    }


    public boolean init(PipProperties properties) {
        try {
            for (Map<String, String> attributeMap : properties.getAttributes()) {

                Attribute attribute = new Attribute();
                buildAttribute(attribute, attributeMap);

                addAttribute(attribute);

                // timer for polling the value of the attribute
                PIPSubscriberTimer subscriberTimer = new PIPSubscriberTimer(this);
                subscriberTimer.setRate(properties.getRefreshRate());
                subscriberTimer.start();
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }


    /**
     * Use the information in the PipProperties to build an attribute
     *
     * @param attribute    the attribute to build
     * @param attributeMap the part of the PipProperties representing an attribute
     */
    public void buildAttribute(Attribute attribute, Map<String, String> attributeMap) {
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
     * Add the attribute passed as argument to the list of subscriptions
     *
     * @param attribute the attribute to add to the list
     */
    public void addSubscription(Attribute attribute) {
        if (!subscriptions.contains(attribute)) {
            subscriptions.add(attribute);
        }
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
                            subscribedAttribute.getAdditionalInformation()
                                    .equals(attributeToUnsubscribe.getAdditionalInformation())) {
                        atLeastOneAttributeRemoved = removeAttribute(subscribedAttribute);
                        break;
                    }
                }
            }
        }
        return atLeastOneAttributeRemoved;
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
                getLogger().log(Level.WARNING, "Error reading attribute " + attribute.getAttributeId());
                return;
            }

            if (!oldValue.equals(value)) { // if the attribute has changed
                getLogger().log(Level.INFO,
                        "Attribute {0}={1}:{2} changed at {3}",
                        new Object[]{attribute.getAttributeId(), value,
                                attribute.getAdditionalInformation(),
                                System.currentTimeMillis()});
                attribute.setValue(attribute.getDataType(), value);
                notifyRequestManager(attribute);
            }
        }
    }


    /**
     * Remove the attribute passed as argument from the list of subscriptions
     *
     * @param subscribedAttribute the attribute to remove from the list
     */
    private boolean removeAttribute(Attribute subscribedAttribute) {
        if (!subscriptions.remove(subscribedAttribute)) {
            throw new IllegalStateException("Unable to remove attribute from list");
        }
        return true;
    }


    /**
     * Check if the attribute's category is 'environment'
     *
     * @param attribute the attribute to examine
     * @return true is the category of the attribute is 'environment',
     * false otherwise
     */
    public boolean isEnvironmentCategory(Attribute attribute) {
        return attribute.getCategory() == Category.ENVIRONMENT;
    }


    /**
     * Add information coming from the request to the attribute.
     * In particular, the attribute is added the value of either the 'subject-id',
     * the 'resource-id', or the 'action-id' attribute. This information
     * is stored in the additionalInformation field of the attribute.
     *
     * @param request   The XACML request
     * @param attribute the attribute to which the additionalInformation has to be set
     */
    private void addAdditionalInformation(RequestType request, Attribute attribute) {
        String filter = request.getAttributeValue(expectedCategoryMap.get(attribute.getAttributeId()));
        attribute.setAdditionalInformation(filter);
    }


    @Override
    public final ArrayList<String> getAttributeIds() {
        return new ArrayList<>(attributesMap.keySet());
    }

    @Override
    public final ArrayList<Attribute> getAttributes() {
        return new ArrayList<>(attributesMap.values());
    }

    @Override
    public final HashMap<String, Attribute> getAttributesCharacteristics() {
        return attributesMap;
    }

    @Override
    public RequestManagerInterface getRequestManager() {
        Reject.ifNull(requestManager, "request manager is null");
        return requestManager;
    }

    @Override
    public void setRequestManager(RequestManagerInterface requestManager) {
        Reject.ifNull(requestManager);
        this.requestManager = requestManager;
    }

    protected final boolean addAttribute(Attribute attribute) {
        Reject.ifNull(attribute);
        if (attributesMap.containsKey(attribute.getAttributeId())) {
            return false;
        }
        attributesMap.put(attribute.getAttributeId(), attribute);
        return true;
    }


    /**
     * Send a message to the Request Manager to notify it that an attribute
     * value changed.
     *
     * @param attribute the attribute whose value changed
     */
    public void notifyRequestManager(Attribute attribute) {
        AttributeChangeMessage attrChangeMessage = new AttributeChangeMessage(ENTITIES.PIP.toString(), ENTITIES.CH.toString());
        ArrayList<Attribute> attrList = new ArrayList<>(Arrays.asList(attribute));
        attrChangeMessage.setAttributes(attrList);
        getRequestManager().sendMessage(attrChangeMessage);
    }

    /**
     * Get the queue containing the subscriptions
     *
     * @return the queue containing the subscriptions
     */
    public BlockingQueue<Attribute> getSubscriptions() {
        return subscriptions;
    }

    protected abstract Logger getLogger();
}
