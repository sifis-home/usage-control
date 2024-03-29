package it.cnr.iit.ucsdht;

import it.cnr.iit.ucs.message.endaccess.EndAccessResponseMessage;
import it.cnr.iit.ucs.message.startaccess.StartAccessResponseMessage;
import it.cnr.iit.ucs.message.tryaccess.TryAccessResponseMessage;
import it.cnr.iit.ucs.properties.components.PepProperties;
import it.cnr.iit.ucsdht.properties.UCSDhtPepProperties;
import it.cnr.iit.utility.JsonUtility;
import it.cnr.iit.utility.dht.jsonvolatile.JsonIn;
import it.cnr.iit.utility.dht.jsonvolatile.JsonOut;
import it.cnr.iit.utility.dht.jsonvolatile.MessageContent;
import it.cnr.iit.utility.dht.jsonvolatile.endaccess.EndAccessRequest;
import it.cnr.iit.utility.dht.jsonvolatile.endaccess.EndAccessResponse;
import it.cnr.iit.utility.dht.jsonvolatile.error.ErrorResponse;
import it.cnr.iit.utility.dht.jsonvolatile.registration.RegisterRequest;
import it.cnr.iit.utility.dht.jsonvolatile.registration.RegisterResponse;
import it.cnr.iit.utility.dht.jsonvolatile.startaccess.StartAccessRequest;
import it.cnr.iit.utility.dht.jsonvolatile.startaccess.StartAccessResponse;
import it.cnr.iit.utility.dht.jsonvolatile.tryaccess.TryAccessRequest;
import it.cnr.iit.utility.dht.jsonvolatile.tryaccess.TryAccessResponse;

import java.io.File;
import java.util.Base64;

import static it.cnr.iit.ucsdht.ManagerUtils.*;
import static it.cnr.iit.ucsdht.UCSDht.*;
import static it.cnr.iit.utility.dht.DHTUtils.buildOutgoingJsonObject;

public class PEPMessageManager {

    protected static void processMessage(JsonIn jsonIn) {
        MessageContent message = jsonIn.getVolatile().getValue().getCommand().getValue().getMessage();
        if (message instanceof RegisterRequest) {
            // handle register request
            System.out.println("Handling register request...");
            handleRegisterRequest(jsonIn);
        } else if (message instanceof TryAccessRequest) {
            // handle try access request
            System.out.println("Handling try access request...");
            handleTryAccessRequest(jsonIn);
        } else if (message instanceof StartAccessRequest) {
            // handle start access request
            System.out.println("Handling start access request...");
            handleStartAccessRequest(jsonIn);
        } else if (message instanceof EndAccessRequest) {
            // handle end access request
            System.out.println("Handling end access request...");
            handleEndAccessRequest(jsonIn);
        } else {
            // class not recognized. Handle case
            // this should not happen since the deserialization would already have thrown an exception
            System.err.println("Class not recognized. It might be a ResponseMessage");
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
        boolean isAdded = false;

        if (!isPepRegistered(jsonIn)) {
            RegisterRequest messageIn = (RegisterRequest) getMessageFromJson(jsonIn);
            String pepId = getIdFromJson(jsonIn);
            String subTopicName = messageIn.getSub_topic_name();
            String subTopicUuid = messageIn.getSub_topic_uuid();

            isAdded = ucsClient.addPep(pepId, subTopicName, subTopicUuid);
            if (!isAdded) {
                jsonOut = buildRegisterResponseMessage(jsonIn, "KO");
                serializeAndSend(jsonOut);
                return;
            } else {
                jsonOut = buildRegisterResponseMessage(jsonIn, "OK");
                serializeAndSend(jsonOut);
                serializePepToFile(pepId, subTopicName, subTopicUuid);
                return;
            }
        }
        jsonOut = buildRegisterResponseMessage(jsonIn, "OK");
        serializeAndSend(jsonOut);
    }

    private static void serializePepToFile(String id, String subTopicName, String subTopicUuid) {
        UCSDhtPepProperties pepProperties = new UCSDhtPepProperties();
        pepProperties.setId(id);
        pepProperties.setSubTopicName(subTopicName);
        pepProperties.setSubTopicUuid(subTopicUuid);

        JsonUtility.dumpObjectToJsonFile(pepProperties,
                pepsDir + File.separator + id + ".json", true);
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
