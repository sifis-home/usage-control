package it.cnr.iit.ucsdht;

import it.cnr.iit.ucs.message.Message;
import it.cnr.iit.ucs.message.reevaluation.ReevaluationResponseMessage;
import it.cnr.iit.ucs.pep.PEPInterface;
import it.cnr.iit.ucs.properties.components.PepProperties;
import it.cnr.iit.utility.dht.jsondht.JsonOut;
import it.cnr.iit.utility.dht.jsondht.MessageContent;
import it.cnr.iit.utility.dht.jsondht.reevaluation.ReevaluationResponse;
import it.cnr.iit.utility.errorhandling.Reject;

import static it.cnr.iit.utility.dht.DHTUtils.buildOutgoingJsonObject;

public class PEPDhtUCSSide implements PEPInterface {

    private final PepProperties properties;

    public PEPDhtUCSSide(PepProperties properties) {
        Reject.ifNull(properties);
        this.properties = properties;
    }

    @Override
    public Message onGoingEvaluation(ReevaluationResponseMessage message) {

        MessageContent messageOut =
                new ReevaluationResponse(
                        message.getMessageId(), message.getEvaluation().getResult(), message.getSessionId());
        JsonOut jsonOut = buildOutgoingJsonObject(
                messageOut,
                properties.getId(),
                properties.getSubTopicName(),
                properties.getSubTopicUuid(),
                properties.getCommandType());

        UCSDht.handleReevaluation(jsonOut);

        return null;
    }

    @Override
    public String receiveResponse(Message message) {
        return null;
    }


    @Override
    public PepProperties getProperties() {
        return properties;
    }
}
