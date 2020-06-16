package it.cnr.iit.ucs.pipldap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.directory.api.ldap.model.message.SearchScope;

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
import it.cnr.iit.ucs.pipldap.statics.LdapAuthorization;
import it.cnr.iit.ucs.properties.components.PipProperties;
import it.cnr.iit.utility.errorhandling.Reject;
import it.cnr.iit.xacml.Attribute;
import it.cnr.iit.xacml.Category;
import it.cnr.iit.xacml.DataType;
import oasis.names.tc.xacml.core.schema.wd_17.AttributesType;
import oasis.names.tc.xacml.core.schema.wd_17.RequestType;

/**
 * This is a PIPDsa.
 * <p>
 * The PIPDsa is very C3ISP specific since it is the PIP in charge of verifying
 * the characteristics of DSA:
 * <ol>
 * <li>The version of the DSA</li>
 * <li>The status of the DSA</li>
 * <li>Temporarily there will be also the part related to CoCoCloud in which we
 * used to have RuntimeAttestation</li>
 * </ol>
 * All the informations related to these attributes come from the configuration
 * of this PIP. The PIP expects to have the DSA_ID (conversely the PolicyID as
 * an attribute of the resource that is requested). <b>This attributeID has
 * multiple values</b>
 * </p>
 * 
 * @author antonio
 *
 */
public final class PIPLdap extends PIPBase {

	// ---------------------------------------------------------------------------
	// Class attributes
	// ---------------------------------------------------------------------------
	/**
	 * Whenever a PIP has to retrieve some informations related to an attribute that
	 * is stored inside the request, it has to know in advance all the informations
	 * to retrieve that atrtribute. E.g. if this PIP has to retrieve the
	 * informations about the subject, it has to know in advance which is the
	 * attribute id qualifying the subject, its category and the datatype used,
	 * otherwise it is not able to retrieve the value of that attribute, hence it
	 * would not be able to communicate with the AM properly
	 */
	private static Logger log = Logger.getLogger(PIPLdap.class.getName());
	private JournalingInterface journal;

	// list that stores the attributes on which a subscribe has been performed
	protected final BlockingQueue<Attribute> subscriptions = new LinkedBlockingQueue<>();

	/**
	 * Whenever a PIP has to retrieve some informations related to an attribute that
	 * is stored inside the request, it has to know in advance all the informations
	 * to retrieve that attribute. E.g. if this PIP has to retrieve the informations
	 * about the subject, it has to know in advance which is the attribute id
	 * qualifying the subject, its category and the data-type used, otherwise it is
	 * not able to retrieve the value of that attribute, hence it would not be able
	 * to communicate with the AM properly
	 */
	private Category expectedCategory;

	public static final String LDAP_HOST = "host";
	public static final String LDAP_PORT = "port";
	public static final String LDAP_BNDDN = "bnddn";
	public static final String LDAP_PASS = "password";
	public static final String ORG_LIST = "org-list";

	private static final String SUBJECT_ID = "urn:oasis:names:tc:xacml:3.0:subject:subject-id";
	private static final String SUBJECT_ORGANIZATION = "urn:oasis:names:tc:xacml:3.0:subject:subject-organisation";
	private static final String SUBJECT_COUNTRY = "urn:oasis:names:tc:xacml:3.0:subject:subject-country";
	private final static String SUBJECT_MEMBEROF = "urn:oasis:names:tc:xacml:3.0:subject:subject-ismemberof";

	private final static String RESOURCE_OWNER = "urn:oasis:names:tc:xacml:3.0:resource:resource-owner";

	private List<String> orgList = new ArrayList<String>();

	private static volatile boolean initialized = false;

	public PIPLdap(PipProperties properties) {
		super(properties);
		Reject.ifFalse(init(properties), "Error initializing pip : " + properties.getId());
	}

