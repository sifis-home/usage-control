package it.cnr.iit.ucs.pipldap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.directory.api.ldap.model.message.SearchScope;

import it.cnr.iit.common.attributes.AttributeIds;
import it.cnr.iit.common.lambda.exceptions.ConsumerException;
import it.cnr.iit.ucs.constants.ENTITIES;
import it.cnr.iit.ucs.db.DBInfoStorage;
import it.cnr.iit.ucs.db.UserAttributes;
import it.cnr.iit.ucs.exceptions.PIPException;
import it.cnr.iit.ucs.journaling.JournalBuilder;
import it.cnr.iit.ucs.journaling.JournalingInterface;
import it.cnr.iit.ucs.message.attributechange.AttributeChangeMessage;
import it.cnr.iit.ucs.obligationmanager.ObligationInterface;
import it.cnr.iit.ucs.pip.PIPBase;
import it.cnr.iit.ucs.pip.PIPKeywords;
import it.cnr.iit.ucs.pipldap.statics.CachedResult;
import it.cnr.iit.ucs.pipldap.statics.LdapAuthorization;
import it.cnr.iit.ucs.properties.components.PipProperties;
import it.cnr.iit.utility.errorhandling.Reject;
import it.cnr.iit.xacml.Attribute;
import it.cnr.iit.xacml.Category;
import it.cnr.iit.xacml.DataType;
import oasis.names.tc.xacml.core.schema.wd_17.AttributeType;
import oasis.names.tc.xacml.core.schema.wd_17.AttributeValueType;
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
	public static final String DB_URI = "db-uri";

	private List<String> orgList = new ArrayList<String>();

	public PIPLdap(PipProperties properties) {
		super(properties);
		log.setLevel(Level.SEVERE);
		Reject.ifFalse(init(properties), "Error initializing pip : " + properties.getId());
	}

	private boolean init(PipProperties properties) {
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
			e.printStackTrace();
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
		Reject.ifFalse(properties.getAdditionalProperties().containsKey(DB_URI), "missing database URI");
		DBInfoStorage.start(properties.getAdditionalProperties().get(DB_URI));
	}

	private void setOrgList(PipProperties properties) {
		Reject.ifFalse(properties.getAdditionalProperties().containsKey(ORG_LIST), "missing organization list");
		orgList = Arrays.asList(properties.getAdditionalProperties().get(ORG_LIST).split(","));
		orgList.stream().forEach(org -> log.info("orgList: " + org));
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

		try {
			String subjectId = request.getAttribute(Category.SUBJECT.toString(), AttributeIds.SUBJECT_ID);
			UserAttributes userAttributes = Optional
					.ofNullable(DBInfoStorage.getField("username", subjectId, UserAttributes.class)).orElse(null);

			if (userAttributes != null) {
				throw new PIPException("Cannot monitor LDAP attributes for user " + subjectId);
			}

			List<Attribute> attrToRetrieve = new ArrayList<Attribute>();
			attrToRetrieve.add(findAttributeById(AttributeIds.SUBJECT_ORGANIZATION));
			attrToRetrieve.add(findAttributeById(AttributeIds.SUBJECT_ROLE));
			attrToRetrieve.add(findAttributeById(AttributeIds.SUBJECT_ISMEMBEROF));
			attrToRetrieve.add(findAttributeById(AttributeIds.SUBJECT_COUNTRY));
			getAttributes().stream().forEach(attr -> addAdditionalInformation(attr, subjectId));
			attrToRetrieve.stream().forEach(attr -> addAdditionalInformation(attr, subjectId));

			log.info("added the following attributes:");
			attrToRetrieve.stream().forEach(attr -> log.info(attr.getAttributeId()));
			attrToRetrieve.stream()
					.forEach(ConsumerException.unchecked(attr -> request.addAttribute(attr, retrieve(attr))));

			removeEmptyAttributes(request);
			CachedResult.setCached(false);
			CachedResult.getAttributesToValues().clear();

		} catch (PIPException e) {
			log.severe(e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void removeEmptyAttributes(RequestType request) {
		boolean toRemove = false;
		for (AttributesType attrsType : request.getAttributes()) {
			for (AttributeType attrType : attrsType.getAttribute()) {
				for (AttributeValueType values : attrType.getAttributeValue()) {
					if (values.getContent().get(0) == null) {
						attrType.getAttributeValue().remove(values);
						toRemove = true;
						break;
					}
				}
				if (toRemove) {
					attrsType.getAttribute().remove(attrType);
					break;
				}
			}
			if (toRemove)
				break;
		}
		if (toRemove)
			removeEmptyAttributes(request);
	}

	private Map<String, String> mapLdapAttributesToOasis(Map<String, String> userAttributes) {
		Map<String, String> oasisMap = new HashMap<String, String>();

		for (String attr : userAttributes.keySet()) {
			if (!userAttributes.containsKey(attr))
				continue;

			switch (attr) {
			case "o":
				oasisMap.put(AttributeIds.SUBJECT_ORGANIZATION, userAttributes.get(attr));
				break;
			case "c":
				oasisMap.put(AttributeIds.SUBJECT_COUNTRY, userAttributes.get(attr));
				break;
			case "memberof":
				oasisMap.put(AttributeIds.SUBJECT_ISMEMBEROF, userAttributes.get(attr));
				break;
			}
		}

		return oasisMap;
	}

	/**
	 * This is the function called by the context handler whenever we have a remote
	 * retrieve request
	 */
	@Override
	public String retrieve(Attribute attribute) throws PIPException {

		if (CachedResult.isCached()) {
			return CachedResult.getAttributesToValues().get(attribute.getAttributeId());
		}

		String subjectId = attribute.getAdditionalInformations();
		Map<String, String> userAttrMap = LdapQuery.getAttributesByUsername(subjectId, "ou=Pilots,dc=c3isp,dc=eu",
				"(objectClass=*)", "uid", "o", "cn", "c");

		String memberOf = getMemberOf(userAttrMap.get("uid"));
		userAttrMap.put("memberof", memberOf);
		userAttrMap.entrySet().stream()
				.forEach(attr -> log.info("key: " + attr.getKey() + ", value=" + attr.getValue()));
		userAttrMap.values().removeIf(el -> el.equals(LdapQuery.NOT_FOUND));
		userAttrMap.entrySet().stream()
				.forEach(attr -> log.info("key: " + attr.getKey() + ", value=" + attr.getValue()));

		CachedResult.setAttributesToValues(mapLdapAttributesToOasis(userAttrMap));
		CachedResult.setCached(true);

		String retrieved = CachedResult.getAttributesToValues().get(attribute.getAttributeId());
		log.info("retrieved the value " + retrieved + " from PIPLdap");
		return retrieved;
	}

	private String getMemberOf(String uid) {

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
	public void subscribe(RequestType request) {
		Reject.ifNull(request);

		try {
			String subjectId = request.getAttribute(Category.SUBJECT.toString(), AttributeIds.SUBJECT_ID);
			UserAttributes userAttributes = Optional
					.ofNullable(DBInfoStorage.getField("username", subjectId, UserAttributes.class)).orElse(null);

			if (userAttributes != null) {
				throw new PIPException("Cannot monitor LDAP attributes for user " + subjectId);
			}

			List<Attribute> attrToSubscribe = new ArrayList<Attribute>();
			attrToSubscribe.add(findAttributeById(AttributeIds.SUBJECT_ORGANIZATION));
			attrToSubscribe.add(findAttributeById(AttributeIds.SUBJECT_ROLE));
			attrToSubscribe.add(findAttributeById(AttributeIds.SUBJECT_ISMEMBEROF));
			attrToSubscribe.add(findAttributeById(AttributeIds.SUBJECT_COUNTRY));
			getAttributes().stream().forEach(attr -> addAdditionalInformation(attr, subjectId));
			attrToSubscribe.stream().forEach(attr -> addAdditionalInformation(attr, subjectId));

			log.info("subscribing the following attributes: ");
			attrToSubscribe.stream().forEach(attr -> log.info(attr.getAttributeId()));

			attrToSubscribe.stream().forEach(attr -> addAdditionalInformation(attr, subjectId));

			attrToSubscribe.stream()
					.forEach(ConsumerException.unchecked(attr -> request.addAttribute(attr, subscribe(attr))));

			removeEmptyAttributes(request);
			CachedResult.setCached(false);
			CachedResult.getAttributesToValues().clear();

		} catch (PIPException e) {
			log.severe(e.getMessage());
		}
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

	private void addAdditionalInformation(Attribute attribute, String filter) {
		System.out.println("filters from pipjdbc = " + filter);
		attribute.setAdditionalInformations(filter);
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
				log.log(Level.SEVERE, "Error reading attribute " + attribute.getAttributeId());
				return;
			}

			String oldValue = attribute.getAttributeValues(attribute.getDataType()).get(0);
			if (!oldValue.equals(value)) { // if the attribute has changed
				log.log(Level.SEVERE, "Attribute {0}={1}:{2} changed at {3}", new Object[] { attribute.getAttributeId(),
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

	private boolean checkIfMonitored(String organization) {
		organization = organization.toLowerCase().replace(" ", "");
		switch (organization) {
		case "isp@cnr":
		case "iscom-mise":
			return false;
		default:
			return true;
		}
	}

	public Attribute findAttributeById(String id) throws PIPException {
		return getAttributes().stream().filter(attr -> attr.getAttributeId().equals(id)).findFirst()
				.orElseThrow(() -> new PIPException("Cannot subscribe " + id + " because is missing in the request"));
	}

}
