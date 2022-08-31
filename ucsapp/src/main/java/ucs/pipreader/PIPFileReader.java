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
package ucs.pipreader;

import android.content.Context;

import com.example.ucsintent.UCSApplication;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

import ucs.exceptions.PIPException;
import ucs.journaling.JournalBuilder;
import ucs.pip.PIPKeywords;
import ucs.pip.PIPReaderBase;
import ucs.properties.components.PipProperties;
import utility.AndroidFileUtility;
import utility.errorhandling.Reject;
import xacml.Attribute;
import xacml.Category;
import xacml.DataType;

/**
 * The task this PIP is to read data from a file when requested.
 * The Path to reach the file is passed as parameter to the pip.
 *
 * @author Antonio La Marra, Alessandro Rosetti
 *
 */
public class PIPFileReader extends PIPReaderBase {

    private static Logger log = Logger.getLogger( PIPFileReader.class.getName() );

    public static final String FILE_PATH = "FILE_PATH";
    private String filePath;

    public PIPFileReader(PipProperties properties ) {
        super( properties );
        Reject.ifFalse( init( properties ), "Error initialising pip : " + properties.getId() );
    }

    private boolean init( PipProperties properties ) {
        try {
            Map<String, String> attributeMap = properties.getAttributes().get( 0 );
            Attribute attribute = new Attribute();
            attribute.setAttributeId( attributeMap.get( PIPKeywords.ATTRIBUTE_ID ) );
            Category category = Category.toCATEGORY( attributeMap.get( PIPKeywords.CATEGORY ) );
            attribute.setCategory( category );
            DataType dataType = DataType.toDATATYPE( attributeMap.get( PIPKeywords.DATA_TYPE ) );
            attribute.setDataType( dataType );
            if( attribute.getCategory() != Category.ENVIRONMENT ) {
                expectedCategory = Category.toCATEGORY( attributeMap.get( PIPKeywords.EXPECTED_CATEGORY ) );
                Reject.ifNull( expectedCategory, "missing expected category" );
            }
            Reject.ifFalse( attributeMap.containsKey( FILE_PATH ), "missing file path" );
            setFilePath( attributeMap.get( FILE_PATH ) );
            addAttribute( attribute );
            journal = JournalBuilder.build( properties );

            PIPFileReaderSubscriberTimer subscriberTimer = new PIPFileReaderSubscriberTimer( this );
            subscriberTimer.start();
            return true;
        } catch( Exception e ) {
            return false;
        }
    }

    /**
     * Effective retrieval of the monitored value.
     *
     * @return the requested value
     * @throws PIPException
     */
    protected String read() throws PIPException {
        // TODO UCS-33 NOSONAR
        String value = AndroidFileUtility.readFileAsString(filePath, UCSApplication.getContext());
        log.info("Read asked for path: " + filePath+ ", Read result: " + value);

        journal.logString( formatJournaling( value ) );
        return value;
    }

    /**
     * Effective retrieval of the monitored value looking for the line containing a filter.
     * NOTE we suppose that in the file each line has the following structure:
     * filter\tattribute.
     *
     * @param filter
     *          the string to be used to search for the item we're interested into
     * @return the requested value
     * @throws PIPException
    */
    protected String read( String filter ) throws PIPException {
        // TODO UCS-33 NOSONAR
        log.info("Read with filter ("+filter+") asked for path: " + filePath);

        Context context = UCSApplication.getContext();
        InputStream is = null;

        File file = new File(context.getExternalFilesDir(null), filePath);
        try {
            is = new FileInputStream(file);
        } catch( IOException exception ) {
            log.severe( "Unable to read file due to error: " + exception.getLocalizedMessage() );
            return null;
        }
        try (Scanner scanner = new Scanner( is )) {
            for( String line; ( line = scanner.nextLine() ) != null; ) {
                if( line.contains( filter ) ) {
                    String value = line.split( "\\s+" )[1];
                    journal.logString( formatJournaling( value, filter ) );
//                    log.info("Read result: " + value);
                    return value;
                }
            }
        } catch( Exception e ) {
            e.printStackTrace();
            throw new PIPException( "Attribute Manager error : " + e.getMessage() );
        }
        throw new PIPException( "Attribute Manager error : no value for this filter : " + filter );
    }

    private final void setFilePath( String filePath ) {
        this.filePath = filePath;
        Reject.ifBlank( this.filePath );
    }

    private String formatJournaling( String... strings ) {
        StringBuilder logStringBuilder = new StringBuilder();
        logStringBuilder.append( "VALUE READ: " + strings[0] );

        if( strings.length > 1 ) {
            logStringBuilder.append( " FOR FILTER: " + strings[1] );
        }
        return logStringBuilder.toString();
    }

    @Override
    public void update( String data ) throws PIPException {
        try {
            Path path = Paths.get( filePath );
            Files.write( path, data.getBytes() );
        } catch( IOException e ) {
            log.severe( "Error updating attribute : " + e.getMessage() );
        }
    }
}
