package it.cnr.iit.utility.dht.jsondht;

// {
//  "RequestPubMessage": {
//    "value": {
//      "timestamp": 1684316923041,
//      "command": {
//        "command_type": "pep-command",
//        "value": {
//          "message": {
//            "type": "try_access_request",
//            "request": "iVBORw0KGgoAABGdBTUEAALGPC/xhBQAAAAlwSFlzA...",
//            "policy": null
//          },
//          "pep_id": "pep_id",
//          "message_id": "bf2cbe6b-c309-4ad6-b6b3-f300596f8d5a",
//          "topic_name": "topic_name_XXX",
//          "topic_uuid": "pub_topic_uuid"
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