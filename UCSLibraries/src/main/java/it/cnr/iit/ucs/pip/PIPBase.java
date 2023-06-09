/*
 * CNR - IIT (2015-2016)
 *
 * @authors Fabio Bindi and Filippo Lauria
 */
package it.cnr.iit.ucs.pip;

import java.util.ArrayList;
import java.util.HashMap;

import it.cnr.iit.ucs.properties.components.PipProperties;
import it.cnr.iit.ucs.requestmanager.RequestManagerInterface;
import it.cnr.iit.utility.errorhandling.Reject;
import it.cnr.iit.xacml.Attribute;

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

    private final PipProperties properties;

    public PIPBase( PipProperties properties ) {
        Reject.ifNull( properties );
        this.properties = properties;
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
        Reject.ifNull( requestManager, "request manager is null" );
        return requestManager;
    }

    @Override
    public void setRequestManager( RequestManagerInterface requestManager ) {
        Reject.ifNull( requestManager );
        this.requestManager = requestManager;
    }

    protected final boolean addAttribute( Attribute attribute ) {
        Reject.ifNull( attribute );
        if( attributesMap.containsKey( attribute.getAttributeId() ) ) {
            return false;
        }
        attributesMap.put( attribute.getAttributeId(), attribute );
        return true;
    }

}
