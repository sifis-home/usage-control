package it.cnr.iit.ucsdht;

import it.cnr.iit.ucs.pip.AbstractPIPWebSocket;
import it.cnr.iit.ucs.pipreader.PIPReader;
import it.cnr.iit.ucs.piptime.PIPTime;
import it.cnr.iit.ucs.pipwebsocket.*;
// Do not remove "import it.cnr.iit.ucs.pipwebsocket.*;"
// The IDE identifies it as an unused import, but it is needed to recognize classes that extend AbstractPIPWebSocket
import it.cnr.iit.ucsdht.properties.UCSDhtPipProperties;
import it.cnr.iit.utility.JsonUtility;
import it.cnr.iit.utility.dht.jsonvolatile.JsonIn;
import it.cnr.iit.utility.dht.jsonvolatile.JsonOut;
import it.cnr.iit.utility.dht.jsonvolatile.MessageContent;
import it.cnr.iit.utility.dht.jsonvolatile.addpip.AddPipRequest;
import it.cnr.iit.utility.dht.jsonvolatile.addpip.AddPipResponse;
import it.cnr.iit.utility.dht.jsonvolatile.error.ErrorResponse;

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
            System.out.println("Handling add Pip request...");
            handleAddPipRequest(jsonIn);
        } else {
            // class not recognized. Handle case
            // this should not happen since the deserialization would already have thrown an exception
            System.err.println("Class not recognized. It might be a ResponseMessage");
        }
    }

    // todo: at the moment, we support only the addition of a PIP monitoring one attribute only.
    //       Nonetheless, a PIP could monitor multiple attributes.
    //       To add PIP monitoring multiple attributes, we should change the json schema.
    private static void handleAddPipRequest(JsonIn jsonIn) {
        AddPipRequest messageIn = (AddPipRequest) getMessageFromJson(jsonIn);

        String pipType = messageIn.getPip_type();

        Class<?> cls;
        try {
            cls = Class.forName(pipType);
        } catch (ClassNotFoundException e) {
            JsonOut jsonOut = buildErrorResponseMessage(jsonIn, "PIP class " + pipType + " not recognized");
            serializeAndSend(jsonOut);
            return;
        }

        if (cls == PIPReader.class) {
            handleAddPipReaderRequest(jsonIn);
        } else if (cls == PIPTime.class) {
            handleAddPipTimeRequest(jsonIn);
        } else if (AbstractPIPWebSocket.class.isAssignableFrom(cls)) {
            handleAddPipWebSocketRequest(jsonIn);
        } else {
            JsonOut jsonOut = buildErrorResponseMessage(jsonIn, "PIP class " + pipType + " not recognized");
            serializeAndSend(jsonOut);
        }

    }


    private static void handleAddPipWebSocketRequest(JsonIn jsonIn) {
        AddPipRequest messageIn = (AddPipRequest) getMessageFromJson(jsonIn);

        String pipType = messageIn.getPip_type();
        String attributeId = messageIn.getAttribute_id();
        String category = messageIn.getCategory();
        String dataType = messageIn.getData_type();
        String attributePath = pipsDir + File.separator + getIdFromJson(jsonIn) + File.separator;
        long refreshRate = messageIn.getRefresh_rate();
        Map<String, String> additionalProperties = messageIn.getAdditional_properties();

        // dhtUri, topicName, and topicUuid are stored in the additional_properties field
        String dhtUri = additionalProperties.get("dhtUri");
        String topicName = additionalProperties.get("topicName");
        String topicUuid = additionalProperties.get("topicUuid");

        // create additionalProperties with the required fields only.
        // this is to ignore other properties possibly specified in the request.
        Map<String, String> checkedAdditionalProperties = new HashMap<>();
        checkedAdditionalProperties.put("dhtUri", dhtUri);
        checkedAdditionalProperties.put("topicName", topicName);
        checkedAdditionalProperties.put("topicUuid", topicUuid);

        JsonOut jsonOut;

        boolean isAdded = ucsClient.addPip(
                pipType, attributeId, category, dataType, refreshRate, checkedAdditionalProperties);

        if (!isAdded) {
            jsonOut = buildAddPipResponseMessage(jsonIn, "KO");
        } else {
            Utils.createDir(new File(attributePath));
            jsonOut = buildAddPipResponseMessage(jsonIn, "OK");
        }
        serializeAndSend(jsonOut);

        if (isAdded) {
            serializePipToFile(pipType, getIdFromJson(jsonIn), attributeId, category,
                    dataType, refreshRate, checkedAdditionalProperties);
        }

    }


    private static void handleAddPipReaderRequest(JsonIn jsonIn) {
        AddPipRequest messageIn = (AddPipRequest) getMessageFromJson(jsonIn);

        String pipType = messageIn.getPip_type();
        String attributeId = messageIn.getAttribute_id();
        String category = messageIn.getCategory();
        String dataType = messageIn.getData_type();
        String attributePath = pipsDir + File.separator + getIdFromJson(jsonIn) + File.separator;
        long refreshRate = messageIn.getRefresh_rate();
        Map<String, String> additionalProperties = messageIn.getAdditional_properties();

        // fileName and its value are stored in the additional_properties field
        String fileName = additionalProperties.get(attributeId);
        String attributeValue = additionalProperties.get(fileName);

        // prepend the attribute path
        Map<String, String> absolutePathAdditionalProperties = new HashMap<>();
        absolutePathAdditionalProperties.put(attributeId, attributePath + fileName);
        absolutePathAdditionalProperties.put(attributePath + fileName, attributeValue);

        // create additionalProperties with the filename and attribute value only.
        // this is to ignore other properties possibly specified in the request.
        Map<String, String> checkedAdditionalProperties = new HashMap<>();
        checkedAdditionalProperties.put(attributeId, fileName);
        checkedAdditionalProperties.put(fileName, attributeValue);

        JsonOut jsonOut;

        boolean isAdded = ucsClient.addPip(
                pipType, attributeId, category, dataType, refreshRate, absolutePathAdditionalProperties);

        if (!isAdded) {
            jsonOut = buildAddPipResponseMessage(jsonIn, "KO");
        } else {
            Utils.createDir(new File(attributePath));
            setAttributeValue(attributePath + fileName, attributeValue);
            jsonOut = buildAddPipResponseMessage(jsonIn, "OK");
        }
        serializeAndSend(jsonOut);

        if (isAdded) {
            serializePipToFile(pipType, getIdFromJson(jsonIn), attributeId, category,
                    dataType, refreshRate, checkedAdditionalProperties);
        }
    }


    private static void handleAddPipTimeRequest(JsonIn jsonIn) {
        AddPipRequest messageIn = (AddPipRequest) getMessageFromJson(jsonIn);

        String pipType = messageIn.getPip_type();
        String attributeId = messageIn.getAttribute_id();
        String category = messageIn.getCategory();
        String dataType = messageIn.getData_type();
        String attributePath = pipsDir + File.separator + getIdFromJson(jsonIn) + File.separator;
        long refreshRate = messageIn.getRefresh_rate();

        JsonOut jsonOut;

        boolean isAdded = ucsClient.addPip(
                pipType, attributeId, category, dataType, refreshRate, new HashMap<>());

        if (!isAdded) {
            jsonOut = buildAddPipResponseMessage(jsonIn, "KO");
        } else {
            Utils.createDir(new File(attributePath));
            jsonOut = buildAddPipResponseMessage(jsonIn, "OK");
        }
        serializeAndSend(jsonOut);

        if (isAdded) {
            serializePipToFile(pipType, getIdFromJson(jsonIn), attributeId, category,
                    dataType, refreshRate, new HashMap<>());
        }
    }


    private static JsonOut buildAddPipResponseMessage(JsonIn jsonIn, String code) {

        MessageContent messageOut = new AddPipResponse(getMessageIdFromJson(jsonIn), code);
        return buildOutgoingJsonObject(messageOut, getIdFromJson(jsonIn),
                "topic-name-pip-is-subscribed-to", "topic-uuid-pip-is-subscribed-to", COMMAND_TYPE);
    }

    protected static JsonOut buildErrorResponseMessage(JsonIn jsonIn, String description) {
        MessageContent messageOut =
                new ErrorResponse(getMessageIdFromJson(jsonIn), description);
        return buildOutgoingJsonObject(
                messageOut, getIdFromJson(jsonIn), PIP_SUB_TOPIC_NAME, PIP_SUB_TOPIC_UUID, COMMAND_TYPE);
    }


    protected static void serializePipToFile(String name, String id, String attributeId, String category,
                                             String dataType, long refreshRate, Map<String, String> additionalProperties) {
        UCSDhtPipProperties pipProperties = new UCSDhtPipProperties();

        pipProperties.setName(name);
        pipProperties.setId(id);
        pipProperties.addAttribute(attributeId, category, dataType);
        pipProperties.setRefreshRate(refreshRate);
        pipProperties.setAdditionalProperties(additionalProperties);

        JsonUtility.dumpObjectToJsonFile(pipProperties,
                pipsDir + File.separator + id + File.separator + id + ".json", true);
    }
}
