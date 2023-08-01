/*******************************************************************************
 * Copyright 2018 IIT-CNR
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package it.cnr.iit.ucs.pipreader;

import it.cnr.iit.ucs.exceptions.PIPException;
import it.cnr.iit.ucs.journaling.JournalBuilder;
import it.cnr.iit.ucs.journaling.JournalingInterface;
import it.cnr.iit.ucs.obligationmanager.ObligationInterface;
import it.cnr.iit.ucs.pip.PIPBase;
import it.cnr.iit.ucs.pip.PIPSubscriberTimer;
import it.cnr.iit.ucs.properties.components.PipProperties;
import it.cnr.iit.utility.FileUtility;
import it.cnr.iit.utility.errorhandling.Reject;
import it.cnr.iit.xacml.Attribute;
import oasis.names.tc.xacml.core.schema.wd_17.RequestType;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * The task this PIP is to read data from a file when requested.
 * The Path to reach the file is passed as parameter to the pip.
 *
 * @author Antonio La Marra, Alessandro Rosetti
 */
public class PIPReader extends PIPBase {

    private static final Logger log = Logger.getLogger(PIPReader.class.getName());
    private JournalingInterface journal;

    public static final String FILE_PATH_KEYWORD = "FILE_PATH";

    /**
     * Map having attributeId as key and the file path as value
     */
    private final Map<String, String> filePathMap = new HashMap<>();

    public PIPReader(PipProperties properties) {
        super(properties);
        Reject.ifFalse(init(properties), "Error initialising pip : " + properties.getId());
    }

    @Override
    public boolean init(PipProperties properties) {
        try {
            for (Map<String, String> attributeMap : properties.getAttributes()) {

                Attribute attribute = new Attribute();
                buildAttribute(attribute, attributeMap);

                // extract the file name from the additionalProperties, as value of the entry with
                // the key matching this attributeId
                String filePath = properties.getAdditionalProperties().get(attribute.getAttributeId());
                Reject.ifNull(filePath, "Missing file path");
                Reject.ifTrue(filePath.equals(""), "Empty file path");
                setFilePath(attribute.getAttributeId(), filePath);

                addAttribute(attribute);

                journal = JournalBuilder.build(properties);

                // timer for polling the value of the attribute
                PIPSubscriberTimer subscriberTimer = new PIPSubscriberTimer(this);
                subscriberTimer.setRate(properties.getRefreshRate());
                subscriberTimer.start();
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }


    /**
     * Retrieve the value of the attribute passed as argument.
     * If the value is not of type environment, this method uses the additionalInformation
     * (which has to be set beforehand) to select the right value from the file.
     * Then, it sets the value within the attribute, and finally returns a String
     * containing such a value.
     */
    @Override
    public String retrieve(Attribute attribute) throws PIPException {

        String filePath = this.filePathMap.get(attribute.getAttributeId());
        String value;
        if (isEnvironmentCategory(attribute)) {
            value = read(filePath);
        } else {
            value = read(filePath, attribute.getAdditionalInformation());
        }
        attribute.setValue(attribute.getDataType(), value);
        return value;
    }

    /**
     * Effective retrieval of the monitored value.
     *
     * @return the requested value
     * @throws PIPException if the value cannot be retrieved
     */
    private String read(String filePath) throws PIPException {
        try {
            Path path = Paths.get(filePath);
            // TODO UCS-33 NOSONAR
            String value = new String(Files.readAllBytes(path));
            journal.logString(formatJournaling(value));
            return value;
        } catch (IOException e) {
            throw new PIPException("Attribute Manager error : " + e.getMessage());
        }
    }

    /**
     * Effective retrieval of the monitored value looking for the line containing a filter.
     * Note that each line of the file MUST be composed of two fields separated
     * by either one or more spaces or tab, e.g.:
     * filter attribute
     * filter   attribute
     * filter\tattribute.
     *
     * @param filter the string to be used to search for the item we're interested into
     * @return the requested value
     * @throws PIPException if the value cannot be retrieved
     */
    private String read(String filePath, String filter) throws PIPException {
        // TODO UCS-33 NOSONAR
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            for (String line; (line = br.readLine()) != null; ) {
                String key = line.split("\\s+")[0];
                if (key.equals(filter)) {
                    String value = line.split("\\s+")[1];
                    journal.logString(formatJournaling(value, filter));
                    return value;
                }
            }
        } catch (Exception e) {
            throw new PIPException("Attribute Manager error: " + e.getMessage());
        }
        throw new PIPException("Attribute Manager: unable to retrieve a value for filter: \"" + filter + "\"\n");
    }

    private void setFilePath(String attributeId, String filePath) {
        String absFilePath = FileUtility.findFileAbsPathUsingClassLoader(filePath);
        if (absFilePath != null) {
            this.filePathMap.put(attributeId, absFilePath);
        } else {
            this.filePathMap.put(attributeId, filePath);
        }
        Reject.ifNull(this.filePathMap.get(attributeId));
    }

    private String formatJournaling(String... strings) {
        StringBuilder logStringBuilder = new StringBuilder();
        logStringBuilder.append("VALUE READ: ").append(strings[0]);

        if (strings.length > 1) {
            logStringBuilder.append(" FOR FILTER: ").append(strings[1]);
        }
        return logStringBuilder.toString();
    }

    @Override
    public void update(String data) throws PIPException {
// fixme: who is supposed to call this?
//        try {
//            Path path = Paths.get( filePath );
//            Files.write( path, data.getBytes() );
//        } catch( IOException e ) {
//            log.severe( "Error updating attribute : " + e.getMessage() );
//        }
        System.err.println("update() method not implemented");
    }

    @Override
    public void retrieve(RequestType request,
                         List<Attribute> attributeRetrievals) {
        log.severe("Multiple retrieve is unimplemented");
    }

    @Override
    public void subscribe(RequestType request,
                          List<Attribute> attributeRetrieval) {
        log.severe("Multiple subscribe is unimplemented");
    }

    @Override
    public void performObligation(ObligationInterface obligation) {
        if (obligation != null) {
            log.severe("Perform obligation is unimplemented");
        }
    }


    @Override
    protected Logger getLogger() {
        return log;
    }
}
