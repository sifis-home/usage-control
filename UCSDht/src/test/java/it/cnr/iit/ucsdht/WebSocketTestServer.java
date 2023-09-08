package it.cnr.iit.ucsdht;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.*;

import java.io.IOException;

@WebSocket
public class WebSocketTestServer {

    public Session session;


    @OnWebSocketConnect
    public void onConnect(Session session) {
        this.session = session;
        System.out.println("WebSocket connected: " + session.getRemoteAddress());
    }

    @OnWebSocketClose
    public void onClose(int statusCode, String reason) {
        System.out.println("WebSocket closed: " + reason);
    }

    @OnWebSocketMessage
    public void onMessage(String message) throws IOException {
        System.out.println("Received message: " + message);


        if (message.contains("SIFIS:UCS") && message.contains("status")) {
            String response;
            if (message.contains("RequestPostTopicUUID")) {
            // The UCS uploaded a new status, and it does expect a response
                response = StatusMock.getInstance().getResponseToUploadStatus();
            } else if (message.contains("RequestGetTopicUUID")) {
            // The UCS asked to download the status
                response = StatusMock.getInstance().getResponseToDownloadStatus();
            } else {
                System.err.println("Message not recognized");
                return;
            }
            System.out.println("\n\n\n\n" + response + "\n\n\n\n");
            session.getRemote().sendString(response);
        } else {
            // The UCS sent a response, such as one with purpose ADD_PIP_RESPONSE
            // We save the received message at the MessageReceiver
            // so that tests can retrieve it
            MessageReceiver.getInstance().setReceivedMessage(message);
        }
    }
}