package it.cnr.iit.ucs.pipwebsocket;

import com.google.gson.GsonBuilder;
import it.cnr.iit.ucs.exceptions.PIPException;
import it.cnr.iit.ucs.obligationmanager.ObligationInterface;
import it.cnr.iit.ucs.pip.AbstractPIPWebSocket;
import it.cnr.iit.ucs.pipwebsocket.json.DomoLight;
import it.cnr.iit.ucs.pipwebsocket.json.DomoLightRequestPostTopicUuid;
import it.cnr.iit.ucs.properties.components.PipProperties;
import it.cnr.iit.utility.dht.jsonpersistent.JsonInResponse;
import it.cnr.iit.utility.errorhandling.Reject;
import it.cnr.iit.xacml.Attribute;
import oasis.names.tc.xacml.core.schema.wd_17.RequestType;

import java.util.List;
import java.util.logging.Logger;

public class PIPWebSocketLamps extends AbstractPIPWebSocket {

    private static final Logger log = Logger.getLogger(PIPWebSocketLamps.class.getName());

    public PIPWebSocketLamps(PipProperties properties) {
        super(properties);
        setClassForTypeFactory(DomoLightRequestPostTopicUuid.class);
        Reject.ifFalse(init(properties), "Error initialising pip : " + properties.getId());
    }

    @Override
    public String retrieve(Attribute attribute) throws PIPException {
        String response = performRequestGetTopicUuid();

        JsonInResponse jsonInResponse = new GsonBuilder()
                .registerTypeAdapterFactory(getTypeFactory())
                .create().fromJson(response, JsonInResponse.class);

        DomoLight domoLight =
                ((DomoLightRequestPostTopicUuid) jsonInResponse.getResponse().getValue()).getValue();

        String value;
        if (domoLight.isStatus()) {
            value = "true";
        } else {
            value = "false";
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