	private boolean init(PipProperties properties) {
		if (initialized == true)
			return false;

		initialized = true;

		try {
			log.severe("Initializing PIPLdap...");
			List<Map<String, String>> pipProperties = properties.getAttributes();
			setAuthorizations(properties);
			setOrgList(properties);
			pipProperties.stream().forEach(pip -> addAttributes(pip));
			journal = JournalBuilder.build(properties);
			PIPLdapSubscriberTimer subscriberTimer = new PIPLdapSubscriberTimer(this);
			subscriberTimer.start();
			LdapQuery.init();
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	private void setAuthorizations(PipProperties properties) {
		Reject.ifFalse(properties.getAdditionalProperties().containsKey(LDAP_HOST), "missing ldap host");
		LdapAuthorization.setHost(properties.getAdditionalProperties().get(LDAP_HOST));
		Reject.ifFalse(properties.getAdditionalProperties().containsKey(LDAP_PORT), "missing ldap port");
		LdapAuthorization.setPort(properties.getAdditionalProperties().get(LDAP_PORT));
		Reject.ifFalse(properties.getAdditionalProperties().containsKey(LDAP_BNDDN), "missing bnddn");
		LdapAuthorization.setBnddn(properties.getAdditionalProperties().get(LDAP_BNDDN));
		Reject.ifFalse(properties.getAdditionalProperties().containsKey(LDAP_PASS), "missing bnddn password");
		LdapAuthorization.setPassword(properties.getAdditionalProperties().get(LDAP_PASS));
	}

	private void setOrgList(PipProperties properties) {
		Reject.ifFalse(properties.getAdditionalProperties().containsKey(ORG_LIST), "missing organization list");
		orgList = Arrays.asList(properties.getAdditionalProperties().get(ORG_LIST).split(","));
		orgList.stream().forEach(org -> log.severe("orgList: " + org));
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

	@Override
	public void retrieve(RequestType request) {
		Reject.ifNull(request);

		List<Attribute> attributeList = getAttributes();
		addAdditionalInformation(request, attributeList.get(0));

		try {

			Map<String, String> attributesToValues = new HashMap<>();
			attributesToValues = new ObjectMapper().readValue(attributeList.get(0).getAdditionalInformations(),
					new TypeReference<Map<String, String>>() {
					});

			log.severe("attributesToValues = " + new ObjectMapper().writeValueAsString(attributesToValues));

			String uid = attributesToValues.get(SUBJECT_ID);
			String organization = attributesToValues.get(RESOURCE_OWNER).toLowerCase().replace(" ", "");
			orgList = orgList.stream().filter(org -> org.equals(organization)).collect(Collectors.toList());

			if (!orgList.isEmpty()) {
				Map<String, String> ldapAttributes = new ObjectMapper().readValue(retrieve(getAttributes().get(0)),
						new TypeReference<Map<String, String>>() {
						});

				log.severe("specific ldap user attributes: " + new ObjectMapper().writeValueAsString(ldapAttributes));
				Map<String, String> userAttributes = mapLdapAttributesToOasis(ldapAttributes);
				log.severe("specific oasis attributes to fatten the request: "
						+ new ObjectMapper().writeValueAsString(userAttributes));
				addAttributesToRequest(request, userAttributes);

			} else {
				throw new PIPException("The organization " + organization + " is not monitored by the PIPLdap");
			}

		} catch (PIPException e) {
			log.severe(e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private Map<String, String> mapLdapAttributesToOasis(Map<String, String> userAttributes) {
		Map<String, String> oasisMap = new HashMap<String, String>();

		for (String attr : userAttributes.keySet()) {
			if (!userAttributes.containsKey(attr))
				continue;

			switch (attr) {
			case "o":
				oasisMap.put(SUBJECT_ORGANIZATION, userAttributes.get(attr));
				break;
			case "c":
				oasisMap.put(SUBJECT_COUNTRY, userAttributes.get(attr));
				break;
			case "memberof":
				oasisMap.put(SUBJECT_MEMBEROF, userAttributes.get(attr));
				break;
			}
		}

		return oasisMap;
	}

	private void addAttributesToRequest(RequestType request, Map<String, String> userAttributes) {

		if (userAttributes.containsKey(SUBJECT_ORGANIZATION)) {
			request.addAttribute(Category.SUBJECT.toString(), DataType.STRING.toString(), SUBJECT_ORGANIZATION,
					userAttributes.get(SUBJECT_ORGANIZATION));
		}
		if (userAttributes.containsKey(SUBJECT_COUNTRY)) {
			request.addAttribute(Category.SUBJECT.toString(), DataType.STRING.toString(), SUBJECT_COUNTRY,
					userAttributes.get(SUBJECT_COUNTRY));
		}
		if (userAttributes.containsKey(SUBJECT_MEMBEROF)) {
			request.addAttribute(Category.SUBJECT.toString(), DataType.STRING.toString(), SUBJECT_MEMBEROF,
					userAttributes.get(SUBJECT_MEMBEROF));
		}

	}

	/**
	 * This is the function called by the context handler whenever we have a remote
	 * retrieve request
	 */
	@Override
	public String retrieve(Attribute attribute) throws PIPException {
		String[] attrsToSearch;

		String organization = orgList.get(0);
		String memberof = new String();
		String uid = "andreachino";
		String filter = "(objectClass=*)";
		String baseDn = new String();
		SearchScope level = SearchScope.ONELEVEL;

		switch (organization) {
		case "chino":
		case "gps":
		case "3drepo":
		case "kent": {
			Map<String, Map<String, String>> uidsToLdapAttributes = getUidsToLdapAttributesMap(organization);
			uid = getUid(uidsToLdapAttributes);
			memberof = getMemberOf(uid);
			baseDn = "ou=SME Pilot,ou=SME,ou=Pilots,dc=c3isp,dc=eu";
			String[] attrs = { "uid", "o", "cn" };
			attrsToSearch = attrs;
			break;
		}
		case "spartacompany1":
		case "spartacompany2":
		case "spartacompany3": {
			baseDn = "ou=Users,ou=SPARTA,ou=Pilots,dc=c3isp,dc=eu";
			String[] attrs = { "uid", "o", "cn", "c" };
			attrsToSearch = attrs;
			break;
		}
		default: {
			baseDn = "ou=Users,dc=c3isp,dc=eu";
			String[] attrs = { "uid", "o", "cn", "c" };
			attrsToSearch = attrs;
			break;
		}
		}

//		log.severe("filter for ldap: " + baseDn);
//
		Map<String, Map<String, String>> queriedUsersAttributes = LdapQuery.queryForAll(baseDn, filter, level,
				attrsToSearch);

		try {
			log.severe("userAttributes after query = " + new ObjectMapper().writeValueAsString(queriedUsersAttributes));
			queriedUsersAttributes = filterForOrganization(queriedUsersAttributes, organization);
			Map<String, String> userAttributes = queriedUsersAttributes.get(uid);
			if (!memberof.isEmpty()) {
				userAttributes.put("memberof", memberof);
			}
			log.severe("userAttributes after filtering = " + new ObjectMapper().writeValueAsString(userAttributes));
			return new ObjectMapper().writeValueAsString(userAttributes);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}

		return new String();

	}

	private Map<String, Map<String, String>> filterForOrganization(Map<String, Map<String, String>> map,
			String organization) {
		return map.entrySet().stream()
				.filter(uid -> uid.getValue().get("o").toLowerCase().replace(" ", "").equals(organization))
				.collect(Collectors.toMap(Entry::getKey, Entry::getValue));
	}

	private Map<String, Map<String, String>> getUidsToLdapAttributesMap(String organization) {
		String[] attrsToSearchUser = { "uid", "o", "cn" };
		String baseDn = "ou=SME Pilot,ou=SME,ou=Pilots,dc=c3isp,dc=eu";
		SearchScope level = SearchScope.ONELEVEL;
		String filter = "(objectclass=*)";
		Map<String, Map<String, String>> userAttributes = LdapQuery.queryForAll(baseDn, filter, level,
				attrsToSearchUser);
		userAttributes = filterForOrganization(userAttributes, organization);
		return userAttributes;
	}

	private String getUid(Map<String, Map<String, String>> uidsToLdapAttributes) {
		try {

			Map<String, String> attributesToValues = new HashMap<>();
			attributesToValues = new ObjectMapper().readValue(getAttributes().get(0).getAdditionalInformations(),
					new TypeReference<Map<String, String>>() {
					});
			String subjectId = attributesToValues.get(SUBJECT_ID);
			String uid = uidsToLdapAttributes.entrySet().stream().filter(el -> el.getKey().equals(subjectId))
					.findFirst().orElseThrow(() -> new PIPException("Impossible to retrieve the uid for user "
							+ subjectId + " because is different from the subject id"))
					.getKey();
			return uid;
		} catch (IOException e) {
			e.printStackTrace();
		} catch (PIPException e) {
			log.severe(e.getMessage());
		}

		return new String();
	}

	private String getMemberOf(String uid) {
		log.severe("uid in getMemberOf=" + uid);
		String[] attrsToSearchUser = { "cn" };
		String filterDn = "ou=SME Pilot,ou=SME,ou=Pilots,dc=c3isp,dc=eu";
		String baseDn = "ou=SME,ou=Pilots,dc=c3isp,dc=eu";
		SearchScope level = SearchScope.ONELEVEL;
		String filter = "(&(objectclass=groupOfUniqueNames)(uniqueMember=cn=" + uid + "," + filterDn + "))";
		String memberOf = LdapQuery.queryForMemberOf(baseDn, filter, level, attrsToSearchUser);
		return memberOf;
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

		Attribute attribute = getAttributes().get(1);

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
							.getAdditionalInformations().equals(attribute.getAdditionalInformations())) {
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

	private Map<String, String> getAttributesFromCategory(RequestType request, String category) {

		List<AttributesType> attrstype = request.getAttributes().stream().filter(a -> a.getCategory().equals(category))
				.collect(Collectors.toList());
		List<String> attrIds = attrstype.stream().flatMap(a -> a.getAttribute().stream().map(b -> b.getAttributeId()))
				.collect(Collectors.toList());
		List<String> attrValues = attrstype.stream()
				.flatMap(a -> a.getAttribute().stream()
						.flatMap(b -> b.getAttributeValue().stream().map(c -> c.getContent().get(0).toString())))
				.collect(Collectors.toList());
		Map<String, String> idsToValues = new HashMap<String, String>();
		idsToValues = IntStream.range(0, attrIds.size()).boxed()
				.collect(Collectors.toMap(attrIds::get, attrValues::get));

		return idsToValues;

	}

	private void addAdditionalInformation(RequestType request, Attribute attribute) {

//		Map<String, String> idsToValuesSubject = getAttributesFromCategory(request, Category.SUBJECT.toString());
		Map<String, String> idsToValuesSubject = new HashMap<String, String>();
		idsToValuesSubject.put(Category.SUBJECT.toString(), "aarighi");
		Map<String, String> idsToValuesResource = getAttributesFromCategory(request, Category.RESOURCE.toString());

		Map<String, String> idsToValues = new HashMap<String, String>();
		idsToValues.putAll(idsToValuesSubject);
		idsToValues.putAll(idsToValuesResource);

		String filters = null;
		try {
			filters = new ObjectMapper().writeValueAsString(idsToValues);
			log.severe("filters = " + filters);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		attribute.setAdditionalInformations(filters);

	}

	public boolean isEnvironmentCategory(Attribute attribute) {
		return attribute.getCategory() == Category.ENVIRONMENT;
	}

//	journal.logString(formatJournaling(value, filter));

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
		// TODO
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
						value, attribute.getAdditionalInformations(), System.currentTimeMillis() });
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
