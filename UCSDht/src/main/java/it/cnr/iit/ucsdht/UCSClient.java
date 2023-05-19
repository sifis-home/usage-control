package it.cnr.iit.ucsdht;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import it.cnr.iit.ucs.core.UCSCoreService;
import it.cnr.iit.ucs.core.UCSCoreServiceBuilder;
import it.cnr.iit.ucs.exceptions.RequestException;
import it.cnr.iit.ucs.message.endaccess.EndAccessMessage;
import it.cnr.iit.ucs.message.endaccess.EndAccessResponseMessage;
import it.cnr.iit.ucs.message.startaccess.StartAccessMessage;
import it.cnr.iit.ucs.message.startaccess.StartAccessResponseMessage;
import it.cnr.iit.ucs.message.tryaccess.TryAccessMessage;
import it.cnr.iit.ucs.message.tryaccess.TryAccessResponseMessage;
import it.cnr.iit.ucs.pap.PolicyAdministrationPoint;
import it.cnr.iit.ucs.pdp.PolicyDecisionPoint;
import it.cnr.iit.ucs.pep.PEPInterface;
import it.cnr.iit.ucs.properties.components.PapProperties;
import it.cnr.iit.ucs.properties.components.PepProperties;
import it.cnr.iit.ucs.properties.components.PipProperties;
import it.cnr.iit.ucs.ucs.UCSInterface;
import it.cnr.iit.ucsdht.properties.UCSDhtCoreProperties;
import it.cnr.iit.ucsdht.properties.UCSDhtProperties;
import it.cnr.iit.xacml.wrappers.PolicyWrapper;
import it.cnr.iit.xacml.wrappers.RequestWrapper;
//import se.sics.ace.ucs.properties.AceUcsProperties;

/**
 *
 * @author Simone Facchini and Marco Rasori
 *
 */
public class UCSClient {

    private static final Logger LOGGER = Logger.getLogger(UCSClient.class.getName());

    private final UCSDhtProperties properties;

    private UCSInterface ucs;

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
        return (TryAccessResponseMessage) ucs.tryAccess(message);
    }

    public StartAccessResponseMessage startAccess(String sessionId, String pepId, String messageId) {
        StartAccessMessage message = buildStartAccessMessage(sessionId, pepId, messageId);
        return (StartAccessResponseMessage) ucs.startAccess(message);
    }

    public EndAccessResponseMessage endAccess(String sessionId, String pepId, String messageId) {
        EndAccessMessage message = buildEndAccessMessage(sessionId, pepId, messageId);
        return (EndAccessResponseMessage) ucs.endAccess(message);
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

    public void addPolicy(String policy) {
        PolicyAdministrationPoint pap = new PolicyAdministrationPoint(properties.getPolicyAdministrationPoint());
        pap.addPolicy(policy);
    }

    public UCSInterface getInterface() {
        return ucs;
    }

    public UCSDhtProperties getProperties() {
        return properties;
    }

    public Map<String, PEPInterface> getPepMap() {
        return ((UCSCoreService)ucs).getPEPMap();
    }

//    public void setUcsHelperForPeps(UcsHelper uh){
//        Map<String, PEPInterface> pepMap = getPepMap();
//        for (Map.Entry<String, PEPInterface> entry : pepMap.entrySet()) {
//            AcePep ap = (AcePep)(entry.getValue());
//            ap.setUcsHelper(uh);
//        }
//    }

    public String findPolicy(String req) {
        RequestWrapper request;
        try{
            request = RequestWrapper.build(req);
        } catch(RequestException e) {
            LOGGER.info("Unable to create request wrapper");
            return null;
        }
        PolicyAdministrationPoint pap = new PolicyAdministrationPoint(properties.getPolicyAdministrationPoint());
        PolicyDecisionPoint pdp = new PolicyDecisionPoint(properties.getPolicyDecisionPoint());
        pdp.setPap(pap);
        PolicyWrapper policy = pdp.findPolicy(request);
        return policy.getPolicyType().getPolicyId();
    }

    public String getPapPath() {
        return papPath;
    }

//    public void addPep(String id, PEPInterface pepInterface) {
//        this.getPepMap().put(id, pepInterface);
//    }
}