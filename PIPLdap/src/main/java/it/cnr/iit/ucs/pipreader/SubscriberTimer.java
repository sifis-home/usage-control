package it.cnr.iit.ucs.pipreader;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;

import it.cnr.iit.ucs.pip.PIPCHInterface;
import it.cnr.iit.xacml.Attribute;

public class SubscriberTimer extends TimerTask {

	protected final BlockingQueue<Attribute> subscriptions;
	private PIPCHInterface contextHandlerInterface;
	private final LDAPConnector ldapConnector;
	private final ArrayList<String> attributeNames;

	/**
	 * Constructor: it initializes the parameters used by the thread
	 *
	 * @param subscriptions_ a reference to the subscriptions map used to read the
	 *                       last attributes values read
	 * @param properties_    a reference to the properties object used to retrieve
	 *                       LDAP sever authentication parameters
	 * @param queue_         a reference to the subscriptions queue used to push
	 *                       changed attributes
	 *
	 */
	public SubscriberTimer(PIPCHInterface contextHandlerInterface, BlockingQueue<Attribute> subscriptions,
			LDAPConnector ldapConnector, ArrayList<String> attributeNames) {
		this.contextHandlerInterface = contextHandlerInterface;
		this.subscriptions = subscriptions;
		this.ldapConnector = ldapConnector;
		this.attributeNames = attributeNames;
	}

	@Override
	public synchronized void run() {
		for (Attribute attribute : subscriptions) {
			// System.out.println("[PipFile] Subscribe iteration");
			String filter = attribute.getAdditionalInformations();

			Map<String, Set<String>> ldapMap = ldapConnector.search("dc=c3isp,dc=eu", attributeNames, filter);

			Set<String> values = ldapMap.get(attribute.getAttributeId());
			LinkedList<String> oldValues = new LinkedList<>();
			for (List<String> v : attribute.getAttributeValueMap().values()) {
				oldValues.addAll(v);
			}

			for (String value : values) {
				if (!oldValues.contains(value)) {
					PipChContent pipChContent = new PipChContent();
					pipChContent.addAttribute(attribute);
					MessagePipCh messagePipCh = new MessagePipCh(PART.PIP.toString(), PART.CH.toString());
					messagePipCh.setMotivation(pipChContent);
					contextHandlerInterface.attributeChanged(messagePipCh);
				}
			}

		}
		return;
	}

	public void setContextHandlerInterface(PIPCHInterface contextHandlerInterface) {
		this.contextHandlerInterface = contextHandlerInterface;
	}

	public PIPCHInterface getContextHandler() {
		return this.contextHandlerInterface;
	}
}
