package it.cnr.iit.ucsdht;

import it.cnr.iit.utility.dht.jsondht.JsonIn;
import it.cnr.iit.utility.dht.jsondht.JsonOut;
import it.cnr.iit.utility.dht.jsondht.MessageContent;
import it.cnr.iit.utility.dht.jsondht.addpolicy.AddPolicyRequest;
import it.cnr.iit.utility.dht.jsondht.addpolicy.AddPolicyResponse;
import it.cnr.iit.utility.dht.jsondht.deletepolicy.DeletePolicyRequest;
import it.cnr.iit.utility.dht.jsondht.deletepolicy.DeletePolicyResponse;
import it.cnr.iit.utility.dht.jsondht.error.ErrorResponse;
import it.cnr.iit.utility.dht.jsondht.getpolicy.GetPolicyRequest;
import it.cnr.iit.utility.dht.jsondht.getpolicy.GetPolicyResponse;
import it.cnr.iit.utility.dht.jsondht.listpolicies.ListPoliciesRequest;
import it.cnr.iit.utility.dht.jsondht.listpolicies.ListPoliciesResponse;

import java.util.Base64;
import java.util.List;

import static it.cnr.iit.ucsdht.ManagerUtils.*;
import static it.cnr.iit.ucsdht.UCSDht.*;
import static it.cnr.iit.utility.dht.DHTUtils.buildOutgoingJsonObject;

public class PAPMessageManager {
    protected static void processMessage(JsonIn jsonIn) {
        MessageContent message = jsonIn.getVolatile().getValue().getCommand().getValue().getMessage();
        if (message instanceof AddPolicyRequest) {
            System.out.println("handle add policy request");
            handleAddPolicyRequest(jsonIn);
        } else if (message instanceof DeletePolicyRequest) {
            System.out.println("handle delete policy request");
            handleDeletePolicyRequest(jsonIn);
        } else if (message instanceof ListPoliciesRequest) {
            System.out.println("handle list policies request");
            handleListPoliciesRequest(jsonIn);
        } else if (message instanceof GetPolicyRequest) {
            System.out.println("handle get policy request");
            handleGetPolicyRequest(jsonIn);
        } else {
            // class not recognized. Handle case
            // this should not happen since the deserialization would already have thrown an exception
            System.err.println("class not recognized. It might be a ResponseMessage");
        }
    }

    private static void handleAddPolicyRequest(JsonIn jsonIn) {
        AddPolicyRequest messageIn = (AddPolicyRequest) getMessageFromJson(jsonIn);

        //String policy = messageIn.getPolicy();
        String policy = new String(Base64.getDecoder().decode(messageIn.getPolicy()));

        JsonOut jsonOut;
        if (!ucsClient.addPolicy(policy)) {
            jsonOut = buildAddPolicyResponseMessage(jsonIn, "KO");
        } else {
            jsonOut = buildAddPolicyResponseMessage(jsonIn, "OK");
        }
        serializeAndSend(jsonOut);
    }


    private static JsonOut buildAddPolicyResponseMessage(JsonIn jsonIn, String code) {

        MessageContent messageOut = new AddPolicyResponse(getMessageIdFromJson(jsonIn), code);
        return buildOutgoingJsonObject(messageOut, getIdFromJson(jsonIn), "topic-name-pap-is-subscribed-to", "topic-uuid-pap-is-subscribed-to", COMMAND_TYPE);
    }


    private static void handleDeletePolicyRequest(JsonIn jsonIn) {
        DeletePolicyRequest messageIn = (DeletePolicyRequest) getMessageFromJson(jsonIn);

        String policyId = messageIn.getPolicy_id();

        JsonOut jsonOut;
        if (!ucsClient.deletePolicy(policyId)) {
            jsonOut = buildDeletePolicyResponseMessage(jsonIn, "KO");
        } else {
            jsonOut = buildDeletePolicyResponseMessage(jsonIn, "OK");
        }
        serializeAndSend(jsonOut);
    }


    private static JsonOut buildDeletePolicyResponseMessage(JsonIn jsonIn, String code) {

        MessageContent messageOut = new DeletePolicyResponse(getMessageIdFromJson(jsonIn), code);
        return buildOutgoingJsonObject(messageOut, getIdFromJson(jsonIn), "topic-name-pap-is-subscribed-to", "topic-uuid-pap-is-subscribed-to", COMMAND_TYPE);
    }


    private static void handleListPoliciesRequest(JsonIn jsonIn) {
        List<String> policyList = ucsClient.listPolicies();

        JsonOut jsonOut = buildListPoliciesResponseMessage(jsonIn, policyList);
        serializeAndSend(jsonOut);
    }


    private static JsonOut buildListPoliciesResponseMessage(JsonIn jsonIn, List<String> policyList) {

        MessageContent messageOut = new ListPoliciesResponse(getMessageIdFromJson(jsonIn), policyList);
        return buildOutgoingJsonObject(messageOut, getIdFromJson(jsonIn), "topic-name-pap-is-subscribed-to", "topic-uuid-pap-is-subscribed-to", COMMAND_TYPE);
    }


    private static void handleGetPolicyRequest(JsonIn jsonIn) {
        GetPolicyRequest messageIn = (GetPolicyRequest) getMessageFromJson(jsonIn);

        String policyId = messageIn.getPolicy_id();

        String policy = ucsClient.getPolicy(policyId);
        String base64Policy = Base64.getEncoder().encodeToString(policy.getBytes());

        JsonOut jsonOut = buildGetPolicyResponseMessage(jsonIn, base64Policy);

        serializeAndSend(jsonOut);
    }


    private static JsonOut buildGetPolicyResponseMessage(JsonIn jsonIn, String policy) {

        MessageContent messageOut = new GetPolicyResponse(getMessageIdFromJson(jsonIn), policy);
        return buildOutgoingJsonObject(messageOut, getIdFromJson(jsonIn), "topic-name-pap-is-subscribed-to", "topic-uuid-pap-is-subscribed-to", COMMAND_TYPE);
    }

    protected static JsonOut buildErrorResponseMessage(JsonIn jsonIn, String description) {
        MessageContent messageOut =
                new ErrorResponse(getMessageIdFromJson(jsonIn), description);
        return buildOutgoingJsonObject(
                messageOut, getIdFromJson(jsonIn), PAP_SUB_TOPIC_NAME, PAP_SUB_TOPIC_UUID, COMMAND_TYPE);
    }
}
