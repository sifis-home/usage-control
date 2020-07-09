package it.cnr.iit.ucs.pipdsa;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import it.cnr.iit.common.attributes.AttributeIds;
import it.cnr.iit.common.lambda.exceptions.ConsumerException;
import it.cnr.iit.ucs.constants.ENTITIES;
import it.cnr.iit.ucs.exceptions.PIPException;
import it.cnr.iit.ucs.journaling.JournalBuilder;
import it.cnr.iit.ucs.journaling.JournalingInterface;
import it.cnr.iit.ucs.message.attributechange.AttributeChangeMessage;
import it.cnr.iit.ucs.obligationmanager.ObligationInterface;
import it.cnr.iit.ucs.pip.PIPBase;
import it.cnr.iit.ucs.pip.PIPKeywords;
import it.cnr.iit.ucs.pipdsa.statics.DsaUrlMethods;
import it.cnr.iit.ucs.pipdsa.statics.HttpAuthorization;
import it.cnr.iit.ucs.pipdsa.types.DsaAttribute;
import it.cnr.iit.ucs.pipdsa.types.Message;
import it.cnr.iit.ucs.properties.components.PipProperties;
import it.cnr.iit.utility.errorhandling.Reject;
import it.cnr.iit.xacml.Attribute;
import it.cnr.iit.xacml.Category;
import it.cnr.iit.xacml.DataType;
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
public final class PIPDsa extends PIPBase {

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
	private static Logger log = Logger.getLogger(PIPDsa.class.getName());
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

	public static final String DSAMGR_URL = "dsamgr-endpoint";
	public static final String DSAMGR_STATUS = "status";
	public static final String DSAMGR_VERSION = "version";
	public static final String REST_USERNAME = "rest-username";
	public static final String REST_PASSWORD = "rest-password";

	public static final String DSA_FILTER = "filter";

	public PIPDsa(PipProperties properties) {
		super(properties);
		Reject.ifFalse(init(properties), "Error initialising pip : " + properties.getId());
	}

