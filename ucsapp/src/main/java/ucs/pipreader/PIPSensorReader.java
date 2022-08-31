package ucs.pipreader;

import android.hardware.Sensor;

import com.example.ucsintent.UCSApplication;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import ucs.exceptions.PIPException;
import ucs.journaling.JournalBuilder;
import ucs.obligationmanager.ObligationInterface;
import ucs.pip.PIPKeywords;
import ucs.pip.PIPReaderBase;
import ucs.properties.components.PipProperties;
import utility.errorhandling.Reject;
import wd_17.RequestType;
import xacml.Attribute;
import xacml.Category;
import xacml.DataType;

/**
 * PIP Responsible for fetching data from sensors and various managers depending on SENSOR_TYPE attribute.
 * It also manages the direct monitoring of mutable attribute if necessary.
 */
public class PIPSensorReader extends PIPReaderBase {

    private static Logger log = Logger.getLogger( PIPSensorReader.class.getName() );
    public static final String SENSOR_TYPE = "SENSOR_TYPE";
    private String sensorType;
    private PIPSensorEventListener sensorEventListener;

    public PIPSensorReader(PipProperties properties) {
        super(properties);
        Reject.ifFalse( init( properties ), "Error initialising pip : " + properties.getId() );
    }

    private boolean init( PipProperties properties ) {
        try {
            Map<String, String> attributeMap = properties.getAttributes().get( 0 );
            Attribute attribute = new Attribute();
            attribute.setAttributeId( attributeMap.get( PIPKeywords.ATTRIBUTE_ID ) );
            Category category = Category.toCATEGORY( attributeMap.get( PIPKeywords.CATEGORY ) );
            log.info("Category: " + category);
            attribute.setCategory( category );
            DataType dataType = DataType.toDATATYPE( attributeMap.get( PIPKeywords.DATA_TYPE ) );
            attribute.setDataType( dataType );
            if( attribute.getCategory() != Category.ENVIRONMENT ) {
                expectedCategory = Category.toCATEGORY( attributeMap.get( PIPKeywords.EXPECTED_CATEGORY ) );
                Reject.ifNull( expectedCategory, "missing expected category" );
            }
            Reject.ifFalse( attributeMap.containsKey( SENSOR_TYPE ), "missing sensor type" );
            setSensorType( attributeMap.get( SENSOR_TYPE ));
            Reject.ifNull(getSensorType(), "SensorType must not be null");
            addAttribute( attribute );
            journal = JournalBuilder.build( properties );

            PIPSensorReaderSubscriberTimer subscriberTimer = new PIPSensorReaderSubscriberTimer( this );
            subscriberTimer.start();

            String dataManager = SensorHelper.getDataManagerName(sensorType);
            sensorEventListener = new PIPSensorEventListener(UCSApplication.getContext());
            if(dataManager.equals("SENSOR")){
                sensorEventListener.startRegisteringSensor(SensorHelper.getSensorTypeFromStringType(sensorType));
            } else if(dataManager.equals("LOCATION")){
                sensorEventListener.startRegisteringLocation();
            }
            return true;
        } catch( Exception e ) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public void update(String json) throws PIPException {
        log.severe( "Update is unimplemented" );
    }

    @Override
    public void retrieve( RequestType request,
                          List<Attribute> attributeRetrievals ) {
        log.severe( "Multiple retrieve is unimplemented" );
    }

    @Override
    public void subscribe( RequestType request,
                           List<Attribute> attributeRetrieval ) {
        log.severe( "Multiple subscribe is unimplemented" );
    }

    @Override
    public void performObligation( ObligationInterface obligation ) {
        log.severe( "Perform obligation is unimplemented" );
    }

    protected String read() throws PIPException {
        log.info("Reading from eventListener...");
        String result = SensorHelper.read(sensorEventListener, sensorType);
        log.severe("Read result: " + result);
        return result;
    }

    @Override
    protected String read(String filter) throws PIPException {
        return null;
    }

    public String getSensorType() {
        return sensorType;
    }

    public void setSensorType(String sensorType) {
        this.sensorType = sensorType;
    }

}
