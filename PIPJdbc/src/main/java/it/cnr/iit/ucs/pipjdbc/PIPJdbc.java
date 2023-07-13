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
package it.cnr.iit.ucs.pipjdbc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import it.cnr.iit.ucs.constants.ENTITIES;
import it.cnr.iit.ucs.exceptions.PIPException;
import it.cnr.iit.ucs.journaling.JournalBuilder;
import it.cnr.iit.ucs.journaling.JournalingInterface;
import it.cnr.iit.ucs.message.attributechange.AttributeChangeMessage;
import it.cnr.iit.ucs.obligationmanager.ObligationInterface;
import it.cnr.iit.ucs.pip.PIPBase;
import it.cnr.iit.ucs.pip.PIPKeywords;
import it.cnr.iit.ucs.pipjdbc.db.DBInfoStorage;
import it.cnr.iit.ucs.pipjdbc.pipmysql.tables.UserAttributes;
import it.cnr.iit.ucs.properties.components.PipProperties;
import it.cnr.iit.utility.errorhandling.Reject;
import it.cnr.iit.xacml.Attribute;
import it.cnr.iit.xacml.Category;
import it.cnr.iit.xacml.DataType;
import oasis.names.tc.xacml.core.schema.wd_17.AttributesType;
import oasis.names.tc.xacml.core.schema.wd_17.RequestType;

/**
 * The task this PIP is to read data from a file when requested. The Path to
 * reach the file is passed as parameter to the pip.
 *
 * @author Antonio La Marra, Alessandro Rosetti
 *
 */
public final class PIPJdbc extends PIPBase {

	private static Logger log = Logger.getLogger(PIPJdbc.class.getName());
	private JournalingInterface journal;

	// list that stores the attributes on which a subscribe has been performed
	protected final BlockingQueue<Attribute> subscriptions = new LinkedBlockingQueue<>();
	private static final String SUBJECT_ID = "urn:oasis:names:tc:xacml:1.0:subject:subject-id";

	/**
	 * Whenever a PIP has to retrieve some information related to an attribute that
	 * is stored inside the request, it has to know in advance all the information
	 * to retrieve that attribute. E.g. if this PIP has to retrieve the information
	 * about the subject, it has to know in advance which is the attribute id
	 * qualifying the subject, its category and the data-type used, otherwise it is
	 * not able to retrieve the value of that attribute, hence it would not be able
	 * to communicate with the AM properly
	 */
	private Category expectedCategory;

	public static final String DB_URI = "DB_URI";

	public PIPJdbc(PipProperties properties) {
		super(properties);
		Reject.ifFalse(init(properties), "Error initialising pip : " + properties.getId());
	}

