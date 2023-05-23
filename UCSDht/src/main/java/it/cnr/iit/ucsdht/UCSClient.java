package it.cnr.iit.ucsdht;

import it.cnr.iit.ucs.core.UCSCoreService;
import it.cnr.iit.ucs.core.UCSCoreServiceBuilder;
import it.cnr.iit.ucs.exceptions.RequestException;
import it.cnr.iit.ucs.message.endaccess.EndAccessMessage;
import it.cnr.iit.ucs.message.endaccess.EndAccessResponseMessage;
import it.cnr.iit.ucs.message.startaccess.StartAccessMessage;
import it.cnr.iit.ucs.message.startaccess.StartAccessResponseMessage;
import it.cnr.iit.ucs.message.tryaccess.TryAccessMessage;
import it.cnr.iit.ucs.message.tryaccess.TryAccessResponseMessage;
import it.cnr.iit.ucs.pep.PEPInterface;
import it.cnr.iit.ucs.properties.components.PapProperties;
import it.cnr.iit.ucs.properties.components.PepProperties;
import it.cnr.iit.ucs.properties.components.PipProperties;
import it.cnr.iit.ucs.ucs.UCSInterface;
import it.cnr.iit.ucsdht.properties.UCSDhtPepProperties;
import it.cnr.iit.ucsdht.properties.UCSDhtProperties;
import it.cnr.iit.xacml.wrappers.PolicyWrapper;
import it.cnr.iit.xacml.wrappers.RequestWrapper;

import java.util.List;
import java.util.logging.Logger;

/**
 * @author Marco Rasori
 */
public class UCSClient {

    private static final Logger LOGGER = Logger.getLogger(UCSClient.class.getName());

    private final UCSDhtProperties properties;

    private UCSCoreService ucs;

    private final String papPath;

    public UCSClient(List<PipProperties> pipPropertiesList, PapProperties papProperties) {
        properties = new UCSDhtProperties(pipPropertiesList, papProperties);
        try {
            ucs = new UCSCoreServiceBuilder().setProperties(properties).build();
        } catch (Exception e) {
            e.printStackTrace();
        }
        papPath = properties.getPolicyAdministrationPoint().getPath();
    }

    public TryAccessResponseMessage tryAccess(String request, String policy, String pepId, String messageId) {
        TryAccessMessage message = buildTryAccessMessage(request, policy, pepId, messageId);
        return ucs.tryAccess(message);
    }

    public StartAccessResponseMessage startAccess(String sessionId, String pepId, String messageId) {
        StartAccessMessage message = buildStartAccessMessage(sessionId, pepId, messageId);
        return ucs.startAccess(message);
    }

    public EndAccessResponseMessage endAccess(String sessionId, String pepId, String messageId) {
        EndAccessMessage message = buildEndAccessMessage(sessionId, pepId, messageId);
        return ucs.endAccess(message);
    }

    private TryAccessMessage buildTryAccessMessage(String request, String policy, String pepId, String messageId) {
        // todo: get the properties related to the pepId passed as argument
        PepProperties pepProperties = properties.getPepList().get(0);
//        TryAccessMessage message = new TryAccessMessage(pepProperties.getId(), pepProperties.getUri());
        // todo: find out what 'destination' (second parameter of the TryAccessMessage method) should be.
        //       In existing invocations, once is the ucsUri and once the pep.Uri contained in the pepProperties.
        //       I can't figure out its purpose yet.
        TryAccessMessage message = new TryAccessMessage(pepId, pepProperties.getUri(), messageId);
        message.setRequest(request);
//        message.setPolicy(policy);
        return message;
    }

    private StartAccessMessage buildStartAccessMessage(String sessionId, String pepId, String messageId) {
        // todo: same as in buildTryAccessMessage
        PepProperties pepProperties = properties.getPepList().get(0);
        // todo: same as in buildTryAccessMessage
        StartAccessMessage message = new StartAccessMessage(pepId, pepProperties.getUri(), messageId);
        message.setSessionId(sessionId);
        return message;
    }

    private EndAccessMessage buildEndAccessMessage(String sessionId, String pepId, String messageId) {
        // todo: same as in buildTryAccessMessage
        PepProperties pepProperties = properties.getPepList().get(0);
        // todo: same as in buildTryAccessMessage
        EndAccessMessage message = new EndAccessMessage(pepId, pepProperties.getUri(), messageId);
        message.setSessionId(sessionId);
        return message;
    }

    public boolean addPolicy(String policy) {
        String id = ucs.getPap().addPolicy(policy);
        return id != null;
    }

    public boolean deletePolicy(String policyId) {
        return ucs.getPap().deletePolicy(policyId);
    }

    public UCSInterface getInterface() {
        return ucs;
    }

    public UCSDhtProperties getProperties() {
        return properties;
    }

    public String findPolicy(String req) {
        RequestWrapper request;
        try {
            request = RequestWrapper.build(req);
        } catch (RequestException e) {
            LOGGER.info("Unable to create request wrapper");
            return null;
        }
        PolicyWrapper policy = ucs.getPdp().findPolicy(request);
        return policy.getPolicyType().getPolicyId();
    }

    public String getPapPath() {
        return papPath;
    }

    public boolean addPep(String pepId, String subTopicName, String subTopicUuid) {
        UCSDhtPepProperties pepProperties = new UCSDhtPepProperties();
        pepProperties.setId(pepId);
        pepProperties.setSubTopicName(subTopicName);
        pepProperties.setSubTopicUuid(subTopicUuid);
        PEPInterface pep = new PEPDhtUCSSide(pepProperties);
        try {
            ucs.getPEPMap().put(pepId, pep);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public boolean pepMapHas(String pepId) {
        return ucs.getPEPMap().containsKey(pepId);
    }

    public PepProperties getPepProperties(String pepId) {
        return ucs.getPEPMap().get(pepId).getProperties();
    }
}