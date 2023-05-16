package it.cnr.iit.json;

import com.github.tomakehurst.wiremock.common.Json;

// {
//   "RequestPubMessage":{
//      "value":{
//         "timestamp":2839812938912,
//         "command":{
//            "command_type":"pep_command",
//            "value":{
//               "try_request":{
//                  "request":"iVBORw0KGgoAABGdBTUEAALGPC/xhBQAAAAlwSFlzA...",
//                  "policy":null
//               },
//               "pep_id":"pep1",
//               "message_id":"23423-d23d23-234a",
//               "topic_name":"my_topic",
//               "topic_uuid":"topic_uuid"
//            }
//         }
//      }
//   }
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