package it.cnr.iit.ucs.requestmanager;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.xml.bind.JAXBException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import it.cnr.iit.ucs.constants.STATUS;
import it.cnr.iit.ucs.properties.UCSProperties;
import it.cnr.iit.ucs.properties.components.PipProperties;
import it.cnr.iit.utility.JAXBUtility;
import it.cnr.iit.xacml.Attribute;
import it.cnr.iit.xacml.Category;
import it.cnr.iit.xacml.DataType;
import oasis.names.tc.xacml.core.schema.wd_17.PolicyType;
import oasis.names.tc.xacml.core.schema.wd_17.RequestType;

@Component
public class RequestEnricher {

	@Autowired
	private UCSProperties ucsProperties;

	private final static String TRYACCESS_POLICY = "pre";
	private final static String STARTACCESS_POLICY = "ongoing";
	private final static String ENDACCESS_POLICY = "post";

	private static final String DSA_ID_ATTRIBUTE = "urn:oasis:names:tc:xacml:3.0:resource:dsa-id";

	private final Logger log = Logger.getLogger(RequestEnricher.class.getName());

	public RequestEnricher() {
	}

	public String enrichRequest(String request, String policy) {
		log.info("enrichRequest received at " + System.currentTimeMillis());
//		PolicyHelper policyHelper = PolicyHelper.buildPolicyHelper(policy);
		String fattenRequest = addPolicyInformationInRequest(policy, request);
//		List<Attribute> attributes = policyHelper.getAttributesForCondition(TRYACCESS_POLICY);
//		String requestFull = makeRequestFull(fattenRequest, attributes, STATUS.TRY, true);
//		return requestFull;
		return fattenRequest;
	}

	private String addPolicyInformationInRequest(String policy, String request) {
		try {
			PolicyType policyType = JAXBUtility.unmarshalToObject(PolicyType.class, policy);
			RequestType requestType = JAXBUtility.unmarshalToObject(RequestType.class, request);
			String policyId = policyType.getPolicyId();
			List<String> arrayList = new ArrayList<>();
			arrayList.add(policyId);
			requestType.addAttribute(Category.RESOURCE.toString(), DataType.STRING.toString(), DSA_ID_ATTRIBUTE,
					arrayList);
			return JAXBUtility.marshalToString(RequestType.class, requestType, "Request", JAXBUtility.SCHEMA);
		} catch (Exception e) {
			e.printStackTrace();
			log.info("UNABLE TO PARSE POLICY: " + policy + "\nAND REQUEST" + request);
			return null;
		}
	}

	private synchronized String makeRequestFull(String request, List<Attribute> attributes, STATUS status,
			boolean complete) {
		try {
			RequestType requestType = JAXBUtility.unmarshalToObject(RequestType.class, request);

			log.info("makeRequestFull attributes : " + Arrays.toString(attributes.toArray()));

			// handles all the cases except startaccess
			if (status == STATUS.TRY || status == STATUS.END || status == STATUS.REVOKE) {
				retrieveLocalAttributes(requestType, attributes);
			}
			/*
			 * handles the startaccess case which is different because in this case we have
			 * to perform the subscribe operation to signal to the PIPs to continuously
			 * monitor the attributes
			 */
			if (status == STATUS.START) {
				subscribeLocalAttributes(requestType, attributes);
			}
			String requestString = JAXBUtility.marshalToString(RequestType.class, requestType, "Request",
					JAXBUtility.SCHEMA);
			return requestString;
		} catch (JAXBException exception) {
			exception.printStackTrace();
			return null;
		}
	}

//	private void retrieveLocalAttributes(RequestType requestType, List<Attribute> attributes) {
//		try {
//			// FIXME dummy way of doing this
//
//			for (PipProperties pipProperty : ucsProperties.getPipList()) {
//
//				String clazz = pipProperty.getName();
//				Class<?> classType = Class.forName(clazz);
//				Object obj = classType.getConstructor(PipProperties.class).newInstance(pipProperty);
//				log.severe("className for PIP from properties =" + PIPBase.class.getName());
//
//				classType = Class.forName(PIPBase.class.getName());
//				log.severe("className for PIPBase=" + PIPBase.class.getName());
//				ArrayList<String> idsList = (ArrayList<String>) classType.getDeclaredMethod("getAttributeIds")
//						.invoke(PIPBase.class, null);
//
//				classType = Class.forName(clazz);
//				log.severe("className for PIP from properties =" + PIPBase.class.getName());
//
//				for (int i = 0; i < attributes.size();) {
//					Attribute attribute = attributes.get(i);
//					if (idsList.contains(attribute.getAttributeId())) {
//						classType.getMethod("retrieve", void.class).invoke(null, requestType);
//						attributes = removeAttributesFromList(attributes, idsList);
//					} else {
//						++i;
//					}
//				}
//			}
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//	}

	private void retrieveLocalAttributes(RequestType requestType, List<Attribute> attributes) {
		try {

			log.severe("attributesIdList:");
			attributes.stream().forEach(el -> log.severe(el.getAttributeId()));
			// FIXME dummy way of doing this
			for (PipProperties pipProperty : ucsProperties.getPipList()) {
				String clazz = pipProperty.getName();
				Class<?> classType = Class.forName(clazz);

				for (Attribute attribute : attributes) {
					log.severe("processing attribute " + attribute.getAttributeId());

//					log.severe("attributeId from list: " + attribute.getAttributeId());
//
//					pipProperty.getAttributes().stream()
//							.forEach(attr -> attr.entrySet().stream().forEach(entry -> log.severe("pip attribute key: "
//									+ entry.getKey() + ", pip attribute value: " + entry.getValue())));

					Map<String, String> pipToCall = pipProperty.getAttributes().stream()
							.filter(pip -> pip.get("ATTRIBUTE_ID").equals(attribute.getAttributeId())).findFirst()
							.orElse(new HashMap<>());
					if (pipToCall.isEmpty()) {
						log.severe("The attribute " + attribute.getAttributeId() + " is not monitored from the PIP "
								+ pipProperty.getName());
						continue;
					}

					pipToCall.entrySet().stream().forEach(pip -> log
							.severe("pipToCall key: " + pip.getKey() + ", pipToCall value: " + pip.getValue()));

					Object obj = classType.getConstructor(PipProperties.class).newInstance(pipProperty);
					Method method = classType.getMethod("retrieve", RequestType.class);
					method.invoke(obj, requestType);
//						pipProperty.retrieve(requestType);
//						attributes = removeAttributesFromList(attributes, pip.getAttributeIds());
				}
			}
		} catch (

		Exception e) {
			e.printStackTrace();
		}
	}

	private void subscribeLocalAttributes(RequestType requestType, List<Attribute> attributes) {
		try {
			// FIXME dummy way of doing this
			// for (Attribute attribute : attributes) {

			for (PipProperties pipProperty : ucsProperties.getPipList()) {
				// if (pip.getAttributes().contains(attribute)) {
//				PIPJdbc reader = new PIPJdbc(pipProperty);
//				reader.subscribe(requestType);
				// }
			}
			// }
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private List<Attribute> removeAttributesFromList(List<Attribute> attributes, ArrayList<String> attributeIds) {
		List<Attribute> tmpList = new ArrayList<>();
		for (Attribute attribute : attributes) {
			if (!attributeIds.contains(attribute.getAttributeId())) {
				tmpList.add(attribute);
			}
		}
		return tmpList;
	}

}
