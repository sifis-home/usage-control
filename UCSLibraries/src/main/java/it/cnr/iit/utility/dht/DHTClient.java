package it.cnr.iit.utility.dht;

import jakarta.websocket.ClientEndpoint;
import jakarta.websocket.ContainerProvider;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.WebSocketContainer;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Juneau
 */
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
            //container.setDefaultMaxTextMessageBufferSize(200);
            session = container.connectToServer(this, endpointURI);
        } catch (Exception e) {
            throw new RuntimeException("Unable to connect to " + wsURI.toString() + ". " + e.getMessage());
        }
        isLoggingEnabled = true;
    }

    @OnOpen
    public void onOpen(Session session) {
        this.session = session;
//        try {
//            //session.getBasicRemote().sendText("Opening connection");
//        } catch (IOException ex){
//            System.out.println(ex);
//        }
        System.out.println("Connected to " + wsURI);
    }

    public void addMessageHandler(MessageHandler msgHandler) {
        this.handler = msgHandler;
    }

    @OnMessage
    public void onMessage(String message) {
        handler.handleMessage(message);
        //System.out.println("Received message in client: " + message);
    }

//    public void sendMessage(String message) {
//        try {
//            this.session.getBasicRemote().sendText(message);
//        } catch (IOException ex) {
//            Logger.getLogger(DHTClient.class.getName()).log(Level.SEVERE, null, ex);
//        }
//    }


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
    }
}
//
//import java.io.IOException;
//import java.net.URI;
//import java.net.URISyntaxException;
//
//import jakarta.websocket.*;
//import org.glassfish.tyrus.client.ClientManager;
//
//@ClientEndpoint
//public class DHTClient {
//
//    private static ClientManager dhtClient = null;
//    private static Session session = null;
//
//    private static String websocketUri = "ws://localhost:3000/ws";
//
//    private static boolean isLoggingEnabled;
//
//    private MessageHandler handler;
//
//    public DHTClient(URI endpointURI) {
//        try {
//            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
//            container.connectToServer(this, endpointURI);
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
//    }
//
//    /**
//     * Sends a logging message to the DHT
//     *
//     */
//     public void sendMessage(String jsonOut) {
//
//        // Return if DHT logging is not used
//        if (!isLoggingEnabled) {
//            return;
//        }
//
//        // If a connection is not established yet (which should
//        // have been done from the application), do it now
//        if (dhtClient == null || session == null) {
//            boolean dhtConnected = establishConnection();
//
//            // If the connection failed to be established, return
//            if (!dhtConnected) {
//                return;
//            }
//        }
//
//        // Now send the payload to the DHT
//        try {
//            session.getBasicRemote().sendText(jsonOut);
//        } catch (IOException e) {
//            System.err.println("Error: Sending logging payload to DHT failed");
//            e.printStackTrace();
//        }
//    }
//
//
//    /**
//     * Enable or disable logging to the DHT
//     *
//     * @param logging true/false
//     */
//    static public void setLogging(boolean logging) {
//        isLoggingEnabled = logging;
//    }
//
//    /**
//     * Retrieve logging status
//     *
//     */
//    static public boolean isLoggingEnabled() {
//        return isLoggingEnabled;
//    }
//
//    /**
//     * Retrieve the client instance connected to the DHT.
//     *
//     * @return the client
//     */
//    public static ClientManager getClientInstance() {
//
//        if (dhtClient == null || session == null) {
//            establishConnection();
//        }
//
//        return dhtClient;
//    }
//
//    /**
//     * Retrieve the session instance associated with the connection to the DHT.
//     *
//     * @return the session
//     */
//    public static Session getSessionInstance() {
//
//        if (dhtClient == null || session == null) {
//            establishConnection();
//        }
//
//        return session;
//    }
//
//    /**
//     * Get the URI used for the WebSocket connection to the DHT.
//     *
//     * @return the URI
//     */
//    public static String getWebsocketUri() {
//        return websocketUri;
//    }
//
//    /**
//     * Set the URI to use for the WebSocket connection to the DHT.
//     *
//     * @param websocketUri the desired URI
//     */
//    public static void setWebsocketUri(String websocketUri) {
//        DHTClient.websocketUri = websocketUri;
//    }
//
//    /**
//     * Establish the connection to the DHT.
//     *
//     * @return if the connection was successfully established
//     */
//    public static boolean establishConnection() {
//
//        System.out.println("Connecting to DHT for logging");
//
//        //CountDownLatch latch = new CountDownLatch(1);
//        dhtClient = ClientManager.createClient();
//        try {
//            URI uri = new URI(websocketUri);
//            session = dhtClient.connectToServer(DHTClient.class, uri);
//            //latch.await();
//        } catch (DeploymentException | URISyntaxException | IOException e) {
//            System.err.println("Error: Failed to connect to DHT for logging");
//            e.printStackTrace();
//            return false;
//        }
//
//        return true;
//    }
//
//    /**
//     * Establish the connection to the DHT.
//     *
//     * @param dhtWebsocketUri the URI to connect to the DHT using WebSocket
//     * @return if the connection was successfully established
//     */
//    public static boolean establishConnection(String dhtWebsocketUri) {
//        setWebsocketUri(dhtWebsocketUri);
//        return establishConnection();
//    }
//
//    @OnOpen
//    public void onOpen(Session session) {
//        System.out.println("--- Connected " + session.getId());
//
//    }
//
//    @OnMessage
//    public String onMessage(String message, Session session) {
//        // Do nothing for incoming messages from DHT
//        System.out.println("Received message");
//        return null;
//    }
//
//    @OnClose
//    public void onClose(Session session, CloseReason closeReason) {
//        System.out.println("Session " + session.getId() + " closed because " + closeReason);
//        //    latch.countDown();
//    }
//
//    public void addMessageHandler(MessageHandler msgHandler) {
//        this.handler = msgHandler;
//    }
//
//    public static interface MessageHandler {
//
//        public void handleMessage(String message);
//    }
//}
