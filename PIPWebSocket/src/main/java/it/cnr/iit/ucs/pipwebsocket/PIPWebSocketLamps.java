package it.cnr.iit.ucs.pipwebsocket;

import it.cnr.iit.ucs.exceptions.PIPException;
import it.cnr.iit.ucs.obligationmanager.ObligationInterface;
import it.cnr.iit.ucs.pip.AbstractPIPWebSocket;
import it.cnr.iit.ucs.properties.components.PipProperties;
import it.cnr.iit.utility.errorhandling.Reject;
import it.cnr.iit.xacml.Attribute;
import oasis.names.tc.xacml.core.schema.wd_17.RequestType;

import java.util.List;
import java.util.logging.Logger;

// todo: the code of the retrieve method has to be fixed. In particular,
//       it should properly parse the 'response'.
public class PIPWebSocketLamps extends AbstractPIPWebSocket {

    private static final Logger log = Logger.getLogger(PIPWebSocketLamps.class.getName());

    public PIPWebSocketLamps(PipProperties properties) {
        super(properties);
        Reject.ifFalse(init(properties), "Error initialising pip : " + properties.getId());
    }

    @Override
    public String retrieve(Attribute attribute) {
        String response = performRequestGetTopicUuid();
        // fixme: parse the response and return the attribute value
        System.out.println(response);
        String value;
        if (response.contains("\"status\":false")) {
            value = "false";
        } else {
            value = "true";
        }

        // attribute value must always be set after it has been retrieved
        attribute.setValue(attribute.getDataType(), value);
        return value;
    }

    @Override
    protected Logger getLogger() {
        return log;
    }


    @Override
    public void retrieve(RequestType request, List<Attribute> attributeRetrievals) {

    }

    @Override
    public void subscribe(RequestType request, List<Attribute> attributeRetrieval) {

    }

    @Override
    public void update(String json) throws PIPException {

    }

    @Override
    public void performObligation(ObligationInterface obligation) {

    }
}
