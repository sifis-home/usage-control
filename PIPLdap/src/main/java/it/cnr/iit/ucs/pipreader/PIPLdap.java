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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.NamingException;
import javax.xml.bind.JAXBException;

import it.cnr.iit.ucs.exceptions.PIPException;
import it.cnr.iit.ucs.obligationmanager.ObligationInterface;
import it.cnr.iit.ucs.pip.PIPBase;
import it.cnr.iit.utility.JAXBUtility;
import it.cnr.iit.xacml.Attribute;
import it.cnr.iit.xacml.Category;
import it.cnr.iit.xacml.DataType;
import oasis.names.tc.xacml.core.schema.wd_17.RequestType;

/**
 * PIP LDAP implementation: it performs PIP operations by retrieving and
 * updating user information stored in an LDAP server
 *
 * @author Fabio Bindi and Filippo Lauria
 */
public class PIPLdap extends PIPBase {

	private final static Logger LOGGER = Logger.getLogger(PIPLdap.class.getName());

	protected final BlockingQueue<Attribute> subscriptions = new LinkedBlockingQueue<>();
	private SubscriberTimer subscriberTimer;
	private volatile boolean initialized = false;
	private Category expectedCategory;
	private LDAPConnector ldapConnector = null;
	private Timer timer = new Timer();

	public PIPLdap(String xmlPip) {
		super(xmlPip);
		if (!isInitialized()) {
			return;
		}
		if (initialize(xmlPip)) {
			initialized = true;
			subscriberTimer = new SubscriberTimer(contextHandlerInterface, subscriptions, ldapConnector,
					getAttributeIds());
			timer.scheduleAtFixedRate(subscriberTimer, 0, 10 * 1000);
		} else {
			return;
		}
	}

