package it.cnr.iit.ucsdht;

import it.cnr.iit.ucs.message.endaccess.EndAccessResponseMessage;
import it.cnr.iit.ucs.message.startaccess.StartAccessResponseMessage;
import it.cnr.iit.ucs.message.tryaccess.TryAccessResponseMessage;
import it.cnr.iit.ucs.properties.components.PepProperties;
import it.cnr.iit.utility.dht.jsondht.JsonIn;
import it.cnr.iit.utility.dht.jsondht.JsonOut;
import it.cnr.iit.utility.dht.jsondht.MessageContent;
import it.cnr.iit.utility.dht.jsondht.endaccess.EndAccessRequest;
import it.cnr.iit.utility.dht.jsondht.endaccess.EndAccessResponse;
import it.cnr.iit.utility.dht.jsondht.error.ErrorResponse;
import it.cnr.iit.utility.dht.jsondht.registration.RegisterRequest;
import it.cnr.iit.utility.dht.jsondht.registration.RegisterResponse;
import it.cnr.iit.utility.dht.jsondht.startaccess.StartAccessRequest;
import it.cnr.iit.utility.dht.jsondht.startaccess.StartAccessResponse;
import it.cnr.iit.utility.dht.jsondht.tryaccess.TryAccessRequest;
import it.cnr.iit.utility.dht.jsondht.tryaccess.TryAccessResponse;

import java.util.Base64;

import static it.cnr.iit.ucsdht.ManagerUtils.*;
import static it.cnr.iit.ucsdht.UCSDht.*;
import static it.cnr.iit.utility.dht.DHTUtils.buildOutgoingJsonObject;

public class PEPMessageManager {

    protected static void processMessage(JsonIn jsonIn) {
        MessageContent message = jsonIn.getVolatile().getValue().getCommand().getValue().getMessage();
        if (message instanceof RegisterRequest) {
            // handle register request
            System.out.println("handle register request");
            handleRegisterRequest(jsonIn);
        } else if (message instanceof TryAccessRequest) {
            // handle try access request
            System.out.println("handle try access request");
            handleTryAccessRequest(jsonIn);
        } else if (message instanceof StartAccessRequest) {
            // handle start access request
            System.out.println("handle start access request");
            handleStartAccessRequest(jsonIn);
        } else if (message instanceof EndAccessRequest) {
            // handle end access request
            System.out.println("handle end access request");
            handleEndAccessRequest(jsonIn);
        } else {
            // class not recognized. Handle case
            // this should not happen since the deserialization would already have thrown an exception
            System.err.println("class not recognized. It might be a ResponseMessage");
        }
    }


    private static void handleTryAccessRequest(JsonIn jsonIn) {
        // construct a TryAccess message compliant with what the UCS accepts
        TryAccessRequest messageIn = (TryAccessRequest) getMessageFromJson(jsonIn);

        // make the actual try access request to the UCS
        String request = new String(Base64.getDecoder().decode(messageIn.getRequest()));
        TryAccessResponseMessage response =
                ucsClient.tryAccess(request, null, getIdFromJson(jsonIn), getMessageIdFromJson(jsonIn));

        // build the json object
        JsonOut jsonOut = response == null ?
                buildErrorResponseMessage(jsonIn, "Error during try access") :
                buildTryAccessResponseMessage(jsonIn, response);

        serializeAndSend(jsonOut);
    }


    private static JsonOut buildTryAccessResponseMessage(JsonIn jsonIn, TryAccessResponseMessage response) {
        MessageContent messageOut = new TryAccessResponse(
                response.getMessageId(), response.getEvaluation().getResult(), response.getSessionId());
        return buildJsonOutForPep(messageOut, getIdFromJson(jsonIn));
    }


