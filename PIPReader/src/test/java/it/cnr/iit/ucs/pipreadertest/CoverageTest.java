package it.cnr.iit.ucs.pipreadertest;

import com.tngtech.jgiven.annotation.AfterScenario;
import it.cnr.iit.ucs.exceptions.PIPException;
import it.cnr.iit.ucs.pipreader.PIPReader;
import it.cnr.iit.ucs.properties.UCFPipProperties;
import it.cnr.iit.ucs.requestmanager.RequestManagerInterface;
import it.cnr.iit.utility.JAXBUtility;
import it.cnr.iit.utility.JsonUtility;
import it.cnr.iit.utility.errorhandling.exception.PreconditionException;
import it.cnr.iit.xacml.Attribute;
import it.cnr.iit.xacml.Category;
import it.cnr.iit.xacml.DataType;
import oasis.names.tc.xacml.core.schema.wd_17.AttributeType;
import oasis.names.tc.xacml.core.schema.wd_17.AttributeValueType;
import oasis.names.tc.xacml.core.schema.wd_17.AttributesType;
import oasis.names.tc.xacml.core.schema.wd_17.RequestType;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Optional;
import java.util.logging.Logger;

import static org.junit.Assert.*;

@EnableConfigurationProperties
@TestPropertySource(properties = "application.properties")
@ActiveProfiles("test")
@RunWith(SpringRunner.class)
@SpringBootTest
@SpringBootConfiguration
public class CoverageTest {

    private static final Logger log = Logger.getLogger(CoverageTest.class.getName());

    @Value("${environment.filepath}")
    private String environmentFilePath;

    @Value("${requestType}")
    private String requestS;

    @Value("${subjectPip}")
    private String subjectPip;

    @Value("${resourcePip}")
    private String resourcePip;

    @Value("${actionPip}")
    private String actionPip;

    @Value("${environmentPip}")
    private String environmentPip;

    @Value("${missingCategory}")
    private String missingCategory;

    @Value("${missingAttributeId}")
    private String missingAttributeId;

    @Value("${missingDataType}")
    private String missingDataType;

    @Value("${missingFilePath}")
    private String missingFilePath;

    @Value("${missingExpectedCategory}")
    private String missingExpectedCategory;

    @Value("${malformedInput}")
    private String malformedInput;

    private RequestType requestType = new RequestType();
    private PIPReader subjectAttributePip;
    private PIPReader resourceAttributePip;
    private PIPReader actionAttributePip;
    private PIPReader environmentAttributePip;
    private PIPReader fault;
    private final Attribute subjectAttribute = new Attribute();
    private final Attribute resourceAttribute = new Attribute();
    private final Attribute actionAttribute = new Attribute();
    private final Attribute environmentAttribute = new Attribute();

