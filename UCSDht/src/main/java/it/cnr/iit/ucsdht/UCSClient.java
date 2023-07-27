package it.cnr.iit.ucsdht;

import it.cnr.iit.ucs.contexthandler.ContextHandler;
import it.cnr.iit.ucs.contexthandler.pipregistry.PIPRegistryInterface;
import it.cnr.iit.ucs.core.UCSCoreService;
import it.cnr.iit.ucs.core.UCSCoreServiceBuilder;
import it.cnr.iit.ucs.exceptions.PAPException;
import it.cnr.iit.ucs.exceptions.RequestException;
import it.cnr.iit.ucs.message.endaccess.EndAccessMessage;
import it.cnr.iit.ucs.message.endaccess.EndAccessResponseMessage;
import it.cnr.iit.ucs.message.startaccess.StartAccessMessage;
import it.cnr.iit.ucs.message.startaccess.StartAccessResponseMessage;
import it.cnr.iit.ucs.message.tryaccess.TryAccessMessage;
import it.cnr.iit.ucs.message.tryaccess.TryAccessResponseMessage;
import it.cnr.iit.ucs.pep.PEPInterface;
import it.cnr.iit.ucs.pip.PIPBase;
import it.cnr.iit.ucs.pipreader.PIPReader;
import it.cnr.iit.ucs.properties.components.PapProperties;
import it.cnr.iit.ucs.properties.components.PepProperties;
import it.cnr.iit.ucs.properties.components.PipProperties;
import it.cnr.iit.ucs.properties.components.SessionManagerProperties;
import it.cnr.iit.ucs.ucs.UCSInterface;
import it.cnr.iit.ucsdht.properties.UCSDhtPepProperties;
import it.cnr.iit.ucsdht.properties.UCSDhtPipReaderProperties;
import it.cnr.iit.ucsdht.properties.UCSDhtProperties;
import it.cnr.iit.utility.errorhandling.Reject;
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

    private final UCSCoreService ucs;

    private final String papPath;

    public UCSClient(List<PipProperties> pipPropertiesList, PapProperties papProperties,
                     SessionManagerProperties smProperties, List<PepProperties> pepPropertiesList) {
        properties = new UCSDhtProperties(pipPropertiesList, papProperties, smProperties, pepPropertiesList);
        try {
            ucs = new UCSCoreServiceBuilder().setProperties(properties).build();
        } catch (Exception e) {
            // e.printStackTrace();
            throw new RuntimeException(e);
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
        PepProperties pepProperties = ucs.getPEPMap().get(pepId).getProperties();
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
        PepProperties pepProperties = ucs.getPEPMap().get(pepId).getProperties();
        // todo: same as in buildTryAccessMessage
        StartAccessMessage message = new StartAccessMessage(pepId, pepProperties.getUri(), messageId);
        message.setSessionId(sessionId);
        return message;
    }

    private EndAccessMessage buildEndAccessMessage(String sessionId, String pepId, String messageId) {
        PepProperties pepProperties = ucs.getPEPMap().get(pepId).getProperties();
        // todo: same as in buildTryAccessMessage
        EndAccessMessage message = new EndAccessMessage(pepId, pepProperties.getUri(), messageId);
        message.setSessionId(sessionId);
        return message;
    }

    public boolean addPolicy(String policy) {
        // todo: check that policyId and the policy identifier within the policy match. If not, return false
        String id = ucs.getPap().addPolicy(policy);
        return id != null;
    }

    public boolean deletePolicy(String policyId) {
        return ucs.getPap().deletePolicy(policyId);
    }

    public List<String> listPolicies() {
        try {
            return ucs.getPap().listPolicies();
        } catch (PAPException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public String getPolicy(String policyId) {
        return ucs.getPap().retrievePolicy(policyId);
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
            ucs.getRequestManager().setPEPMap(ucs.getPEPMap());
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public boolean addPip(String pipType, String attributeId, String category,
                          String dataType, String attributePath, String fileName, long refreshRate) {
        if (!pipType.equals("PIPReader")) {
            System.err.println("pip_type differs from PIPReader: " +
                    "at the moment, PIPReader is the only type of PIP supported");
            return false;
        }

        checkIfAttributeIdAlreadyMonitored(attributeId);

        // need to check it here since in the properties we concatenate the attributePath and the fileName.
        // Therefore, when the PIP gets initialized, the fileName will never be empty, but it possibly
        // contains only the attributePath if the fileName happened to be empty.
        Reject.ifTrue(fileName.isEmpty(), "'file_name' cannot be empty");

        UCSDhtPipReaderProperties pipProperties = new UCSDhtPipReaderProperties();

        pipProperties.addAttribute(attributeId, category, dataType, attributePath + fileName);
        pipProperties.setRefreshRate(refreshRate);

        PIPBase pip = new PIPReader(pipProperties);
        try {
            ucs.getPipList().add(pip);
            pip.setRequestManager(ucs.getRequestManager());
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private void checkIfAttributeIdAlreadyMonitored(String attributeId) {
        for (PIPBase pip : ucs.getPipList()) {
            Reject.ifTrue(pip.getAttributeIds().contains(attributeId),
                    "Another PIP is already monitoring the same attributeId");
        }
    }

    public boolean pepMapHas(String pepId) {
        return ucs.getPEPMap().containsKey(pepId);
    }

    public PepProperties getPepProperties(String pepId) {
        return ucs.getPEPMap().get(pepId).getProperties();
    }

    public PIPRegistryInterface getPipRegistry() {
        return ucs.getContextHandler().getPipRegistry();
    }

    public ContextHandler getContextHandler() {
        return (ContextHandler) ucs.getContextHandler();
    }
}