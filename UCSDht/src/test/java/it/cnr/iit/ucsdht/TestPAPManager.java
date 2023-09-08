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
import static org.junit.Assert.*;

public class TestPAPManager {

    private static final int TEST_SERVER_PORT = 3001;
    private static final  String TEST_SERVER_HOST = "localhost";

    private final String policy = Utils.readContent(Utils.accessFile(TestPEPManagerCompleteFlow.class, "test-policy.xml"));
    private final String base64Policy = Base64.getEncoder().encodeToString(policy.getBytes());
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
    public void testPAPManager() throws InterruptedException {

        addPolicy();
        listPolicies();
        getPolicy();
        deletePolicy();
    }


    public void addPolicy() throws InterruptedException {

        // Restore the value of receivedMessage to null
        MessageReceiver.getInstance().setReceivedMessage(null);

        String incomingJson =
                "{\n" +
                "  \"Volatile\": {\n" +
                "    \"value\": {\n" +
                "      \"timestamp\": 1684256524618,\n" +
                "      \"command\": {\n" +
                "        \"command_type\": \"pap-command\",\n" +
                "        \"value\": {\n" +
                "          \"message\": {\n" +
                "            \"purpose\": \"ADD_POLICY\",\n" +
                "            \"message_id\": \"testSuccessAddPolicy\",\n" +
                "            \"policy\": \"" + base64Policy + "\",\n" +
                "            \"policy_id\": \"test-policy\"\n" +
                "          },\n" +
                "          \"id\": \"pap-0\",\n" +
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

        PAPMessageManager.processMessage(jsonIn);

        // wait some time if the WebSocketTestServer has not yet propagated
        // the message to the MessageReceiver
        if (MessageReceiver.getInstance().getReceivedMessage() == null) {
            sleep(400);
        }

        String ucsResponse = MessageReceiver.getInstance().getReceivedMessage();
        assertTrue(ucsResponse.contains("\"purpose\":\"ADD_POLICY_RESPONSE\""));
        assertTrue(ucsResponse.contains("\"code\":\"OK\""));
        assertTrue(ucsResponse.contains("\"message_id\":\"testSuccessAddPolicy\""));

    }


    public void listPolicies() throws InterruptedException {

        // Restore the value of receivedMessage to null
        MessageReceiver.getInstance().setReceivedMessage(null);

        String incomingJson =
                "{\n" +
                "  \"Volatile\": {\n" +
                "    \"value\": {\n" +
                "      \"timestamp\": 1684256524618,\n" +
                "      \"command\": {\n" +
                "        \"command_type\": \"pap-command\",\n" +
                "        \"value\": {\n" +
                "          \"message\": {\n" +
                "            \"purpose\": \"LIST_POLICIES\",\n" +
                "            \"message_id\": \"testSuccessListPolicies\"\n" +
                "          },\n" +
                "          \"id\": \"pap-0\",\n" +
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

        PAPMessageManager.processMessage(jsonIn);

        // wait some time if the WebSocketTestServer has not yet propagated
        // the message to the MessageReceiver
        if (MessageReceiver.getInstance().getReceivedMessage() == null) {
            sleep(400);
        }

        String ucsResponse = MessageReceiver.getInstance().getReceivedMessage();
        assertTrue(ucsResponse.contains("\"purpose\":\"LIST_POLICIES_RESPONSE\""));
        assertTrue(ucsResponse.contains("\"message_id\":\"testSuccessListPolicies\""));
        assertTrue(ucsResponse.contains("\"test-policy\""));

    }


    public void getPolicy() throws InterruptedException {


        // Restore the value of receivedMessage to null
        MessageReceiver.getInstance().setReceivedMessage(null);

        String incomingJson =
                "{\n" +
                "  \"Volatile\": {\n" +
                "    \"value\": {\n" +
                "      \"timestamp\": 1684256524618,\n" +
                "      \"command\": {\n" +
                "        \"command_type\": \"pap-command\",\n" +
                "        \"value\": {\n" +
                "          \"message\": {\n" +
                "            \"purpose\": \"GET_POLICY\",\n" +
                "            \"message_id\": \"testSuccessGetPolicy\",\n" +
                "            \"policy_id\": \"test-policy\"\n" +
                "          },\n" +
                "          \"id\": \"pap-0\",\n" +
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

        PAPMessageManager.processMessage(jsonIn);

        // wait some time if the WebSocketTestServer has not yet propagated
        // the message to the MessageReceiver
        if (MessageReceiver.getInstance().getReceivedMessage() == null) {
            sleep(400);
        }

        String ucsResponse = MessageReceiver.getInstance().getReceivedMessage();
        assertTrue(ucsResponse.contains("\"purpose\":\"GET_POLICY_RESPONSE\""));
        assertTrue(ucsResponse.contains("\"message_id\":\"testSuccessGetPolicy\""));

        // Extract the policy
        Pattern policyPattern = Pattern.compile("\"policy\":\"([^\"]+)\"");
        Matcher matcher = policyPattern.matcher(ucsResponse);

        String retrievedBase64Policy = null;
        if (matcher.find()) {
            retrievedBase64Policy = matcher.group(1);
            System.out.println("Extracted base 64 policy: " + retrievedBase64Policy);
        }
        assertNotNull(retrievedBase64Policy);
        assertEquals(base64Policy, retrievedBase64Policy);

    }


    public void deletePolicy() throws InterruptedException {

        // Restore the value of receivedMessage to null
        MessageReceiver.getInstance().setReceivedMessage(null);

        String incomingJson =
                "{\n" +
                "  \"Volatile\": {\n" +
                "    \"value\": {\n" +
                "      \"timestamp\": 1684256524618,\n" +
                "      \"command\": {\n" +
                "        \"command_type\": \"pap-command\",\n" +
                "        \"value\": {\n" +
                "          \"message\": {\n" +
                "            \"purpose\": \"DELETE_POLICY\",\n" +
                "            \"message_id\": \"testSuccessDeletePolicy\",\n" +
                "            \"policy\": \"null\",\n" +
                "            \"policy_id\": \"test-policy\"\n" +
                "          },\n" +
                "          \"id\": \"pap-0\",\n" +
                "          \"topic_name\": \"topic-name\",\n" +
                "          \"topic_uuid\": \"topic-uuid-the-ucs-is-subscribed-to\"\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}\n";
        JsonIn jsonIn = deserializeIncomingJson(incomingJson);
        System.out.println(new GsonBuilder()
                .disableHtmlEscaping()
                .serializeNulls()
                .setPrettyPrinting()
                .create()
                .toJson(jsonIn));

        PAPMessageManager.processMessage(jsonIn);

        // wait some time if the WebSocketTestServer has not yet propagated
        // the message to the MessageReceiver
        if (MessageReceiver.getInstance().getReceivedMessage() == null) {
            sleep(400);
        }

        String ucsResponse = MessageReceiver.getInstance().getReceivedMessage();
        assertTrue(ucsResponse.contains("\"purpose\":\"DELETE_POLICY_RESPONSE\""));
        assertTrue(ucsResponse.contains("\"code\":\"OK\""));
        assertTrue(ucsResponse.contains("\"message_id\":\"testSuccessDeletePolicy\""));
    }
}
