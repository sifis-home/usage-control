package it.cnr.iit.pepdht.track;

import it.cnr.iit.utility.dht.jsondht.MessageContent;
import it.cnr.iit.utility.dht.jsondht.endaccess.EndAccessRequest;
import it.cnr.iit.utility.dht.jsondht.endaccess.EndAccessResponse;
import it.cnr.iit.utility.dht.jsondht.reevaluation.ReevaluationResponse;
import it.cnr.iit.utility.dht.jsondht.startaccess.StartAccessRequest;
import it.cnr.iit.utility.dht.jsondht.startaccess.StartAccessResponse;
import it.cnr.iit.utility.dht.jsondht.tryaccess.TryAccessRequest;
import it.cnr.iit.utility.dht.jsondht.tryaccess.TryAccessResponse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AccessTracker {
    private final Map<String, List<String>> msgIdsPerSession = new HashMap<>();
    private final Map<String, MessageInfo> msgFlow = new HashMap<>();

    public boolean add(MessageContent message) {
        if (msgFlow.containsKey(message.getMessage_id())) {
            return mergeMessages(message);
        } else if (message instanceof TryAccessRequest) {
            return addNewMessage(message);
        } else if (message instanceof StartAccessRequest) {
            addMessageId(((StartAccessRequest) message).getSession_id(), message.getMessage_id());
            return addNewMessage(message);
        } else if (message instanceof EndAccessRequest) {
            addMessageId(((EndAccessRequest) message).getSession_id(), message.getMessage_id());
            return addNewMessage(message);
        } else if (message instanceof ReevaluationResponse) {
            addMessageId(((ReevaluationResponse) message).getSession_id(), message.getMessage_id());
            return addNewMessage(message);
        } else {
            throw new IllegalArgumentException("Invalid message");
        }
    }


    private boolean mergeMessages(MessageContent message) {
        MessageInfo messageInfo = msgFlow.get(message.getMessage_id());
        if (message instanceof TryAccessResponse) {
            addMessagePerSession((TryAccessResponse) message);
            messageInfo.merge((TryAccessResponse) message);
        } else if (message instanceof StartAccessResponse) {
            messageInfo.merge((StartAccessResponse) message);
        } else if (message instanceof EndAccessResponse) {
            messageInfo.merge((EndAccessResponse) message);
        }
        return insert(message.getMessage_id(), messageInfo);
    }


    private boolean addNewMessage(MessageContent message) {
        MessageInfo messageInfo = null;
        if (message instanceof TryAccessRequest) {
            messageInfo = MessageInfo.build((TryAccessRequest) message);
        } else if (message instanceof StartAccessRequest) {
            messageInfo = MessageInfo.build((StartAccessRequest) message);
        } else if (message instanceof EndAccessRequest) {
            messageInfo = MessageInfo.build((EndAccessRequest) message);
        } else if (message instanceof ReevaluationResponse) {
            messageInfo = MessageInfo.build((ReevaluationResponse) message);
        }
        return insert(message.getMessage_id(), messageInfo);
    }


    private boolean insert(String messageId, MessageInfo messageInfo) {
        if (messageId == null || messageInfo == null) {
            System.err.println("Insertion in msgFlow aborted: null arguments");
            return false;
        }
        msgFlow.put(messageId, messageInfo);
        return true;
    }


    public List<String> getMessagesPerSession(String sessionId) {
//        Reject.ifBlank( sessionId );
        if (!msgIdsPerSession.containsKey(sessionId)) {
            throw new IllegalArgumentException();
        }
        return msgIdsPerSession.get(sessionId);
    }


    private void addMessagePerSession(TryAccessResponse message) {
        if (message.getEvaluation().equalsIgnoreCase("Permit")) {
            msgIdsPerSession.put(message.getSession_id(), new ArrayList<>());
            addMessageId(message.getSession_id(), message.getMessage_id());
        }
    }


    private void addMessageId(String sessionId, String messageId) {
        msgIdsPerSession.get(sessionId).add(messageId);
    }


    public String getSessionId(String messageId) {
        if (!msgFlow.containsKey(messageId)) {
            throw new IllegalArgumentException();
        }
        MessageInfo messageInfo = msgFlow.get(messageId);
        return messageInfo.getSessionId();
    }


    public void clear() {
        msgFlow.clear();
        msgIdsPerSession.clear();
    }
}
