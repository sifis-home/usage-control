package it.cnr.iit.ucsdht;

import com.google.gson.GsonBuilder;
import it.cnr.iit.utility.dht.jsonvolatile.JsonIn;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.websocket.server.WebSocketHandler;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static it.cnr.iit.utility.dht.DHTUtils.deserializeIncomingJson;
import static java.lang.Thread.sleep;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class TestPEPManagerCompleteFlow {

    private static final int TEST_SERVER_PORT = 3001;
    private static final  String TEST_SERVER_HOST = "localhost";

    private Server server;

    private String sessionId = null;


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
    public void testSuccessCompleteFlow() throws RuntimeException, InterruptedException {

        register();
        tryAccess();

        // Add a PIP to the UCS so that the ongoing section of the policy
        // can be evaluated successfully (it should evaluate to Permit)
        addPipReader();

        startAccess();
        endAccess();

    }

    public void addPipReader() throws InterruptedException {

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
                    "            \"attribute_id\":\"eu:sifis-home:1.0:environment:all-lamps-are-on\",\n" +
                    "            \"category\":\"urn:oasis:names:tc:xacml:3.0:attribute-category:environment\",\n" +
                    "            \"data_type\":\"http://www.w3.org/2001/XMLSchema#boolean\",\n" +
                    "            \"refresh_rate\":1000,\n" +
                    "            \"additional_properties\":{\n" +
                    "              \"eu:sifis-home:1.0:environment:all-lamps-are-on\":\"all-lamps-are-on.txt\",\n" +
                    "              \"all-lamps-are-on.txt\":\"true\"\n" +
                    "            }\n" +
                    "          },\n" +
                    "          \"id\":\"test-pip\",\n" +
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

    public void register() throws InterruptedException {
        // Restore the value of receivedMessage to null
        MessageReceiver.getInstance().setReceivedMessage(null);

        String incomingJson =
                "{\n" +
                "  \"Volatile\": {\n" +
                "    \"value\": {\n" +
                "      \"timestamp\": 1684851919378,\n" +
                "      \"command\": {\n" +
                "        \"command_type\": \"pep-command\",\n" +
                "        \"value\": {\n" +
                "          \"message\": {\n" +
                "            \"purpose\": \"REGISTER\",\n" +
                "            \"message_id\": \"testSuccessRegister\",\n" +
                "            \"sub_topic_name\": \"topic-name-the-pep-is-subscribed-to\",\n" +
                "            \"sub_topic_uuid\": \"topic-uuid-the-pep-is-subscribed-to\"\n" +
                "          },\n" +
                "          \"id\": \"pep-0\",\n" +
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

        PEPMessageManager.processMessage(jsonIn);

        // wait some time if the WebSocketTestServer has not yet propagated
        // the message to the MessageReceiver
        if (MessageReceiver.getInstance().getReceivedMessage() == null) {
            sleep(400);
        }

        String ucsResponse = MessageReceiver.getInstance().getReceivedMessage();
        assertTrue(ucsResponse.contains("\"purpose\":\"REGISTER_RESPONSE\""));
        assertTrue(ucsResponse.contains("\"code\":\"OK\""));
        assertTrue(ucsResponse.contains("\"message_id\":\"testSuccessRegister\""));
    }


    public void tryAccess() throws InterruptedException {

        // Restore the value of receivedMessage to null
        MessageReceiver.getInstance().setReceivedMessage(null);

        // Install a policy that is applicable to the request
        String policy = Utils.readContent(Utils.accessFile(TestPEPManagerCompleteFlow.class, "test-policy.xml"));
        UCSDht.ucsClient.addPolicy(policy);

        // Load the request that is included in the json message
        String request = Utils.readContent(Utils.accessFile(TestPEPManagerCompleteFlow.class, "test-request.xml"));
        String base64Request = Base64.getEncoder().encodeToString(request.getBytes());

        String incomingJson =
                "{\n" +
                "  \"Volatile\": {\n" +
                "    \"value\": {\n" +
                "      \"timestamp\": 1684851920422,\n" +
                "      \"command\": {\n" +
                "        \"command_type\": \"pep-command\",\n" +
                "        \"value\": {\n" +
                "          \"message\": {\n" +
                "            \"purpose\": \"TRY\",\n" +
                "            \"message_id\": \"testSuccessTryAccess\",\n" +
                "            \"request\": \"" + base64Request + "\",\n" +
                "            \"policy\": null\n" +
                "          },\n" +
                "          \"id\": \"pep-0\",\n" +
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

        PEPMessageManager.processMessage(jsonIn);

        // wait some time if the WebSocketTestServer has not yet propagated
        // the message to the MessageReceiver
        if (MessageReceiver.getInstance().getReceivedMessage() == null) {
            sleep(400);
        }

        String ucsResponse = MessageReceiver.getInstance().getReceivedMessage();
        assertTrue(ucsResponse.contains("\"purpose\":\"TRY_RESPONSE\""));
        assertTrue(ucsResponse.contains("\"message_id\":\"testSuccessTryAccess\""));
        assertTrue(ucsResponse.contains("\"evaluation\":\"Permit\""));

        // Extract the session_id and save it in a global variable to be used in other tests
        Pattern sessionPattern = Pattern.compile("\"session_id\":\"([^\"]+)\"");
        Matcher matcher = sessionPattern.matcher(ucsResponse);

        if (matcher.find()) {
            sessionId = matcher.group(1);
            System.out.println("Extracted session_id: " + sessionId);
        }
        assertNotNull(sessionId);
    }


    public void startAccess() throws InterruptedException {
        // Restore the value of receivedMessage to null
        MessageReceiver.getInstance().setReceivedMessage(null);

        // Start with the actual Start Access test
        String incomingJson =
                "{\n" +
                "  \"Volatile\": {\n" +
                "    \"value\": {\n" +
                "      \"timestamp\": 1684851921396,\n" +
                "      \"command\": {\n" +
                "        \"command_type\": \"pep-command\",\n" +
                "        \"value\": {\n" +
                "          \"message\": {\n" +
                "            \"purpose\": \"START\",\n" +
                "            \"message_id\": \"testSuccessStartAccess\",\n" +
                "            \"session_id\": \"" + sessionId + "\"\n" +
                "          },\n" +
                "          \"id\": \"pep-0\",\n" +
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

        PEPMessageManager.processMessage(jsonIn);

        // wait some time if the WebSocketTestServer has not yet propagated
        // the message to the MessageReceiver
        if (MessageReceiver.getInstance().getReceivedMessage() == null) {
            sleep(400);
        }

        String ucsResponse = MessageReceiver.getInstance().getReceivedMessage();
        assertTrue(ucsResponse.contains("\"purpose\":\"START_RESPONSE\""));
        assertTrue(ucsResponse.contains("\"message_id\":\"testSuccessStartAccess\""));
        assertTrue(ucsResponse.contains("\"evaluation\":\"Permit\""));

    }


    public void endAccess() throws InterruptedException {
        // Restore the value of receivedMessage to null
        MessageReceiver.getInstance().setReceivedMessage(null);

        // Start with the actual End Access test
        String incomingJson =
                "{\n" +
                "  \"Volatile\": {\n" +
                "    \"value\": {\n" +
                "      \"timestamp\": 1684851924867,\n" +
                "      \"command\": {\n" +
                "        \"command_type\": \"pep-command\",\n" +
                "        \"value\": {\n" +
                "          \"message\": {\n" +
                "            \"purpose\": \"END\",\n" +
                "            \"message_id\": \"testSuccessEndAccess\",\n" +
                "            \"session_id\": \"" + sessionId + "\"\n" +
                "          },\n" +
                "          \"id\": \"pep-0\",\n" +
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

        PEPMessageManager.processMessage(jsonIn);

        // wait some time if the WebSocketTestServer has not yet propagated
        // the message to the MessageReceiver
        if (MessageReceiver.getInstance().getReceivedMessage() == null) {
            sleep(400);
        }

        String ucsResponse = MessageReceiver.getInstance().getReceivedMessage();
        assertTrue(ucsResponse.contains("\"purpose\":\"END_RESPONSE\""));
        assertTrue(ucsResponse.contains("\"message_id\":\"testSuccessEndAccess\""));
        assertTrue(ucsResponse.contains("\"evaluation\":\"Permit\""));
    }
}
