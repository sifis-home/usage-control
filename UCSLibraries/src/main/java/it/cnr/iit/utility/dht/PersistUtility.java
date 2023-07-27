package it.cnr.iit.utility.dht;

import it.cnr.iit.ucs.properties.components.PepProperties;
import it.cnr.iit.ucs.properties.components.PipProperties;
import it.cnr.iit.utility.JAXBUtility;
import it.cnr.iit.utility.JsonUtility;
import oasis.names.tc.xacml.core.schema.wd_17.PolicyType;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Optional;

public class PersistUtility {

    /**
     * Convert the content of a file into a base64 string.
     * @param sourceFilePath the path of the file to convert
     * @return the base64 string representing the content of the file
     */
    public static String fileToBase64String(String sourceFilePath) {
        // Read the content of the source file
        byte[] bytes;
        try {
            bytes = Files.readAllBytes(Paths.get(sourceFilePath));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return new String(Base64.getEncoder().encode(bytes));
    }

    /**
     * Decode a base64 string and save it as the content of the file whose
     * path and file name is specified as argument.
     * @param base64String the base64 string
     * @param destinationFilePath the file path of the destination file
     */
    public static void base64StringToFile(String base64String, String destinationFilePath) {
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(base64String);
            Files.write(Paths.get(destinationFilePath), bytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Extract the pip properties from a base64 string. The decoded base64 string must
     * have a json structure matching that of a class that extends PipProperties.
     * @param base64String the base64 string
     * @param clazz the class that extends PipProperties
     * @return the PipProperties object
     */
    public static PipProperties getPipPropertiesFromBase64String(String base64String, Class<? extends PipProperties> clazz) {
        byte[] bytes = Base64.getDecoder().decode(base64String);
        String jsonString = new String(bytes);
        Optional<? extends PipProperties> object = JsonUtility.loadObjectFromJsonString(jsonString, clazz);
        return object.orElse(null);
    }

    /**
     * Extract the pep properties from a base64 string. The decoded base64 string must
     * have a json structure matching that of a class that extends PepProperties.
     * @param base64String the base64 string
     * @param clazz the class that extends PepProperties
     * @return the PepProperties object
     */
    public static PepProperties getPepPropertiesFromBase64String(String base64String, Class<? extends PepProperties> clazz) {
        byte[] bytes = Base64.getDecoder().decode(base64String);
        String jsonString = new String(bytes);
        Optional<? extends PepProperties> object = JsonUtility.loadObjectFromJsonString(jsonString, clazz);
        return object.orElse(null);
    }

    /**
     * Extract the policyId from a base64 string. The decoded base64 string must be a
     * valid policy that can be unmarshalled to a PolicyType object.
     * Once unmarshalled, the policyId is returned.
     * @param base64String the base64 string
     * @return the string containing the policyId
     */
    public static String getPolicyIdFromBase64String(String base64String) {
        byte[] bytes = Base64.getDecoder().decode(base64String);
        String xmlString = new String(bytes);
        PolicyType policy;
        try {
            policy = JAXBUtility.unmarshalToObject(PolicyType.class, xmlString);
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
        return policy.getPolicyId();
    }
}
