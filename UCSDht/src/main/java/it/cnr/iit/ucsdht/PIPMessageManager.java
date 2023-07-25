package it.cnr.iit.ucsdht;

import it.cnr.iit.ucsdht.properties.UCSDhtPipReaderProperties;
import it.cnr.iit.utility.JsonUtility;
import it.cnr.iit.utility.dht.jsondht.JsonIn;
import it.cnr.iit.utility.dht.jsondht.JsonOut;
import it.cnr.iit.utility.dht.jsondht.MessageContent;
import it.cnr.iit.utility.dht.jsondht.addpip.AddPipRequest;
import it.cnr.iit.utility.dht.jsondht.addpip.AddPipResponse;
import it.cnr.iit.utility.dht.jsondht.error.ErrorResponse;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static it.cnr.iit.ucsdht.ManagerUtils.*;
import static it.cnr.iit.ucsdht.UCSDht.*;
import static it.cnr.iit.utility.dht.DHTUtils.buildOutgoingJsonObject;

public class PIPMessageManager {
    protected static void processMessage(JsonIn jsonIn) {
        MessageContent message = jsonIn.getVolatile().getValue().getCommand().getValue().getMessage();
        if (message instanceof AddPipRequest) {
            System.out.println("handle add Pip request");
            handleAddPipRequest(jsonIn);
        } else {
            // class not recognized. Handle case
            // this should not happen since the deserialization would already have thrown an exception
            System.err.println("class not recognized. It might be a ResponseMessage");
        }
    }

    private static void handleAddPipRequest(JsonIn jsonIn) {
        AddPipRequest messageIn = (AddPipRequest) getMessageFromJson(jsonIn);

        String pipType = messageIn.getPip_type();
        String attributeId = messageIn.getAttribute_id();
        String category = messageIn.getCategory();
        String dataType = messageIn.getData_type();
        String attributeValue = messageIn.getAttribute_value();
        String attributePath = pipsDir + File.separator + getIdFromJson(jsonIn) + File.separator;
        String fileName = messageIn.getFile_name();
        long refreshRate = messageIn.getRefresh_rate();

        JsonOut jsonOut;

        boolean isAdded = ucsClient.addPip(
                pipType, attributeId, category, dataType, attributePath, fileName, refreshRate);

        if (!isAdded) {
            jsonOut = buildAddPipResponseMessage(jsonIn, "KO");
        } else {
            Utils.createDir(new File(attributePath));
            setAttributeValue(attributePath + fileName, attributeValue);
            jsonOut = buildAddPipResponseMessage(jsonIn, "OK");
        }
        serializeAndSend(jsonOut);

        if (isAdded) {
            serializePipToFile(getIdFromJson(jsonIn), attributeId, category,
                    dataType, fileName, refreshRate, attributeValue);
        }
    }


    private static JsonOut buildAddPipResponseMessage(JsonIn jsonIn, String code) {

        MessageContent messageOut = new AddPipResponse(getMessageIdFromJson(jsonIn), code);
        return buildOutgoingJsonObject(messageOut, getIdFromJson(jsonIn), "topic-name-pip-is-subscribed-to", "topic-uuid-pip-is-subscribed-to", COMMAND_TYPE);
    }

    protected static JsonOut buildErrorResponseMessage(JsonIn jsonIn, String description) {
        MessageContent messageOut =
                new ErrorResponse(getMessageIdFromJson(jsonIn), description);
        return buildOutgoingJsonObject(
                messageOut, getIdFromJson(jsonIn), PIP_SUB_TOPIC_NAME, PIP_SUB_TOPIC_UUID, COMMAND_TYPE);
    }

    // method specific for PIPReader. Need to make it general if we are going to use different PIP types
    // note that when serializing, we specify as "FILE_PATH" the file name only. It's up to who loads
    // the PIP from the json file to prepend the correct path.
    protected static void serializePipToFile(String id, String attributeId, String category,
                                             String dataType, String fileName, long refreshRate, String attributeValue) {
        UCSDhtPipReaderProperties pipReaderProperties = new UCSDhtPipReaderProperties();

        pipReaderProperties.setId(id);
        pipReaderProperties.addAttribute(attributeId, category, dataType, fileName);
        pipReaderProperties.setRefreshRate(refreshRate);
        Map<String, String> additionalProperties = new HashMap<>();
        additionalProperties.put("value", attributeValue);
        pipReaderProperties.setAdditionalProperties(additionalProperties);

        JsonUtility.dumpObjectToJsonFile(pipReaderProperties,
                pipsDir + File.separator + id + File.separator + id + ".json", true);
    }
}
