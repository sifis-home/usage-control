package ucs.contexthandler.pipregistry;

import java.util.List;
import java.util.Optional;

import ucs.pip.PIPCHInterface;
import xacml.Attribute;
import wd_17.RequestType;

/**
 * This class handles a group of PIP basic operations
 *
 * @author Alessandro Rosetti
 */
public interface PIPRegistryInterface {

    public boolean add( PIPCHInterface pip );

    public boolean remove( PIPCHInterface pip );

    public void removeAll();

    public void unsubscribeAll( List<Attribute> attributes );

    public void subscribeAll( RequestType requestType );

    public void retrieveAll( RequestType requestType );

    public Optional<PIPCHInterface> getByAttributeId( String attributeId );

    public Optional<PIPCHInterface> getByAttribute( Attribute attribute );

    public boolean hasAttribute( Attribute attribute );

}