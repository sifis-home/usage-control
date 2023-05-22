package it.cnr.iit.pepdht.track;

import it.cnr.iit.utility.dht.jsondht.EvaluatedMessageContent;
import it.cnr.iit.utility.dht.jsondht.endaccess.EndAccessRequest;
import it.cnr.iit.utility.dht.jsondht.endaccess.EndAccessResponse;
import it.cnr.iit.utility.dht.jsondht.reevaluation.ReevaluationResponse;
import it.cnr.iit.utility.dht.jsondht.startaccess.StartAccessRequest;
import it.cnr.iit.utility.dht.jsondht.startaccess.StartAccessResponse;
import it.cnr.iit.utility.dht.jsondht.tryaccess.TryAccessRequest;
import it.cnr.iit.utility.dht.jsondht.tryaccess.TryAccessResponse;

public class MessageInfo {

    private STATUS status;
    private String sessionId;

    private static MessageInfo build(STATUS status) {
        MessageInfo messageInfo = new MessageInfo();
        messageInfo.setStatus(status);
        return messageInfo;
    }

    public static MessageInfo build(TryAccessRequest message) {
        return build(STATUS.TRYACCESS_SENT);
    }

    public static MessageInfo build(StartAccessRequest message) {
        MessageInfo messageInfo = build(STATUS.STARTACCESS_SENT);
        messageInfo.setSessionId(message.getSession_id());
        return messageInfo;
    }

    public static MessageInfo build(EndAccessRequest message) {
        MessageInfo messageInfo = build(STATUS.ENDACCESS_SENT);
        messageInfo.setSessionId(message.getSession_id());
        return messageInfo;
    }

    public static MessageInfo build(ReevaluationResponse message) {
        MessageInfo messageInfo = null;
        if (message.getEvaluation().equalsIgnoreCase("Permit")) {
            messageInfo = build(STATUS.SESSION_RESUMED);
        } else {
            messageInfo = build(STATUS.REVOKED);
        }
        messageInfo.setSessionId(message.getSession_id());
        return messageInfo;
    }


    public void merge(EvaluatedMessageContent message, STATUS status, STATUS positiveStatus, STATUS negativeStatus) {
        if (this.getStatus() != status) {
            throw new IllegalArgumentException("Wrong flow of messages!! \n status is: " + this.getStatus());
        }
        String evaluation = message.getEvaluation();
        if (evaluation.equalsIgnoreCase("Permit")) {
            this.setStatus(positiveStatus);
        } else {
            this.setStatus(negativeStatus);
        }
    }

    public void merge(TryAccessResponse message) {
        merge(message, STATUS.TRYACCESS_SENT, STATUS.TRYACCESS_PERMIT, STATUS.TRYACCESS_DENY);
        this.setSessionId(message.getSession_id());
    }

    public void merge(StartAccessResponse message) {
        merge(message, STATUS.STARTACCESS_SENT, STATUS.STARTACCESS_PERMIT, STATUS.STARTACCESS_DENY);
    }

    public void merge(EndAccessResponse message) {
        merge(message, STATUS.ENDACCESS_SENT, STATUS.ENDACCESS_PERMIT, STATUS.ENDACCESS_DENY);
    }


    public STATUS getStatus() {
        return status;
    }

    public void setStatus(STATUS status) {
        this.status = status;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
}
