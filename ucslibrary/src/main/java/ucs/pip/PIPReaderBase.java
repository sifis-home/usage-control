package ucs.pip;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import ucs.constants.ENTITIES;
import ucs.exceptions.PIPException;
import ucs.journaling.JournalingInterface;
import ucs.message.attributechange.AttributeChangeMessage;
import ucs.obligationmanager.ObligationInterface;
import ucs.properties.components.PipProperties;
import utility.errorhandling.Reject;
import wd_17.RequestType;
import xacml.Attribute;
import xacml.Category;
import xacml.DataType;

public abstract class PIPReaderBase extends PIPBase {
    private static Logger log = Logger.getLogger( PIPReaderBase.class.getName() );
    protected JournalingInterface journal;
    protected final BlockingQueue<Attribute> subscriptions = new LinkedBlockingQueue<>();

    /**
     * Whenever a PIP has to retrieve some informations related to an attribute
     * that is stored inside the request, it has to know in advance all the
     * informations to retrieve that attribute. E.g. if this PIP has to retrieve
     * the informations about the subject, it has to know in advance which is the
     * attribute id qualifying the subject, its category and the data-type used,
     * otherwise it is not able to retrieve the value of that attribute, hence it
     * would not be able to communicate with the AM properly
     */
    protected Category expectedCategory;

    public PIPReaderBase(PipProperties properties) {
        super(properties);
    }

    /**
     * Performs the retrieve operation.
     * The retrieve operation is a very basic operation in which the PIP simply
     * asks to the AttributeManager the value in which it is interested into. Once
     * that value has been retrieved, the PIP will fatten the request.
     *
     * @param request
     *          this is an in/out parameter
     */
    @Override
    public void retrieve( RequestType request ) throws PIPException {
        Reject.ifNull( request );

        Attribute attribute = getAttributes().get( 0 );
        log.info("Attribute: " + attribute.getAttributeId() + ", additional info: " + attribute.getAdditionalInformations());
        addAdditionalInformation( request, attribute );
        log.info("Attribute: " + attribute.getAttributeId() + ", additional info: " + attribute.getAdditionalInformations());
        String value = retrieve( attribute );

        request.addAttribute( attribute, value );
    }

    /**
     * This is the function called by the context handler whenever we have a
     * remote retrieve request
     */
    @Override
    public String retrieve( Attribute attribute ) throws PIPException {
//        log.info("Attribute: " + attribute.toString());
        if( isEnvironmentCategory( attribute ) ) {
            return read();
        } else {
            return read( attribute.getAdditionalInformations() );
        }
    }

    /**
     * Performs the subscribe operation. This operation is very similar to the
     * retrieve operation. The only difference is that in this case we have to
     * signal to the thread in charge of performing the polling that it has to
     * poll a new attribute
     *
     * @param request
     *          IN/OUT parameter
     */
    @Override
    public void subscribe( RequestType request ) throws PIPException {
        Reject.ifNull( request );

        Attribute attribute = getAttributes().get( 0 );
        addAdditionalInformation( request, attribute );

        String value = subscribe( attribute );

        request.addAttribute( attribute, value );
    }

    /**
     * This is the function called by the context handler whenever we have a
     * remote retrieve request
     */
    @Override
    public String subscribe( Attribute attribute ) throws PIPException {
        Reject.ifNull( attribute );

        String value = retrieve( attribute );
        DataType dataType = attribute.getDataType();
        attribute.setValue( dataType, value );
        addSubscription( attribute );

        return value;

    }

    /**
     * Checks if it has to remove an attribute (the one passed in the list) from
     * the list of subscribed attributes
     *
     * @param attributes
     *          the list of attributes that must be unsubscribed
     */
    @Override
    public boolean unsubscribe( List<Attribute> attributes ) throws PIPException {
        Reject.ifEmpty( attributes );
        for( Attribute attribute : attributes ) {
            if( attribute.getAttributeId().equals( getAttributeIds().get( 0 ) ) ) {
                for( Attribute subscribedAttribute : subscriptions ) {
                    if( subscribedAttribute.getCategory() == Category.ENVIRONMENT ||
                            subscribedAttribute.getAdditionalInformations()
                                    .equals( attribute.getAdditionalInformations() ) ) {
                        return removeAttribute( subscribedAttribute );
                    }
                }
            }
        }
        return false;
    }

    private boolean removeAttribute( Attribute subscribedAttribute ) {
        if( !subscriptions.remove( subscribedAttribute ) ) {
            throw new IllegalStateException( "Unable to remove attribute from list" );
        }
        return true;
    }

    private void addAdditionalInformation( RequestType request, Attribute attribute ) {
        String filter = request.getAttributeValue( expectedCategory );
        log.info("Filter: " + filter);
        log.info("expectedCategory: " + expectedCategory);
        attribute.setAdditionalInformations( filter );
    }

    public boolean isEnvironmentCategory( Attribute attribute ) {
        return attribute.getCategory() == Category.ENVIRONMENT;
    }

    protected abstract String read() throws PIPException;

    protected abstract String read( String filter ) throws PIPException;

    public void addSubscription( Attribute attribute ) {
        if( !subscriptions.contains( attribute ) ) {
            subscriptions.add( attribute );
        }
    }


    @Override
    public void retrieve( RequestType request,
                          List<Attribute> attributeRetrievals ) {
        log.severe( "Multiple retrieve is unimplemented" );
    }

    @Override
    public void subscribe( RequestType request,
                           List<Attribute> attributeRetrieval ) {
        log.severe( "Multiple subscribe is unimplemented" );
    }

    @Override
    public void performObligation( ObligationInterface obligation ) {
        log.severe( "Perform obligation is unimplemented" );
    }

    public void checkSubscriptions() {
        for( Attribute attribute : subscriptions ) {
            String value = "";
            log.log( Level.INFO, "Polling on value of the attribute " + attribute.getAttributeId() + " for change." );

            try {
                value = retrieve( attribute );
            } catch( PIPException e ) {
                log.log( Level.WARNING, "Error reading attribute " + attribute.getAttributeId() );
                return;
            }

            String oldValue = attribute.getAttributeValues( attribute.getDataType() ).get( 0 );
            if( !oldValue.equals( value ) ) { // if the attribute has changed
                log.log( Level.INFO,
                        "Attribute {0}={1}:{2} changed at {3}",
                        new Object[] { attribute.getAttributeId(), value,
                                attribute.getAdditionalInformations(),
                                System.currentTimeMillis() } );
                attribute.setValue( attribute.getDataType(), value );
                notifyRequestManager( attribute );
            }
        }
    }

    public void notifyRequestManager( Attribute attribute ) {
        AttributeChangeMessage attrChangeMessage = new AttributeChangeMessage( ENTITIES.PIP.toString(), ENTITIES.CH.toString() );
        ArrayList<Attribute> attrList = new ArrayList<>( Arrays.asList( attribute ) );
        attrChangeMessage.setAttributes( attrList );
        getRequestManager().sendMessage( attrChangeMessage );
    }


}
