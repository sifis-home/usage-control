package it.cnr.iit.utility.dht;

import jakarta.websocket.*;

import java.net.URI;

@ClientEndpoint
public class DHTPersistentMessageClient {


    private final Object lock = new Object();
    private String response;
    private Session session;
    private final String uri;

    public DHTPersistentMessageClient(String uri) {
        this.uri = uri;
    }

    @OnOpen
    public void onOpen(Session session) {
        this.session = session; // Store the session for later use
        System.out.println("WebSocket connected");
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        String truncatedMessage;
        if (message.length() > 50) {
            truncatedMessage = message.substring(0,49) + "... (remainder omitted for readability)";
        } else {
            truncatedMessage = message;
        }
        System.out.println("Received response: " + truncatedMessage);
        response = message;
        synchronized (lock) {
            lock.notify(); // Notify the waiting thread that the response has been received.
        }
    }

    @OnClose
    public void onClose(Session session) {
        System.out.println("Session " + session.getId() + " closed.");
    }

    public void closeConnection() {
        try {
            if (session != null) {
                session.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String sendRequestAndWaitForResponse(String request) {

        try {
            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
            container.connectToServer(this, new URI(this.uri));

            synchronized (lock) {
                this.session.getBasicRemote().sendText(request); // Send the request using the stored session
                lock.wait(); // Wait for the response to be received
            }
            return response;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}