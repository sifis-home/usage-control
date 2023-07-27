package it.cnr.iit.utility.dht.jsonvolatile;

// example of try access request sent by a PEP
// {
//  "RequestPubMessage": {
//    "value": {
//      "timestamp": 1684402025463,
//      "command": {
//        "command_type": "pep-command",
//        "value": {
//          "message": {
//            "purpose": "TRY",
//            "request": "request",
//            "policy": "policy"
//          },
//          "pep_id": "pep-0",
//          "message_id": "random123-msg_id",
//          "topic_name": "topic-name",
//          "topic_uuid": "topic-uuid-the-ucs-is-subscribed-to"
//        }
//      }
//    }
//  }
// }
public class JsonOut {

    private RequestPubMessage RequestPubMessage;

    public JsonOut() {
    }

    public JsonOut(RequestPubMessage requestPubMessage) {
        this.RequestPubMessage = requestPubMessage;
    }

    public void setRequestPubMessage(RequestPubMessage requestPubMessage) {
        RequestPubMessage = requestPubMessage;
    }

    public RequestPubMessage getRequestPubMessage() {
        return RequestPubMessage;
    }

}