package it.cnr.iit.ucs.pipdsa;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import it.cnr.iit.ucs.constants.ENTITIES;
import it.cnr.iit.ucs.exceptions.PIPException;
import it.cnr.iit.ucs.journaling.JournalBuilder;
import it.cnr.iit.ucs.journaling.JournalingInterface;
import it.cnr.iit.ucs.message.attributechange.AttributeChangeMessage;
import it.cnr.iit.ucs.obligationmanager.ObligationInterface;
import it.cnr.iit.ucs.pip.PIPBase;
import it.cnr.iit.ucs.pip.PIPKeywords;
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

	public static final String DSAMGR_URL = "DSAMGR_URL";
	public static final String DSA_FILTER = "filter";
	private String dsamgrUrl = "";

	private String filePath;

	public PIPDsa(PipProperties properties) {
		super(properties);
		Reject.ifFalse(init(properties), "Error initialising pip : " + properties.getId());
	}

	private boolean init(PipProperties properties) {
		try {
			log.severe("\n\n\nPIPDsa.init()\n\n\n");
			List<Map<String, String>> pipProperties = properties.getAttributes();
			Reject.ifFalse(properties.getAdditionalProperties().containsKey(DSAMGR_URL), "missing DSA Manager url");
			dsamgrUrl = properties.getAdditionalProperties().get(DSAMGR_URL);
			pipProperties.stream().forEach(pip -> addAttributes(pip));
			journal = JournalBuilder.build(properties);
			PIPDsaSubscriberTimer subscriberTimer = new PIPDsaSubscriberTimer(this);
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
		expectedCategory = Category.toCATEGORY(pip.get(DSA_FILTER));
		Reject.ifNull(expectedCategory, "missing filter attribute");
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
		log.severe("\n\n\nin PIPReader.retrieve value = " + value + "\n\n\n");

		request.addAttribute(attribute, value);
	}

	/**
	 * This is the function called by the context handler whenever we have a remote
	 * retrieve request
	 */
	@Override
	public String retrieve(Attribute attribute) throws PIPException {
		log.severe("\n\n\nin PIPReader.retrieve, attribute = " + attribute + "\n\n\n");
		if (isEnvironmentCategory(attribute)) {
			return read();
		} else {
			return read(attribute.getAdditionalInformations());
		}
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
		String filter = request.getAttributeValue(expectedCategory);
		attribute.setAdditionalInformations(filter);
	}

	public boolean isEnvironmentCategory(Attribute attribute) {
		return attribute.getCategory() == Category.ENVIRONMENT;
	}

	/**
	 * Effective retrieval of the monitored value.
	 *
	 * @return the requested value
	 * @throws PIPException
	 */
	private String read() throws PIPException {
		try {
			Path path = Paths.get(filePath);
			// TODO UCS-33 NOSONAR
			String value = new String(Files.readAllBytes(path));
			journal.logString(formatJournaling(value));
			return value;
		} catch (IOException e) {
			throw new PIPException("Attribute Manager error : " + e.getMessage());
		}
	}

	/**
	 * Effective retrieval of the monitored value looking for the line containing a
	 * filter. NOTE we suppose that in the file each line has the following
	 * structure: filter\tattribute.
	 *
	 * @param filter the string to be used to search for the item we're interested
	 *               into
	 * @return the requested value
	 * @throws PIPException
	 */
	private String read(String filter) throws PIPException {
		// TODO UCS-33 NOSONAR
		log.severe("\n\n\nin read(filter), filter = " + filter + "\nfilePath = " + filePath + "\n\n");
		try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
			for (String line; (line = br.readLine()) != null;) {
				log.severe("for...");
				if (line.contains(filter)) {
					log.severe("\n\n\nin if\n\n\n");
					String value = line.split("\\s+")[1];
					journal.logString(formatJournaling(value, filter));
					return value;
				}
			}
		} catch (Exception e) {
			throw new PIPException("Attribute Manager error : " + e.getMessage());
		}
		log.severe("\n\n\nexit without success\n\n\n");
		throw new PIPException("Attribute Manager error : no value for this filter : " + filter);
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
		try {
			Path path = Paths.get(filePath);
			Files.write(path, data.getBytes());
		} catch (IOException e) {
			log.severe("Error updating attribute : " + e.getMessage());
		}
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
