package it.cnr.iit.utility.dht;

import jakarta.websocket.*;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;

@ClientEndpoint
public class DHTClient {

    Session session = null;
    private MessageHandler handler;

    private boolean isLoggingEnabled;

    private WebSocketContainer container;

    private URI wsURI;

    {
        try {
            wsURI = new URI("ws://localhost:3000/ws");
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public DHTClient(URI endpointURI) {
        this.wsURI = endpointURI;
        connect(wsURI);
    }

    public DHTClient() {
        connect(wsURI);
    }

    private void connect(URI endpointURI) throws RuntimeException {
        try {
            container = ContainerProvider.getWebSocketContainer();
            session = container.connectToServer(this, endpointURI);
        } catch (Exception e) {
            throw new RuntimeException("Unable to connect to " + wsURI.toString() + ". " + e.getMessage());
        }
        isLoggingEnabled = true;
    }

    @OnOpen
    public void onOpen(Session session) {
        this.session = session;
        System.out.println("Established websocket connection at " + wsURI);
        System.out.println();
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        System.err.println("There was an error for session: " + session.getId());
        handler.handleError();
    }

    public void addMessageHandler(MessageHandler msgHandler) {
        this.handler = msgHandler;
    }

    @OnMessage
    public void onMessage(String message) {
        handler.handleMessage(message);
        //System.out.println("Received message in client: " + message);
    }


    /**
     * Sends a logging message to the DHT
     *
     */
     public boolean sendMessage(String jsonOut) {

        // Return if DHT logging is not used
        if (!isLoggingEnabled) {
            return false;
        }

        // If a connection is not established yet (which should
        // have been done from the application), do it now
        if (container == null || session == null) {
            try {
                connect(wsURI);
            } catch (RuntimeException e) {
                return false;
            }
        }

        // Now send the payload to the DHT
        try {
            session.getBasicRemote().sendText(jsonOut);
        } catch (IOException e) {
            System.err.println("Error: Sending logging payload to DHT failed");
            Logger.getLogger(DHTClient.class.getName()).log(Level.SEVERE, null, e);
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public interface MessageHandler {
         void handleMessage(String message);
         void handleError();
    }
}