    private static void handleStartAccessRequest(JsonIn jsonIn) {
        // construct a StartAccess message compliant with what the UCS accepts
        StartAccessRequest messageIn = (StartAccessRequest) getMessageFromJson(jsonIn);

        // make the actual start access request to the UCS
        StartAccessResponseMessage response =
                ucsClient.startAccess(messageIn.getSession_id(), getIdFromJson(jsonIn), getMessageIdFromJson(jsonIn));

        // build the json object
        JsonOut jsonOut = response == null ?
                buildErrorResponseMessage(jsonIn, "Error during start access") :
                buildStartAccessResponseMessage(jsonIn, response);

        serializeAndSend(jsonOut);
    }


    private static JsonOut buildStartAccessResponseMessage(JsonIn jsonIn, StartAccessResponseMessage response) {
        MessageContent messageOut =
                new StartAccessResponse(
                        response.getMessageId(), response.getEvaluation().getResult());
        return buildJsonOutForPep(messageOut, getIdFromJson(jsonIn));
    }


    private static void handleEndAccessRequest(JsonIn jsonIn) {
        // construct an EndAccess message compliant with what the UCS accepts
        EndAccessRequest messageIn = (EndAccessRequest) getMessageFromJson(jsonIn);

        // make the actual end access request to the UCS
        EndAccessResponseMessage response =
                ucsClient.endAccess(messageIn.getSession_id(), getIdFromJson(jsonIn), getMessageIdFromJson(jsonIn));

        // build the json object
        JsonOut jsonOut = response == null ?
                buildErrorResponseMessage(jsonIn, "Error during end access") :
                buildEndAccessResponseMessage(jsonIn, response);

        serializeAndSend(jsonOut);
    }


    private static JsonOut buildEndAccessResponseMessage(JsonIn jsonIn, EndAccessResponseMessage response) {
        MessageContent messageOut =
                new EndAccessResponse(
                        response.getMessageId(), response.getEvaluation().getResult());
        return buildJsonOutForPep(messageOut, getIdFromJson(jsonIn));
    }


    protected static void handleReevaluation(JsonOut jsonOut) {
        serializeAndSend(jsonOut);
    }


    // this method returns OK both when the registration went through successfully
    // and when the pep was already registered
    // it returns KO if the pep cannot be registered
    private static void handleRegisterRequest(JsonIn jsonIn) {
        JsonOut jsonOut;
        if (!isPepRegistered(jsonIn)) {
            RegisterRequest messageIn = (RegisterRequest) getMessageFromJson(jsonIn);
            String pepId = getIdFromJson(jsonIn);
            String subTopicName = messageIn.getSub_topic_name();
            String subTopicUuid = messageIn.getSub_topic_uuid();

            if (!ucsClient.addPep(pepId, subTopicName, subTopicUuid)) {
                jsonOut = buildRegisterResponseMessage(jsonIn, "KO");
                serializeAndSend(jsonOut);
                return;
            }
        }
        jsonOut = buildRegisterResponseMessage(jsonIn, "OK");
        serializeAndSend(jsonOut);
    }


    private static JsonOut buildRegisterResponseMessage(JsonIn jsonIn, String code) {
        MessageContent messageOut = new RegisterResponse(getMessageIdFromJson(jsonIn), code);
        return buildJsonOutForPep(messageOut, getIdFromJson(jsonIn));
    }


    private static JsonOut buildJsonOutForPep(MessageContent messageOut, String pepId) {
        PepProperties pepProperties = ucsClient.getPepProperties(pepId);
        return buildOutgoingJsonObject(messageOut, pepId,
                pepProperties.getSubTopicName(), pepProperties.getSubTopicUuid(), COMMAND_TYPE);
    }


    protected static JsonOut buildErrorResponseMessage(JsonIn jsonIn, String description) {
        MessageContent messageOut =
                new ErrorResponse(
                        getMessageIdFromJson(jsonIn), description);
        return buildJsonOutForPep(messageOut, getIdFromJson(jsonIn));
    }

    protected static boolean isRegisterRequest(JsonIn jsonIn) {
        MessageContent messageIn = jsonIn.getVolatile().getValue().getCommand().getValue().getMessage();
        return messageIn instanceof RegisterRequest;
    }

    protected static boolean isPepRegistered(JsonIn jsonIn) {
        return (ucsClient.pepMapHas(getIdFromJson(jsonIn)));
    }
}
