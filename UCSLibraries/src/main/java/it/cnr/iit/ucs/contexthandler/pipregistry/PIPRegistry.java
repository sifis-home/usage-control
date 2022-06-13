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
	 * Given a list of attributes, invokes the PIPs' subscribe method
	 * for each attribute, if that attribute is owned by a PIP.
	 * If an attribute has category different from ENVIRONMENT, e.g.,
	 * is a SUBJECT-related attribute, use the request to retrieve
	 * the value of the SUBJECT attribute and set it as
	 * 'additionalInformation' of the attribute. This is needed by
	 * the PIP for subscribing to the right attribute value.
	 * For example, a PIPReader responsible for the 'role' attribute
	 * (a SUBJECT-related attribute) needs to know the 'subject-id'
	 * to monitor.
	 *
	 * @param request the request
	 * @param attributes the list of attributes to subscribe to. It
	 *                   can contain attributes not managed by any PIP.
	 */
	@Override
	public void subscribe(RequestType request, List<Attribute> attributes) {
		for (PIPCHInterface pip : pipList) {
			int attributesNumber = pip.getAttributes().size();
			for (int i = 0; i < attributesNumber; i++) {
				Attribute pipAttribute = pip.getAttributes().get(i);
				Category category = pipAttribute.getCategory();
				for (Attribute a : attributes) {
					if (pipAttribute.getAttributeId().equals(a.getAttributeId())) {
						try {
							if (!category.equals(Category.ENVIRONMENT)) {
								String filter = request.getAttributeValue(category);
								a.setAdditionalInformations(filter);
							}
							pip.subscribe(a);
						} catch (Exception e) {
							log.severe("Error subscribe : " + e.getMessage());
							Throwables.throwIfUnchecked(new RuntimeException("Error subscribe : " + e.getMessage()));
						}
					}
				}
			}
		}
	}
}