	private boolean init(PipProperties properties) {
		try {
			log.severe("Initializing PIPJdbc...");
			List<Map<String, String>> pipProperties = properties.getAttributes();
			Reject.ifFalse(pipProperties.get(0).containsKey(DB_URI), "missing database uri");
			DBInfoStorage.start(pipProperties.get(0).get(DB_URI));
			pipProperties.stream().forEach(pip -> addAttributes(pip));
			journal = JournalBuilder.build(properties);
			PIPJdbcSubscriberTimer subscriberTimer = new PIPJdbcSubscriberTimer(this);
			subscriberTimer.setRate(properties.getRefreshRate());
			subscriberTimer.start();
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	private void addAttributes(Map<String, String> pip) {
		Attribute attribute = new Attribute();
		attribute.setAttributeId(pip.get(PIPKeywords.ATTRIBUTE_ID));
		Category category = Category.toCATEGORY(pip.get(PIPKeywords.CATEGORY));
		attribute.setCategory(category);
		DataType dataType = DataType.toDATATYPE(pip.get(PIPKeywords.DATA_TYPE));
		attribute.setDataType(dataType);
		expectedCategory = Category.toCATEGORY(pip.get(PIPKeywords.EXPECTED_CATEGORY));
		Reject.ifNull(expectedCategory, "missing expected category");
		addAttribute(attribute);
	}

	/**
	 * Performs the retrieve operation. The retrieve operation is a very basic
	 * operation in which the PIP simply asks to the AttributeManager the value in
	 * which it is interested into. Once that value has been retrieved, the PIP will
	 * fatten the request.
	 *
	 * @param request this is an in/out parameter
	 */
	@Override
	public void retrieve(RequestType request) throws PIPException {
		Reject.ifNull(request);

		Attribute attribute = getAttributes().get(0);
		addAdditionalInformation(request, attribute);
		String value = retrieve(attribute);
		Reject.ifNull(value);
		UserAttributes userAttr = null;
		try {
			userAttr = new ObjectMapper().readValue(value, UserAttributes.class);
		} catch (Exception e) {
			e.printStackTrace();
		}

		addAttributesToRequest(getAttributes(), request, userAttr);
	}

	private void addAttributesToRequest(ArrayList<Attribute> attributes, RequestType request, UserAttributes userAttr) {
		for (Attribute attr : getAttributes()) {
			if (attr.getAttributeId().contains("role")) {
				request.addAttribute(attr, userAttr.getRole());
			}
			if (attr.getAttributeId().contains("ismemberof")) {
				request.addAttribute(attr, userAttr.getMember());
			}
			if (attr.getAttributeId().contains("organisation")) {
				request.addAttribute(attr, userAttr.getOrgname());
			}
			if (attr.getAttributeId().contains("country")) {
				request.addAttribute(attr, userAttr.getCountry());
			}
		}

	}

	/**
	 * This is the function called by the context handler whenever we have a remote
	 * retrieve request
	 */
	@Override
	public String retrieve(Attribute attribute) throws PIPException {
		Map<String, String> attributesToValues = new HashMap<>();
		try {
			attributesToValues = new ObjectMapper().readValue(attribute.getAdditionalInformation(),
					new TypeReference<Map<String, String>>() {
					});

			UserAttributes userAttributes = DBInfoStorage.getField("username", attributesToValues.get(SUBJECT_ID),
					UserAttributes.class);

			System.out.println("Retrieved the following user attributes: " + userAttributes.toString());
			return new ObjectMapper().writeValueAsString(userAttributes);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;

	}


	// to fix
	public void subscribe(RequestType request, String attributeId) {
	}


	/**
	 * Performs the subscribe operation. This operation is very similar to the
	 * retrieve operation. The only difference is that in this case we have to
	 * signal to the thread in charge of performing the polling that it has to poll
	 * a new attribute
	 *
	 * @param request IN/OUT parameter
	 */
	@Override
	public void subscribe(RequestType request) throws PIPException {
		Reject.ifNull(request);

		Attribute attribute = getAttributes().get(0);
		addAdditionalInformation(request, attribute);

		String value = subscribe(attribute);

		request.addAttribute(attribute, value);
	}

	/**
	 * This is the function called by the context handler whenever we have a remote
	 * retrieve request
	 */
	@Override
	public String subscribe(Attribute attribute) throws PIPException {
		Reject.ifNull(attribute);

		String value = retrieve(attribute);
		DataType dataType = attribute.getDataType();
		attribute.setValue(dataType, value);
		addSubscription(attribute);

		return value;

	}

	/**
	 * Checks if it has to remove an attribute (the one passed in the list) from the
	 * list of subscribed attributes
	 *
	 * @param attributes the list of attributes that must be unsubscribed
	 */
	@Override
	public boolean unsubscribe(List<Attribute> attributes) throws PIPException {
		Reject.ifEmpty(attributes);
		for (Attribute attribute : attributes) {
			if (attribute.getAttributeId().equals(getAttributeIds().get(0))) {
				for (Attribute subscribedAttribute : subscriptions) {
					if (subscribedAttribute.getCategory() == Category.ENVIRONMENT || subscribedAttribute
							.getAdditionalInformation().equals(attribute.getAdditionalInformation())) {
						return removeAttribute(subscribedAttribute);
					}
				}
			}
		}
		return false;
	}

	private boolean removeAttribute(Attribute subscribedAttribute) {
		if (!subscriptions.remove(subscribedAttribute)) {
			throw new IllegalStateException("Unable to remove attribute from list");
		}
		return true;
	}

	private void addAdditionalInformation(RequestType request, Attribute attribute) {

		List<AttributesType> attrstype = request.getAttributes().stream()
				.filter(a -> a.getCategory().equals(attribute.getCategory().toString())).collect(Collectors.toList());
		List<String> attrIds = attrstype.stream().flatMap(a -> a.getAttribute().stream().map(b -> b.getAttributeId()))
				.collect(Collectors.toList());
		List<String> attrValues = attrstype.stream()
				.flatMap(a -> a.getAttribute().stream()
						.flatMap(b -> b.getAttributeValue().stream().map(c -> c.getContent().get(0).toString())))
				.collect(Collectors.toList());
		Map<String, String> idsToValues = new HashMap<String, String>();
		idsToValues = IntStream.range(0, attrIds.size()).boxed()
				.collect(Collectors.toMap(attrIds::get, attrValues::get));

		String filters = null;
		try {
			filters = new ObjectMapper().writeValueAsString(idsToValues);
			System.out.println("filters = " + filters);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		attribute.setAdditionalInformation(filters);
	}

	public boolean isEnvironmentCategory(Attribute attribute) {
		return attribute.getCategory() == Category.ENVIRONMENT;
	}

	private String formatJournaling(String... strings) {
		StringBuilder logStringBuilder = new StringBuilder();
		logStringBuilder.append("VALUE READ: " + strings[0]);

		if (strings.length > 1) {
			logStringBuilder.append(" FOR FILTER: " + strings[1]);
		}
		return logStringBuilder.toString();
	}

	@Override
	public void update(String data) throws PIPException {
//		try {
//			Path path = Paths.get(filePath);
//			Files.write(path, data.getBytes());
//		} catch (IOException e) {
//			log.severe("Error updating attribute : " + e.getMessage());
//		}
	}

	@Override
	public void retrieve(RequestType request, List<Attribute> attributeRetrievals) {
		log.severe("Multiple retrieve is unimplemented");
	}

	@Override
	public void subscribe(RequestType request, List<Attribute> attributeRetrieval) {
		log.severe("Multiple subscribe is unimplemented");
	}

	@Override
	public void performObligation(ObligationInterface obligation) {
		log.severe("Perform obligation is unimplemented");
	}

	public void addSubscription(Attribute attribute) {
		if (!subscriptions.contains(attribute)) {
			subscriptions.add(attribute);
		}
	}

	public void checkSubscriptions() {
		for (Attribute attribute : subscriptions) {
			String value = "";
			log.log(Level.INFO, "Polling on value of the attribute " + attribute.getAttributeId() + " for change.");

			try {
				value = retrieve(attribute);
			} catch (PIPException e) {
				log.log(Level.WARNING, "Error reading attribute " + attribute.getAttributeId());
				return;
			}

			String oldValue = attribute.getAttributeValues(attribute.getDataType()).get(0);
			if (!oldValue.equals(value)) { // if the attribute has changed
				log.log(Level.INFO, "Attribute {0}={1}:{2} changed at {3}", new Object[] { attribute.getAttributeId(),
						value, attribute.getAdditionalInformation(), System.currentTimeMillis() });
				attribute.setValue(attribute.getDataType(), value);
				notifyRequestManager(attribute);
			}
		}
	}

	public void notifyRequestManager(Attribute attribute) {
		AttributeChangeMessage attrChangeMessage = new AttributeChangeMessage(ENTITIES.PIP.toString(),
				ENTITIES.CH.toString());
		ArrayList<Attribute> attrList = new ArrayList<>(Arrays.asList(attribute));
		attrChangeMessage.setAttributes(attrList);
		getRequestManager().sendMessage(attrChangeMessage);
	}
}
