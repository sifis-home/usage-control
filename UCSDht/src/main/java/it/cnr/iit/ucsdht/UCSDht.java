package it.cnr.iit.ucsdht;

import it.cnr.iit.ucs.properties.components.PipProperties;
import it.cnr.iit.ucsdht.properties.UCSDhtPapProperties;
import it.cnr.iit.ucsdht.properties.UCSDhtPipReaderProperties;
import it.cnr.iit.utility.dht.DHTClient;
import it.cnr.iit.xacml.Category;
import it.cnr.iit.xacml.DataType;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import static it.cnr.iit.utility.dht.DHTUtils.isDhtReachable;

public class UCSDht {
    static DHTClient dhtClientEndPoint;
    static UCSClient ucsClient;

    private static String dhtUri = "ws://localhost:3000/ws";
    private static UCSClient ucsClient;

    static final String COMMAND_TYPE = "ucs-command";
    static final String UCS_SUB_TOPIC_UUID = "topic-uuid-the-ucs-is-subscribed-to";

    static final String PAP_SUB_TOPIC_NAME = "topic-name-pap-is-subscribed-to";
    static final String PAP_SUB_TOPIC_UUID = "topic-uuid-pap-is-subscribed-to";

    static final String PIP_SUB_TOPIC_NAME = "topic-name-pip-is-subscribed-to";
    static final String PIP_SUB_TOPIC_UUID = "topic-uuid-pip-is-subscribed-to";
    static final File attributesDir = new File(Utils.getResourcePath(UCSDht.class), "attributes");
    static final File policiesDir = new File(Utils.getResourcePath(UCSDht.class), "policies");


    public static void main(String[] args) {

        if (args.length != 0 && args[0].equals("-d")) {
            URI parsed = null;
            try {
                parsed = new URI(args[1]);
            } catch (URISyntaxException | ArrayIndexOutOfBoundsException e) {
                // No URI indicated
                System.err.println("Invalid URI after -d option");
                return;
            }
            dhtUri = parsed.toString();
        }

        initializeUCS();

        if (!isDhtReachable(dhtUri, 2000, Integer.MAX_VALUE)) {
            return;
        }

        try {
            dhtClientEndPoint = new DHTClient(new URI(dhtUri));
            dhtClientEndPoint.addMessageHandler(new DhtMessageHandler());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        System.out.println("Waiting for commands...");
    }


    private static void initializeUCS() {

        Utils.createDir(attributesDir);
        Utils.createDir(policiesDir);

        List<PipProperties> pipPropertiesList = new ArrayList<>();

        // add sample attribute
        UCSDhtPipReaderProperties pipReader = new UCSDhtPipReaderProperties();
        pipReader.addAttribute(
                "urn:oasis:names:tc:xacml:3.0:environment:attribute-1",
                Category.ENVIRONMENT.toString(),
                DataType.STRING.toString(),
                attributesDir + File.separator + "sample-attribute.txt");
        pipReader.setRefreshRate(1000L);
        pipPropertiesList.add(pipReader);

        setAttributeValue(attributesDir.getAbsolutePath() + File.separator
                + "sample-attribute.txt", "attribute-1-value");

        UCSDhtPapProperties papProperties = new UCSDhtPapProperties(policiesDir.getAbsolutePath());

        ucsClient = new UCSClient(pipPropertiesList, papProperties);

        // add sample policy
        String examplePolicy = Utils.readContent(Utils.accessFile(UCSDht.class, "example-policy.xml"));
        ucsClient.addPolicy(examplePolicy);

        System.out.println("Policies directory: " + policiesDir.getAbsolutePath());
        System.out.println("Attributes directory: " + attributesDir.getAbsolutePath());

        System.out.println("UCS initialization complete");
    }

    public static void setAttributeValue(String fileName, String value) {

        File file = new File(fileName);
        FileWriter fw = null;
        try {
            fw = new FileWriter(file);
            fw.write(value);
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}