    public void init() {
        try {
            resetRequest();
            RequestManagerInterface requestManager = Mockito.mock(RequestManagerInterface.class);
            subjectAttributePip = new PIPReader(getPropertiesFromString(subjectPip));
            resourceAttributePip = new PIPReader(getPropertiesFromString(resourcePip));
            actionAttributePip = new PIPReader(getPropertiesFromString(actionPip));
            environmentAttributePip = new PIPReader(getPropertiesFromString(environmentPip));
            initAttributes();
            subjectAttributePip.setRequestManager(requestManager);
            resourceAttributePip.setRequestManager(requestManager);
            actionAttributePip.setRequestManager(requestManager);
            environmentAttributePip.setRequestManager(requestManager);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void resetRequest() {
        try {
            requestType = JAXBUtility.unmarshalToObject(RequestType.class, requestS);
        } catch (JAXBException e) {
            e.printStackTrace();
        }
    }

    private void initAttributes() {
        subjectAttribute.setAttributeId("urn:oasis:names:tc:xacml:1.0:subject:role");
        subjectAttribute.setDataType(DataType.STRING);
        subjectAttribute.setCategory(Category.SUBJECT);

        resourceAttribute.setAttributeId("urn:oasis:names:tc:xacml:1.0:resource:class");
        resourceAttribute.setDataType(DataType.STRING);
        resourceAttribute.setCategory(Category.RESOURCE);

        actionAttribute.setAttributeId("urn:oasis:names:tc:xacml:1.0:resource:type");
        actionAttribute.setDataType(DataType.STRING);
        actionAttribute.setCategory(Category.ACTION);

        environmentAttribute.setAttributeId("urn:oasis:names:tc:xacml:3.0:environment:temperature");
        environmentAttribute.setDataType(DataType.STRING);
        environmentAttribute.setCategory(Category.ENVIRONMENT);
    }


    @Test
    public void test() throws PIPException {
        init();
        resetAttributeValue();
        testRetrieve();
        testSubscribe();
        try {
            Thread.sleep(2000); // NOSONAR
            changeAttributeValue();
            Thread.sleep(2000); // NOSONAR
            resetAttributeValue();
            testUnsubscribe();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static UCFPipProperties getPropertiesFromString(String properties) {
        return JsonUtility.loadObjectFromJsonString(properties, UCFPipProperties.class).get();
    }

    @Test(expected = PreconditionException.class)
    public void testMissingCategory() {
        new PIPReader(getPropertiesFromString(missingCategory));
    }

    @Test(expected = PreconditionException.class)
    public void testMissingAttributeId() {
        new PIPReader(getPropertiesFromString(missingAttributeId));
    }

    @Test(expected = PreconditionException.class)
    public void testMissingExpCat() {
        new PIPReader(getPropertiesFromString(missingExpectedCategory));
    }

    @Test(expected = PreconditionException.class)
    public void testMissingDataType() {
        new PIPReader(getPropertiesFromString(missingDataType));
    }

    @Test(expected = PreconditionException.class)
    public void testMissingFilePath() {
        new PIPReader(getPropertiesFromString(missingFilePath));
    }

    @Test(expected = PreconditionException.class)
    public void testNull() {
        new PIPReader(null);
    }

    public void testRetrieve() throws PIPException {
        try {
            log.info("*****************BEGIN RETRIEVE TEST*******************");
            remoteRetrievalTest();
            localTest();
            log.info("*****************END RETRIEVE TEST*******************");
        } catch (Exception e) {
            throw new PIPException(e.getMessage());
        }
    }

    public void remoteRetrievalTest() throws PIPException {
        // test should fail because additionalInformation is not set
        PIPException subjectException = assertThrows(PIPException.class,
                () -> testRetrieveAttribute(subjectAttribute, subjectAttributePip));
        assertTrue(subjectException.getMessage().contains("Attribute Manager: unable to retrieve a value for filter:"));

        // test should pass because additionalInformation is set with a value present in the file role.txt
        subjectAttribute.setAdditionalInformation("User");
        String subjectRole = testRetrieveAttribute(subjectAttribute, subjectAttributePip);
        assertEquals("IIT", subjectRole);
        // clear additionalInformation for other tests
        subjectAttribute.setAdditionalInformation("");


        // test should fail because additionalInformation is not set
        PIPException resourceException = assertThrows(PIPException.class,
                () -> testRetrieveAttribute(resourceAttribute, resourceAttributePip));
        assertTrue(resourceException.getMessage().contains("Attribute Manager: unable to retrieve a value for filter:"));

        // test should pass because additionalInformation is set with a value present in the file resource.txt
        resourceAttribute.setAdditionalInformation("DATASET");
        String resourceValue = testRetrieveAttribute(resourceAttribute, resourceAttributePip);
        assertEquals("SECRET", resourceValue);
        // clear additionalInformation for other tests
        resourceAttribute.setAdditionalInformation("");

        // test should fail because additionalInformation is not set
        PIPException actionException = assertThrows(PIPException.class,
                () -> testRetrieveAttribute(actionAttribute, actionAttributePip));
        assertTrue(actionException.getMessage().contains("Attribute Manager: unable to retrieve a value for filter:"));

        // test should pass because additionalInformation is set with a value present in the file action.txt
        actionAttribute.setAdditionalInformation("READ");
        String actionValue = testRetrieveAttribute(actionAttribute, actionAttributePip);
        assertEquals("ANALYZE", actionValue);
        // clear additionalInformation for other tests
        actionAttribute.setAdditionalInformation("");

        String value = testRetrieveAttribute(environmentAttribute, environmentAttributePip);
        assertEquals("30.0", value);
        Optional<String> optEnvVal = JsonUtility.getJsonStringFromObject(environmentAttribute, true);
        assertTrue(optEnvVal.isPresent());
        log.info(optEnvVal.get());
    }

    // The retrieve(attribute) method retrieves the attribute directly.
    //
    // For a PIP monitoring a (i) subject-related attribute, (ii) a resource-related attribute,
    // or a (iii) action-related attribute, the additionalInformation field has to be set
    // explicitly before the invocation of this method.
    // Otherwise, this method will fail because the PIP is unaware of the 'filter' to be used
    // For example, if the PIP is monitoring the attribute with attributeId urn:...:subject-role,
    // the additionalInformation, i.e., the value of the attribute with attributeId urn:...:subject-id,
    // has to be set beforehand.
    // The retrieve(attribute) method tries to get the additionalInformation and use it as a filter
    // to select the correct entry (line) in the file, by using the filter as key. A sample line
    // could be "Dave admin".
    // Note that the additionalInformation is set when either the method retrieve(request) or
    // retrieve(request, attributeId)is invoked, and those methods use the request to this aim.
    // Such methods first set the additionalInformation and then call the retrieve(attribute) method.

    private String testRetrieveAttribute(Attribute attribute, PIPReader pipReader) throws PIPException {
        try {
            return pipReader.retrieve(attribute);
        } catch (Exception e) {
            throw new PIPException(e.getMessage());
        }
    }

    public void localTest() throws Exception {
        log.info("-------BEGIN RETRIEVE TEST-------");

        testRetrieveAndEnrichment(requestType, subjectAttributePip);
        assertEquals("[IIT]", extractAttributeValueFromRequest(requestType, subjectAttribute));

        testRetrieveAndEnrichment(requestType, resourceAttributePip);
        assertEquals("[SECRET]", extractAttributeValueFromRequest(requestType, resourceAttribute));

        testRetrieveAndEnrichment(requestType, actionAttributePip);
        assertEquals("[ANALYZE]", extractAttributeValueFromRequest(requestType, actionAttribute));

        testRetrieveAndEnrichment(requestType, environmentAttributePip);
        assertEquals("[30.0]", extractAttributeValueFromRequest(requestType, environmentAttribute));

        resetRequest();

        log.info("-------END RETRIEVE TEST-------");
    }

    private void testRetrieveAndEnrichment(RequestType requestType, PIPReader pipReader) throws PIPException {
        try {
            pipReader.retrieve(requestType);
        } catch (Exception e) {
            e.printStackTrace();
            throw new PIPException(e.getMessage());
        }
    }

    public void testSubscribe() {
        try {
            log.info("*****************BEGIN SUBSCRIBE TEST*******************");
            remoteSubscribeTest();
            localSubscribeTest();
            log.info("*****************END SUBSCRIBE TEST*******************");
        } catch (Exception e) {
            log.severe(e.getMessage());
        }
    }

    public void localSubscribeTest() throws Exception {
        log.info("-------BEGIN SUBSCRIBE TEST-------");

        testSubscribeAndEnrichment(requestType, subjectAttributePip);
        assertEquals("[IIT]", extractAttributeValueFromRequest(requestType, subjectAttribute));

        testSubscribeAndEnrichment(requestType, resourceAttributePip);
        assertEquals("[SECRET]", extractAttributeValueFromRequest(requestType, resourceAttribute));

        testSubscribeAndEnrichment(requestType, actionAttributePip);
        assertEquals("[ANALYZE]", extractAttributeValueFromRequest(requestType, actionAttribute));

        testSubscribeAndEnrichment(requestType, environmentAttributePip);
        assertEquals("[30.0]", extractAttributeValueFromRequest(requestType, environmentAttribute));

        resetRequest();

        log.info("-------END SUBSCRIBE TEST-------");
    }

    private void testSubscribeAndEnrichment(RequestType requestType, PIPReader pipReader) {
        try {
            pipReader.subscribe(requestType);
        } catch (Exception e) {
            log.severe(e.getMessage());
        }
    }

    public void remoteSubscribeTest() throws PIPException {
        PIPException subjectException = assertThrows(PIPException.class,
                () -> testSubscribeAttribute(subjectAttribute, subjectAttributePip));
        assertTrue(subjectException.getMessage().contains("Attribute Manager: unable to retrieve a value for filter:"));

        PIPException resourceException = assertThrows(PIPException.class,
                () -> testSubscribeAttribute(resourceAttribute, resourceAttributePip));
        assertTrue(resourceException.getMessage().contains("Attribute Manager: unable to retrieve a value for filter:"));

        PIPException actionException = assertThrows(PIPException.class,
                () -> testSubscribeAttribute(actionAttribute, actionAttributePip));
        assertTrue(actionException.getMessage().contains("Attribute Manager: unable to retrieve a value for filter:"));

        String value = testSubscribeAttribute(environmentAttribute, environmentAttributePip);
        assertEquals("30.0", value);
    }

    // The subscribe(attribute) method retrieves the attribute directly.
    // All the discussion above related to the retrieve(attribute) method is valid here.
    private String testSubscribeAttribute(Attribute attribute, PIPReader pipReader) throws PIPException {
        try {
            return pipReader.subscribe(attribute);
        } catch (Exception e) {
            throw new PIPException(e.getMessage());
        }
    }

    private void changeAttributeValue() {
        write("60.0");
    }

    private void resetAttributeValue() {
        write("30.0");
    }

    private void write(String string) {
        try {
            Path path = Paths.get(environmentFilePath);
            Files.write(path, string.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void testUnsubscribe() {
        try {
            log.info("*****************BEGIN UNSUBSCRIBE TEST*******************");

            // should return false since the additionalInformation is not set in the attribute
            boolean value = testUnsubscribeAttribute(subjectAttribute, subjectAttributePip);
            assertFalse(value);

            subjectAttribute.setAdditionalInformation("User");
            value = testUnsubscribeAttribute(subjectAttribute, subjectAttributePip);
            assertTrue(value);

            // should return false since there is no such attribute to unsubscribe
            // since it was unsubscribed during the previous test
            value = testUnsubscribeAttribute(subjectAttribute, subjectAttributePip);
            assertFalse(value);

            resourceAttribute.setAdditionalInformation("DATASET");
            value = testUnsubscribeAttribute(resourceAttribute, resourceAttributePip);
            assertTrue(value);

            actionAttribute.setAdditionalInformation("READ");
            value = testUnsubscribeAttribute(actionAttribute, actionAttributePip);
            assertTrue(value);

            value = testUnsubscribeAttribute(environmentAttribute, environmentAttributePip);
            assertTrue(value);

            log.info("*****************END UNSUBSCRIBE TEST*******************");
        } catch (Exception e) {
            log.severe(e.getMessage());
        }
    }

    private boolean testUnsubscribeAttribute(Attribute attribute, PIPReader pipReader) {
        ArrayList<Attribute> list = new ArrayList<>();
        try {
            list.add(attribute);
            return pipReader.unsubscribe(list);
        } catch (Exception e) {
            log.severe(e.getMessage());
        }
        return false;
    }

    /**
     * Given a request and an attribute, search the attributeId of attribute in the request.
     * If the attributeId is found in the request, return the value present in the request for that attribute.
     *
     * @param requestType the request
     * @param attribute   the attribute we are looking for in the request
     * @return the value of the attribute in the request, whose attributeId matches the one of the attribute
     * passed as second argument. If there is no match, return null
     */
    private String extractAttributeValueFromRequest(RequestType requestType, Attribute attribute) {
        // iterate over all the attribute categories in the request
        for (AttributesType attributeTypes : requestType.getAttributes()) {
            // then, for each category, iterate over the attributes of each category
            for (AttributeType attributeType : attributeTypes.getAttribute()) {
                // if the examined attributeId is equal to the one of the attribute we are testing (second argument):
                if (attributeType.getAttributeId().equals(attribute.getAttributeId())) {
                    // iterate over the values of that attribute in the request
                    // note: even it seems that we can have more than one value, we always return the first one. (?)
                    for (AttributeValueType attributeValue : attributeType.getAttributeValue()) {
                        log.info(attributeValue.getContent().toString());
                        return attributeValue.getContent().toString();
                    }
                }
            }
        }
        return null;
    }
}