	private boolean init(PipProperties properties) {

		try {
			log.severe("Initializing PIPDsa...");
			List<Map<String, String>> pipProperties = properties.getAttributes();
			Reject.ifFalse(properties.getAdditionalProperties().containsKey(DSAMGR_URL), "missing DSA Manager url");
			setDsaEndpointMethods(properties);
			setAuthorizations(properties);
			pipProperties.stream().forEach(pip -> addAttributes(pip));
			journal = JournalBuilder.build(properties);
			PIPDsaSubscriberTimer subscriberTimer = new PIPDsaSubscriberTimer(this);
			subscriberTimer.start();
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	private void setDsaEndpointMethods(PipProperties properties) {
		DsaUrlMethods.setDsaUrl(properties.getAdditionalProperties().get(DSAMGR_URL));
		Reject.ifFalse(properties.getAdditionalProperties().containsKey(DSAMGR_STATUS),
				"missing DSA Manager method to get the Status attribute");
		DsaUrlMethods.setDsaStatus(properties.getAdditionalProperties().get(DSAMGR_STATUS));
		Reject.ifFalse(properties.getAdditionalProperties().containsKey(DSAMGR_VERSION),
				"missing DSA Manager method to get the Version attribute");
		DsaUrlMethods.setDsaVersion(properties.getAdditionalProperties().get(DSAMGR_VERSION));
	}

	private void setAuthorizations(PipProperties properties) {
		Reject.ifFalse(properties.getAdditionalProperties().containsKey(REST_USERNAME),
				"missing RestTemplate username");
		HttpAuthorization.setRestUsername(properties.getAdditionalProperties().get(REST_USERNAME));
		Reject.ifFalse(properties.getAdditionalProperties().containsKey(REST_PASSWORD),
				"missing RestTemplate password");
		HttpAuthorization.setRestPassword(properties.getAdditionalProperties().get(REST_PASSWORD));
	}

	private void addAttributes(Map<String, String> pip) {
		Attribute attribute = new Attribute();
		attribute.setAttributeId(pip.get(PIPKeywords.ATTRIBUTE_ID));
		Category category = Category.toCATEGORY(pip.get(PIPKeywords.CATEGORY));
		attribute.setCategory(category);
		DataType dataType = DataType.toDATATYPE(pip.get(PIPKeywords.DATA_TYPE));
		attribute.setDataType(dataType);
		addAttribute(attribute);
	}

	@Override
	public void retrieve(RequestType request) {
		Reject.ifNull(request);
		String dsaId = request.getAttribute(Category.RESOURCE.toString(), AttributeIds.DSA_ID);

		List<Attribute> attributeList = getAttributes();
		attributeList.stream().forEach(a -> addAdditionalInformation(a, dsaId));

//		List<String> dsaAttributeStringList = new ArrayList<String>();

//		attributeList.stream().filter(Objects::nonNull)
//				.map(ThrowingException.unchecked(a -> dsaAttributeStringList.add(retrieve(a)))).filter(Objects::nonNull)
//				.collect(Collectors.toList());

		try {
			List<Attribute> attrToSubscribe = new ArrayList<Attribute>();
			attrToSubscribe.add(findAttributeById(AttributeIds.DSA_VERSION));
			attrToSubscribe.add(findAttributeById(AttributeIds.DSA_STATUS));
			attrToSubscribe.stream()
					.forEach(ConsumerException.unchecked(attr -> request.addAttribute(attr, retrieve(attr))));
		} catch (PIPException e) {
			log.severe(e.getMessage());
		}

//		List<DsaAttribute> dsaAttributeList = dsaAttributeStringList.stream()
//				.map(ThrowingException.unchecked(a -> new ObjectMapper().readValue(a, DsaAttribute.class)))
//				.collect(Collectors.toList());
//		Reject.ifEmpty(dsaAttributeList);
//
//		addAttributesToRequest(request, dsaAttributeList);

	}

	private void addAttributesToRequest(RequestType request, List<DsaAttribute> dsaAttributeList) {

		for (Attribute attr : getAttributes()) {
			for (DsaAttribute dsaAttr : dsaAttributeList) {
				if (attr.getAttributeId().equals(dsaAttr.getId())) {
					request.addAttribute(attr, dsaAttr.getMessage());
				}
			}
		}

	}

	/**
	 * This is the function called by the context handler whenever we have a remote
	 * retrieve request
	 */
	@Override
	public String retrieve(Attribute attribute) throws PIPException {
		try {

			String dsaId = attribute.getAdditionalInformations();
			UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(DsaUrlMethods.getDsaUrl());

			DsaAttribute dsaAttribute = null;
			if (attribute.getAttributeId().equals(AttributeIds.DSA_STATUS)) {
				builder.path(DsaUrlMethods.getDsaStatus()).queryParam("dsaid", dsaId);
				dsaAttribute = idsToMessage(builder, attribute.getAttributeId());
			} else if (attribute.getAttributeId().equals(AttributeIds.DSA_VERSION)) {
				builder.path(DsaUrlMethods.getDsaVersion()).queryParam("dsaid", dsaId);
				dsaAttribute = idsToMessage(builder, attribute.getAttributeId());
			} else {
				throw new PIPException("cannot retrieve the value of " + attribute.getAttributeId());
			}
			String retrieved = dsaAttribute.getMessage();
			log.severe("retrieved the value " + retrieved + " from PIPDsa");
			return retrieved;
		} catch (PIPException e) {
			log.severe(e.getMessage());
		}
		return null;

	}

	private DsaAttribute idsToMessage(UriComponentsBuilder builder, String id) {
		HttpHeaders headers = new HttpHeaders();
		String credentials = "Basic " + HttpAuthorization.base64();
		headers.add("Authorization", credentials);
		headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
		HttpEntity<String> entity = new HttpEntity<String>(headers);

		String url = builder.build().toString();
		ResponseEntity<Message> response = new RestTemplate().exchange(url, HttpMethod.POST, entity, Message.class);
		Reject.ifNull(response);

		DsaAttribute dsaAttr = new DsaAttribute();
		dsaAttr.setId(id);
		dsaAttr.setMessage(response.getBody().getMessage());
		return dsaAttr;
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
		log.severe("called void subscribe in PIPDsa");
		Reject.ifNull(request);

		List<Attribute> attrToSubscribe = new ArrayList<Attribute>();
		attrToSubscribe.add(findAttributeById(AttributeIds.DSA_VERSION));
		attrToSubscribe.add(findAttributeById(AttributeIds.DSA_STATUS));

		log.severe("subscribing the following attributes: ");
		attrToSubscribe.stream().forEach(attr -> log.severe(attr.getAttributeId()));

		String dsaId = request.getAttribute(Category.RESOURCE.toString(), AttributeIds.DSA_ID);
		attrToSubscribe.stream().forEach(attr -> addAdditionalInformation(attr, dsaId));

		attrToSubscribe.stream()
				.forEach(ConsumerException.unchecked(attr -> request.addAttribute(attr, subscribe(attr))));
	}

	/**
	 * This is the function called by the context handler whenever we have a remote
	 * retrieve request
	 */
	@Override
	public String subscribe(Attribute attribute) throws PIPException {
		log.severe("called String subscribe");
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

	private void addAdditionalInformation(Attribute attribute, String dsaId) {
		attribute.setAdditionalInformations(dsaId);
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
			log.log(Level.SEVERE, "Polling on value of the attribute " + attribute.getAttributeId() + " for change.");

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

	public Attribute findAttributeById(String id) throws PIPException {
		return getAttributes().stream().filter(attr -> attr.getAttributeId().equals(id)).findFirst()
				.orElseThrow(() -> new PIPException("Cannot subscribe " + id + " because is missing in the request"));
	}

}
