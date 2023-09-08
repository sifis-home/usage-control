package it.cnr.iit.ucsdht;

import com.google.gson.GsonBuilder;
import it.cnr.iit.utility.dht.jsonvolatile.JsonIn;
import it.cnr.iit.utility.errorhandling.exception.PreconditionException;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.websocket.server.WebSocketHandler;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;
import org.junit.*;

import static it.cnr.iit.utility.dht.DHTUtils.deserializeIncomingJson;
import static java.lang.Thread.sleep;

import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class TestPIPManager {

    private static final int TEST_SERVER_PORT = 3001;
    private static final  String TEST_SERVER_HOST = "localhost";

    private Server server;

    @Before
    public void setUp() throws Exception {

        // Run the test server endpoint
        startTestServer(TEST_SERVER_HOST, TEST_SERVER_PORT);

        String[] args = new String[]{"--dht", "ws://" + TEST_SERVER_HOST + ":" + TEST_SERVER_PORT , "--hard-reset"};
        UCSDht.main(args);
    }

    @After
    public void teardown() throws Exception {
        // Stop the server
        server.stop();
        server.destroy();
    }

    // Start the test WebSocket server
    private void startTestServer(String host, int port) throws Exception {
        server = new Server();

        // Create a WebSocket server connector with the desired URL and port
        ServerConnector connector = new ServerConnector(server);
        connector.setHost(host);
        connector.setPort(port);
        server.addConnector(connector);

        // Add WebSocket handler
        WebSocketHandler wsHandler = new WebSocketHandler() {
            @Override
            public void configure(WebSocketServletFactory factory) {
                factory.register(WebSocketTestServer.class);
            }
        };
        server.setHandler(wsHandler);

        // Start the server
        server.start();
    }

    @Test
    public void testPIPManager() throws InterruptedException {

        addPipTime();

        // Add the same PIP again. This PIP wants to monitor an attributeId that is already monitored
        PreconditionException exception = assertThrows(PreconditionException.class,
                this::addPipTime);
        assertTrue(exception.getMessage().contains("Another PIP is already monitoring " +
                "the same attributeId: expected false, was true"));

        addPipWithTypeNotRecognized();
        addPipWebsocketLampStatus();
        addPipWebsocketLamps();
        addPipReader();
    }

    public void addPipTime() throws InterruptedException {

        // Set the received message at the MessageReceiver to be null
        MessageReceiver.getInstance().setReceivedMessage(null);

        String incomingJson =
                "{\n" +
                "  \"Volatile\": {\n" +
                "    \"value\": {\n" +
                "      \"timestamp\": 1693905111450,\n" +
                "      \"command\": {\n" +
                "        \"command_type\": \"pip-command\",\n" +
                "        \"value\": {\n" +
                "          \"message\": {\n" +
                "            \"purpose\": \"ADD_PIP\",\n" +
                "            \"message_id\": \"testSuccessAddPipTime\",\n" +
                "            \"pip_type\": \"it.cnr.iit.ucs.piptime.PIPTime\",\n" +
                "            \"attribute_id\": \"urn:oasis:names:tc:xacml:1.0:environment:current-time\",\n" +
                "            \"category\": \"urn:oasis:names:tc:xacml:3.0:attribute-category:environment\",\n" +
                "            \"data_type\": \"http://www.w3.org/2001/XMLSchema#time\",\n" +
                "            \"refresh_rate\": 10000\n" +
                "          },\n" +
                "          \"id\": \"pip-time\",\n" +
                "          \"topic_name\": \"topic-name\",\n" +
                "          \"topic_uuid\": \"topic-uuid-the-ucs-is-subscribed-to\"\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}";
        JsonIn jsonIn = deserializeIncomingJson(incomingJson);
        System.out.println(new GsonBuilder()
                .disableHtmlEscaping()
                .serializeNulls()
                .setPrettyPrinting()
                .create()
                .toJson(jsonIn));

        PIPMessageManager.processMessage(jsonIn);

        // wait some time if the WebSocketTestServer has not yet propagated
        // the message to the MessageReceiver
        if (MessageReceiver.getInstance().getReceivedMessage() == null) {
            sleep(400);
        }

        String ucsResponse = MessageReceiver.getInstance().getReceivedMessage();
        assertTrue(ucsResponse.contains("\"purpose\":\"ADD_PIP_RESPONSE\""));
        assertTrue(ucsResponse.contains("\"code\":\"OK\""));
        assertTrue(ucsResponse.contains("\"message_id\":\"testSuccessAddPipTime\""));

    }

    public void addPipWithTypeNotRecognized() throws InterruptedException {

        // Set the received message at the MessageReceiver to be null
        MessageReceiver.getInstance().setReceivedMessage(null);

        String incomingJson =
                "{\n" +
                "  \"Volatile\": {\n" +
                "    \"value\": {\n" +
                "      \"timestamp\": 1693905111450,\n" +
                "      \"command\": {\n" +
                "        \"command_type\": \"pip-command\",\n" +
                "        \"value\": {\n" +
                "          \"message\": {\n" +
                "            \"purpose\": \"ADD_PIP\",\n" +
                "            \"message_id\": \"testFailAddPip_whenPipTypeNotRecognized\",\n" +
                "            \"pip_type\": \"it.cnr.iit.ucs.not.existent.pip\",\n" +
                "            \"attribute_id\": \"urn:oasis:names:tc:xacml:1.0:environment:current-time\",\n" +
                "            \"category\": \"urn:oasis:names:tc:xacml:3.0:attribute-category:environment\",\n" +
                "            \"data_type\": \"http://www.w3.org/2001/XMLSchema#time\",\n" +
                "            \"refresh_rate\": 10000\n" +
                "          },\n" +
                "          \"id\": \"pip-time\",\n" +
                "          \"topic_name\": \"topic-name\",\n" +
                "          \"topic_uuid\": \"topic-uuid-the-ucs-is-subscribed-to\"\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}";
        JsonIn jsonIn = deserializeIncomingJson(incomingJson);
        System.out.println(new GsonBuilder()
                .disableHtmlEscaping()
                .serializeNulls()
                .setPrettyPrinting()
                .create()
                .toJson(jsonIn));

        System.out.println("Received response: " + MessageReceiver.getInstance().getReceivedMessage());
        PIPMessageManager.processMessage(jsonIn);

        // wait some time if the WebSocketTestServer has not yet propagated
        // the message to the MessageReceiver
        if (MessageReceiver.getInstance().getReceivedMessage() == null) {
            sleep(400);
        }

        String ucsResponse = MessageReceiver.getInstance().getReceivedMessage();
        assertTrue(ucsResponse.contains("\"purpose\":\"ERROR_RESPONSE\""));
        assertTrue(ucsResponse.contains("\"description\":\"PIP class it.cnr.iit.ucs.not.existent.pip not recognized\""));
        assertTrue(ucsResponse.contains("\"message_id\":\"testFailAddPip_whenPipTypeNotRecognized\""));

    }

    public void addPipWebsocketLampStatus() throws InterruptedException {

        //Set the received message at the MessageReceiver to be null
        MessageReceiver.getInstance().setReceivedMessage(null);

        String incomingJson =
                "{\n" +
                "  \"Volatile\":{\n" +
                "    \"value\":{\n" +
                "      \"timestamp\": 1693905111432,\n" +
                "      \"command\":{\n" +
                "        \"command_type\":\"pip-command\",\n" +
                "        \"value\":{\n" +
                "          \"message\":{\n" +
                "            \"purpose\":\"ADD_PIP\",\n" +
                "            \"message_id\": \"testSuccessAddPipWebSocketLampStatus\",\n" +
                "            \"pip_type\":\"it.cnr.iit.ucs.pipwebsocket.PIPWebSocketLampStatus\",\n" +
                "            \"attribute_id\":\"eu:sifis-home:1.0:environment:lamp-status\",\n" +
                "            \"category\":\"urn:oasis:names:tc:xacml:3.0:attribute-category:environment\",\n" +
                "            \"data_type\":\"http://www.w3.org/2001/XMLSchema#boolean\",\n" +
                "            \"refresh_rate\":1000,\n" +
                "            \"additional_properties\":{\n" +
                "              \"dhtUri\":\"ws://localhost:3000/ws\",\n" +
                "              \"topicName\":\"domo_light\",\n" +
                "              \"topicUuid\":\"topic_name\"\n" +
                "            }\n" +
                "          },\n" +
                "          \"id\":\"pip-websocket-lamp-status\",\n" +
                "          \"topic_name\":\"topic-name\",\n" +
                "          \"topic_uuid\":\"topic-uuid-the-ucs-is-subscribed-to\"\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}";
        JsonIn jsonIn = deserializeIncomingJson(incomingJson);
        System.out.println(new GsonBuilder()
                .disableHtmlEscaping()
                .serializeNulls()
                .setPrettyPrinting()
                .create()
                .toJson(jsonIn));

        PIPMessageManager.processMessage(jsonIn);

        // wait some time if the WebSocketTestServer has not yet propagated
        // the message to the MessageReceiver
        if (MessageReceiver.getInstance().getReceivedMessage() == null) {
            sleep(400);
        }

        String ucsResponse = MessageReceiver.getInstance().getReceivedMessage();
        assertTrue(ucsResponse.contains("\"purpose\":\"ADD_PIP_RESPONSE\""));
        assertTrue(ucsResponse.contains("\"code\":\"OK\""));
        assertTrue(ucsResponse.contains("\"message_id\":\"testSuccessAddPipWebSocketLampStatus\""));
    }

    public void addPipWebsocketLamps() throws InterruptedException {

        //Set the received message at the MessageReceiver to be null
        MessageReceiver.getInstance().setReceivedMessage(null);

        String incomingJson =
                "{\n" +
                "  \"Volatile\":{\n" +
                "    \"value\":{\n" +
                "      \"timestamp\":1693905119932,\n" +
                "      \"command\":{\n" +
                "        \"command_type\":\"pip-command\",\n" +
                "        \"value\":{\n" +
                "          \"message\":{\n" +
                "            \"purpose\":\"ADD_PIP\",\n" +
                "            \"message_id\":\"testSuccessAddPipWebSocketLamps\",\n" +
                "            \"pip_type\":\"it.cnr.iit.ucs.pipwebsocket.PIPWebSocketLamps\",\n" +
                "            \"attribute_id\":\"eu:sifis-home:1.0:environment:all-lamps-are-on\",\n" +
                "            \"category\":\"urn:oasis:names:tc:xacml:3.0:attribute-category:environment\",\n" +
                "            \"data_type\":\"http://www.w3.org/2001/XMLSchema#boolean\",\n" +
                "            \"refresh_rate\":1000,\n" +
                "            \"additional_properties\":{\n" +
                "              \"dhtUri\":\"ws://localhost:3000/ws\",\n" +
                "              \"topicName\":\"topic_name\"\n" +
                "            }\n" +
                "          },\n" +
                "          \"id\":\"pip-websocket-all-lamps-are-on\",\n" +
                "          \"topic_name\":\"topic-name\",\n" +
                "          \"topic_uuid\":\"topic-uuid-the-ucs-is-subscribed-to\"\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}";
        JsonIn jsonIn = deserializeIncomingJson(incomingJson);
        System.out.println(new GsonBuilder()
                .disableHtmlEscaping()
                .serializeNulls()
                .setPrettyPrinting()
                .create()
                .toJson(jsonIn));

        PIPMessageManager.processMessage(jsonIn);

        // wait some time if the WebSocketTestServer has not yet propagated
        // the message to the MessageReceiver
        if (MessageReceiver.getInstance().getReceivedMessage() == null) {
            sleep(400);
        }

        String ucsResponse = MessageReceiver.getInstance().getReceivedMessage();
        assertTrue(ucsResponse.contains("\"purpose\":\"ADD_PIP_RESPONSE\""));
        assertTrue(ucsResponse.contains("\"code\":\"OK\""));
        assertTrue(ucsResponse.contains("\"message_id\":\"testSuccessAddPipWebSocketLamps\""));

    }

    public void addPipReader() throws InterruptedException {

        // Set the received message at the MessageReceiver to be null
        MessageReceiver.getInstance().setReceivedMessage(null);

        String incomingJson =
                "{\n" +
                "  \"Volatile\":{\n" +
                "    \"value\":{\n" +
                "      \"timestamp\":1693905122932,\n" +
                "      \"command\":{\n" +
                "        \"command_type\":\"pip-command\",\n" +
                "        \"value\":{\n" +
                "          \"message\":{\n" +
                "            \"purpose\":\"ADD_PIP\",\n" +
                "            \"message_id\":\"testSuccessAddPipReader\",\n" +
                "            \"pip_type\":\"it.cnr.iit.ucs.pipreader.PIPReader\",\n" +
                "            \"attribute_id\":\"eu:sifis-home:1.0:environment:all-windows-in-bedroom-closed\",\n" +
                "            \"category\":\"urn:oasis:names:tc:xacml:3.0:attribute-category:environment\",\n" +
                "            \"data_type\":\"http://www.w3.org/2001/XMLSchema#boolean\",\n" +
                "            \"refresh_rate\":1000,\n" +
                "            \"additional_properties\":{\n" +
                "              \"eu:sifis-home:1.0:environment:all-windows-in-bedroom-closed\":\"windows-in-bedroom.txt\",\n" +
                "              \"windows-in-bedroom.txt\":\"true\"\n" +
                "            }\n" +
                "          },\n" +
                "          \"id\":\"pip-reader-windows\",\n" +
                "          \"topic_name\":\"topic-name\",\n" +
                "          \"topic_uuid\":\"topic-uuid-the-ucs-is-subscribed-to\"\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}";
        JsonIn jsonIn = deserializeIncomingJson(incomingJson);
        System.out.println(new GsonBuilder()
                .disableHtmlEscaping()
                .serializeNulls()
                .setPrettyPrinting()
                .create()
                .toJson(jsonIn));

        PIPMessageManager.processMessage(jsonIn);

        // wait some time if the WebSocketTestServer has not yet propagated
        // the message to the MessageReceiver
        if (MessageReceiver.getInstance().getReceivedMessage() == null) {
            sleep(400);
        }

        String ucsResponse = MessageReceiver.getInstance().getReceivedMessage();
        assertTrue(ucsResponse.contains("\"purpose\":\"ADD_PIP_RESPONSE\""));
        assertTrue(ucsResponse.contains("\"code\":\"OK\""));
        assertTrue(ucsResponse.contains("\"message_id\":\"testSuccessAddPipReader\""));
    }
}