	private boolean initialize(String xmlPIP) {
		try {
			XMLPip xmlPip = JAXBUtility.unmarshalToObject(XMLPip.class, xmlPIP);
			List<XMLAttribute> attributes = xmlPip.getAttributes();
			for (XMLAttribute xmlAttribute : attributes) {
				Map<String, String> arguments = xmlAttribute.getArgs();
				Attribute attribute = new Attribute();
				if (!attribute.createAttributeId(arguments.get(ATTRIBUTE_ID))) {
					LOGGER.log(Level.SEVERE, "[PIPReader] wrong set Attribute");
					return false;
				}
				if (!attribute.setCategory(Category.toCATEGORY(arguments.get(CATEGORY)))) {
					LOGGER.log(Level.SEVERE, "[PIPReader] wrong set category " + arguments.get(CATEGORY));
					return false;
				}
				if (!attribute.setAttributeDataType(DataType.toDATATYPE(arguments.get(DATA_TYPE)))) {
					LOGGER.log(Level.SEVERE, "[PIPReader] wrong set datatype");
					return false;
				}
				if (attribute.getCategory() != Category.ENVIRONMENT) {
					if (!setExpectedCategory(arguments.get(EXPECTED_CATEGORY))) {
						return false;
					}
				}
				if (ldapConnector == null) {
					if (setLdapConnector(arguments.get("HOST"), arguments.get("BNDDN"),
							arguments.get("PASSWORD")) == -1) {
						return false;
					}
				}
				addAttribute(attribute);
			}
			return true;
		} catch (JAXBException e) {
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * Sets up the conection with LDAP
	 *
	 * @param host     the host
	 * @param bnddn
	 * @param password the password
	 */
	private int setLdapConnector(String host, String bnddn, String password) {
		// BEGIN parameter checking
		if (host == null || bnddn == null || password == null) {
			return 0;
		}
		// END parameter checking
		else {
			ldapConnector = new LDAPConnector(host, bnddn);
			try {
				ldapConnector.authenticate(password);
				return 1;
			} catch (NamingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return -1;
			}
		}
	}

	final private boolean setExpectedCategory(String category) {
		// BEGIN parameter checking
		if (!isInitialized() || category == null || category.isEmpty()) {
			initialized = false;
			return false;
		}
		// END parameter checking
		Category categoryObj = Category.toCATEGORY(category);
		if (categoryObj == null) {
			initialized = false;
			return false;
		}
		expectedCategory = categoryObj;
		return true;
	}

	/**
	 * Subscribes an user to monitor its mutable attributes by storing in a map his
	 * search base (used to access his information in the LDAP server) and the list
	 * of his mutable attributes (retrieved from the configuration file)
	 *
	 * @param request a XACML file where the user to subscribe will be retrieved
	 * @throws PIPException
	 */
	@Override
	public synchronized void subscribe(RequestType accessRequest) throws PIPException {
		// BEGIN parameter checking
		if (accessRequest == null || !initialized || !isInitialized()) {
			LOGGER.log(Level.SEVERE, "[PIPREader] wrong initialization" + initialized + "\t" + isInitialized());
			return;
		}
		// END parameter checking

		subscriberTimer.setContextHandlerInterface(contextHandlerInterface);

		if (subscriberTimer.getContextHandler() == null || contextHandlerInterface == null) {
			LOGGER.log(Level.SEVERE, "Context handler not set");
			return;
		}

		List<String> searchAttributesList = getAttributeIds();
		List<String> attributeNames = retrieveAttributeName(searchAttributesList);

		Map<String, Set<String>> ldapMap;
		String filter;
		if (EXPECTED_CATEGORY == null) {
			return;
		} else {
			filter = accessRequest.getAttributeValue(expectedCategory);
			ldapMap = ldapConnector.search("dc=c3isp,dc=eu", attributeNames, filter);
		}

		for (Map.Entry<String, Set<String>> entry : ldapMap.entrySet()) {
			String label = entry.getKey();
			Iterator<String> it = entry.getValue().iterator();
			for (Attribute value : getAttributes()) {
				if (new AttributeId(value.getAttributeId()).getSplittedAttribute().equalsIgnoreCase(label)) {
					while (it.hasNext()) {
						String ldapValue = it.next();
						value.setAttributeValues(value.getDataType(), ldapValue);
					}
					for (Map.Entry<String, List<String>> attributeEntry : value.getAttributeValueMap().entrySet()) {
						accessRequest.addAttribute(value.getCategory().toString(), entry.getKey(),
								value.getAttributeId(), attributeEntry.getValue());

					}
				}
				if (!subscriptions.contains(value)) {
					subscriptions.add(value);

				}
			}
		}

		// add the attribute to the access request

		// add the attribute to the subscription list

	}

	/**
	 * Retrieves the attribute values for a certain user from the LDAP server. It
	 * needs autenthication to access to the LDAP server.
	 *
	 * @param request a XACML file where the user will be retrieved
	 * @throws PIPException
	 */
	@Override
	public void retrieve(RequestType accessRequest) throws PIPException {
		ArrayList<String> pilotList = ldapConnector.getPilots();
		String userIdentifier = accessRequest.getAttributeValue(expectedCategory);

		String pilotName = ldapConnector.getPilotName(pilotList, userIdentifier);
		String topPilotName = ldapConnector.getTopPilotName(pilotList, userIdentifier);

		System.out.println("Identity: " + userIdentifier);
		System.out.println("Pilot Name: " + pilotName);
		System.out.println("Top Pilot Name: " + topPilotName);

		String schema = null;
		if (pilotName != null) {
			schema = ldapConnector.getSchemaForUser(pilotName, userIdentifier);
		}
		System.out.println("Schema: " + schema);

		List<Attribute> attributes = retrieveUserAttributesFromLdap(schema, userIdentifier, pilotName, topPilotName);
		for (Attribute attribute : attributes) {

			LOGGER.log(Level.INFO, "[PIPLdap] AttributeId: " + attribute.getAttributeId() + " for " + userIdentifier
					+ " Value:" + attribute.getAttributeValues(DataType.STRING));

			accessRequest.addAttribute(attribute.getCategory().toString(), DataType.STRING.toString(),
					attribute.getAttributeId(), attribute.getAttributeValues(DataType.STRING));
		}
		return;
	}

	private List<Attribute> retrieveUserAttributesFromLdap(String schema, String userIdentifier, String pilotName,
			String topPilotName) {
		List<Attribute> attributesList = new ArrayList<>();
		ArrayList<String> membership = new ArrayList<>();
		ArrayList<String> organization = new ArrayList<>();
		ArrayList<String> country = new ArrayList<>();

		if (schema != null) {
			Map<String, Set<String>> attributes = getAttributes(extractValueFromSchema(schema, "cn="), userIdentifier,
					pilotName, topPilotName);
			membership = getMembership(attributes);
			organization = getOrganization(attributes);
			country = getCountry(attributes);
		} else {
			ArrayList<String> nullString = new ArrayList<>();
			nullString.add("");
			membership = nullString;
			organization = nullString;
			country = nullString;
		}

		System.out.println("Role: " + membership);
		System.out.println("Organization: " + organization);
		System.out.println("country: " + country);

		for (String attributeId : getAttributeIds()) {
			if (attributeId.contains("role") || attributeId.contains("ismemberof")) {
				Attribute attribute = new Attribute();
				attribute.setAttributeId(attributeId);
				attribute.setDataType(DataType.STRING);
				attribute.setCategory(Category.SUBJECT);
				HashMap<String, ArrayList<String>> tmp = new HashMap<String, ArrayList<String>>();
				tmp.put(DataType.STRING.toString(), membership);
				attribute.setAttributevalueMap(tmp);
				attributesList.add(attribute);
			}
			if (attributeId.contains("organisation")) {
				Attribute attribute = new Attribute();
				attribute.setAttributeId(attributeId);
				attribute.setDataType(DataType.STRING);
				attribute.setCategory(Category.SUBJECT);
				HashMap<String, ArrayList<String>> tmp = new HashMap<String, ArrayList<String>>();
				tmp.put(DataType.STRING.toString(), organization);
				attribute.setAttributevalueMap(tmp);
				attributesList.add(attribute);
			}
			if (attributeId.contains("country")) {
				Attribute attribute = new Attribute();
				attribute.setAttributeId(attributeId);
				attribute.setDataType(DataType.STRING);
				attribute.setCategory(Category.SUBJECT);
				HashMap<String, ArrayList<String>> tmp = new HashMap<String, ArrayList<String>>();
				tmp.put(DataType.STRING.toString(), country);
				attribute.setAttributevalueMap(tmp);
				attributesList.add(attribute);
			}
		}
		return attributesList;

	}

	private String extractValueFromSchema(String schema, String value) {
		return schema.split(value)[1].split(",")[0];
	}

	private Map<String, Set<String>> getAttributes(String user, String userIdentifier, String pilotName,
			String topPilotName) {
		Map<String, Set<String>> attributes = null;
		// It tries to extract the role and the organization with the standard LDAP
		// structure
		String cnName = ldapConnector.getCnInSubpilot(userIdentifier, pilotName, topPilotName);
		attributes = ldapConnector.roleInSubPilot(user, pilotName, topPilotName);
		attributes.put("organization", (ldapConnector.companySubpilot(userIdentifier, pilotName)));
		attributes.put("country", (ldapConnector.getCountry(cnName)));
		return attributes;
	}

	private ArrayList<String> getMembership(Map<String, Set<String>> attributes) {
		ArrayList<String> values = new ArrayList<>();
		if (attributes.containsKey("role")) {
			values.addAll(attributes.get("role"));
		} else {
			values.add("empty");
		}

		return values;
	}

	private ArrayList<String> getCountry(Map<String, Set<String>> attributes) {
		ArrayList<String> values = new ArrayList<>();
		if (attributes.containsKey("country")) {
			values.addAll(attributes.get("country"));
		} else {
			values.add("empty");
		}
		return values;
	}

	private ArrayList<String> getOrganization(Map<String, Set<String>> attributes) {
		ArrayList<String> values = new ArrayList<>();
		if (attributes.containsKey("organization")) {
			values.addAll(attributes.get("organization"));
		} else {
			values.add("empty");
		}
		return values;
	}

	private List<String> retrieveAttributeName(List<String> searchAttributesList) {
		List<String> attributeNames = new LinkedList<>();

		for (String string : searchAttributesList) {
			AttributeId attributeId = new AttributeId(string);
			attributeNames.add(attributeId.getSplittedAttribute());
		}

		// attributeNames.add("mail");
		return attributeNames;
	}

	@Override
	public boolean unsubscribe(List<Attribute> attributes) throws PIPException {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public String retrieve(Attribute attributeRetrievals) throws PIPException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String subscribe(Attribute attributeRetrieval) throws PIPException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void retrieve(RequestType request, List<Attribute> attributeRetrievals) {
		// TODO Auto-generated method stub

	}

	@Override
	public void subscribe(RequestType request, List<Attribute> attributeRetrieval) {
		// TODO Auto-generated method stub

	}

	@Override
	public void performObligation(ObligationInterface obligation) {
		// TODO Auto-generated method stub

	}

	@Override
	public void update(String json) throws PIPException {
		// TODO Auto-generated method stub

	}
}
