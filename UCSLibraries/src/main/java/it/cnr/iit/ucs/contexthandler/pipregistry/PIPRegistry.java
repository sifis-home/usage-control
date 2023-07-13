package it.cnr.iit.ucs.contexthandler.pipregistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

import com.google.common.base.Throwables;

import it.cnr.iit.ucs.pip.PIPCHInterface;
import it.cnr.iit.xacml.Attribute;
import it.cnr.iit.xacml.Category;
import oasis.names.tc.xacml.core.schema.wd_17.RequestType;

public class PIPRegistry implements PIPRegistryInterface {

	private static final Logger log = Logger.getLogger(PIPRegistry.class.getName());

	private List<PIPCHInterface> pipList;

	public PIPRegistry() {
		super();
		pipList = new ArrayList<>();
	}

	@Override
	public boolean add(PIPCHInterface pip) {
		return pipList.add(pip);
	}

	@Override
	public boolean remove(PIPCHInterface pip) {
		return pipList.remove(pip);
	}

	@Override
	public void removeAll() {
		pipList.clear();
	}

	@Override
	public void unsubscribeAll(List<Attribute> attributes) {
		for (PIPCHInterface pip : pipList) {
			try {
				pip.unsubscribe(attributes);
			} catch (Exception e) {
				log.severe("Error unsubscribe : " + e.getMessage());
			}
		}
	}

	@Override
	public void subscribeAll(RequestType requestType) {
		try {
			for (PIPCHInterface pip : pipList) {
				pip.subscribe(requestType);
			}
		} catch (Exception e) {
			log.severe("Error subscribe : " + e.getMessage());
			Throwables.throwIfUnchecked(new RuntimeException("Error subscribe : " + e.getMessage()));
		}
	}

	@Override
	public void retrieveAll(RequestType requestType) {
		try {
			for (PIPCHInterface pip : pipList) {
				pip.retrieve(requestType);
			}
		} catch (Exception e) {
			log.severe("Error retrieve : " + e.getMessage());
			Throwables.throwIfUnchecked(new RuntimeException("Error retrieve : " + e.getMessage()));
		}
	}

	@Override
	public Optional<PIPCHInterface> getByAttributeId(String attributeId) {
		Attribute attribute = new Attribute();
		attribute.setAttributeId(attributeId);
		return getByAttribute(attribute);
	}

	@Override
	public Optional<PIPCHInterface> getByAttribute(Attribute attribute) {
		for (PIPCHInterface pip : pipList) {
			if (pip.getAttributeIds().contains(attribute.getAttributeId())) {
				return Optional.of(pip);
			}
		}
		return Optional.empty();
	}

	@Override
	public boolean hasAttribute(Attribute attribute) {
		return getByAttribute(attribute).isPresent();
	}


	/**
	 * Given a list of attributes, invokes the PIPs' subscribe(request, attributeId)
	 * method if that attributeId id handled by a PIP.
	 *
	 * This method has to be revised when a PIP multiAttribute will be
	 * implemented. Indeed, the subscribe(request) method would subscribe
	 * all the attributes the PIP is handling. (Currently, since there is
	 * no MultiAttribute PIP, the subscribe(request) takes only the first
	 * attribute in the attributesMap (call getAttributes().get(0)).
	 * On the contrary, if the PIP is multiAttribute, we need to specify
	 * which attribute we want the PIP to subscribe to.
	 *
	 * @param request the request
	 * @param attributes the list of attributes to subscribe to. It
	 *                   can contain attributes not managed by any PIP.
	 */
	@Override
	public void subscribe(RequestType request, List<Attribute> attributes) {

		// loop on all the PIPs
		for (PIPCHInterface pip : pipList) {

			//loop on the ongoing attributes
			for (Attribute attr : attributes) {

				// if among the attributeIds handled by the PIP there is
				// the attributeId of the attribute we are considering,
				if (pip.getAttributeIds().contains(attr.getAttributeId())) {
					try {
						pip.subscribe(request, attr.getAttributeId());
					} catch (Exception e) {
						log.severe("Error subscribe : " + e.getMessage());
						Throwables.throwIfUnchecked(new RuntimeException("Error subscribe : " + e.getMessage()));
					}
				}
			}
		}
	}
}
