package it.cnr.iit.ucs.pipldap;

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

	private static final String ATTRID_ORG = "urn:oasis:names:tc:xacml:3.0:subject:subject-organisation";
	private List<String> orgList = new ArrayList<String>();

	public PIPLdap(PipProperties properties) {
		super(properties);
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
		attributeList.stream().forEach(a -> addAdditionalInformation(request, a));

		try {
			Map<String, String> attributesToValues = new HashMap<>();
			attributesToValues = new ObjectMapper().readValue(attributeList.get(0).getAdditionalInformations(),
					new TypeReference<Map<String, String>>() {
					});
			String organization = attributesToValues.get(ATTRID_ORG).toLowerCase().replace(" ", "");
			orgList.stream().filter(org -> org.equals(organization)).findFirst().orElseThrow(
					() -> new PIPException("the organization " + organization + " is not monitored by PIPLdap"));

			orgList.stream().forEach(el -> log.severe("organization: " + el));

			if (!orgList.isEmpty()) {
				retrieve(getAttributes().get(0));
			}

		} catch (PIPException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	/**
	 * This is the function called by the context handler whenever we have a remote
	 * retrieve request
	 */
	@Override
	public String retrieve(Attribute attribute) throws PIPException {
		String[] attrsToSearch = { "uid", "o", "cn", "c" };

		String organization = orgList.get(0);
		String filter = "";

		switch (organization) {
		case "chino":
		case "gps":
		case "3drepo":
		case "kent":
			filter = "ou=Pilots,ou=SME,ou=SME Pilot,dc=c3isp,dc=eu";
			break;
		case "spartacompany1":
		case "spartacompany2":
		case "spartacompany3":
			filter = "ou=Pilots,ou=SPARTA,ou=Users,dc=c3isp,dc=eu";
			break;
		default:
			filter = "ou=Users,dc=c3isp,dc=eu";
			break;
		}

		Map<String, Map<String, String>> userAtrributes = LdapQuery.queryForAll(filter, attrsToSearch);
		return null;

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
