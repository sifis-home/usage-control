package ucs.pipreader;

import android.hardware.Sensor;
import android.location.Location;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Objects;
import java.util.logging.Logger;

import ucs.requestmanager.RequestManager;

/**
 * Utility class for managing data coming from different sensors in PIPSensorReader.
 */
public class SensorHelper {
    private static final Logger log = Logger.getLogger( SensorHelper.class.getName() );

    /**
     * Return integer representation of a sensor type
     * @param sensorType String representation of a sensor type
     * @return
     */
    public static int getSensorTypeFromStringType(String sensorType){
        if(sensorType.endsWith("TYPE_LIGHT")){
            return Sensor.TYPE_LIGHT;
        } else {
            return -1;
        }
    }

    /**
     * Return the type of provider for the given data.
     * @param sensorType String representation of a sensor type
     * @return
     */
    public static String getDataManagerName(String sensorType){
        if(sensorType.startsWith("SENSOR"))
            return "SENSOR";
        else if(sensorType.startsWith("LOCATION"))
            return "LOCATION";
        else if(sensorType.startsWith("WIFI"))
            return "WIFI";
        else if(sensorType.startsWith("TIME"))
            return "TIME";
        return "";
    }

    /**
     * Return significant data from values vector, depending on sensor type
     * @param sensorType String representation of a sensor type
     * @param values Vector of data associated which represent a generic sensor reading
     * @return
     */
    public static String getStringFromValues(int sensorType, float[] values){
        if(sensorType == Sensor.TYPE_LIGHT){
            if(values == null) {
                log.info("Reading from null sensor");
                return Float.toString(0);
            }
            return Float.toString(values[0]);
        } else {
            return "";
        }
    }

    /**
     * Return data as a String from different sources (Sensor, Location Manager etc.)
     * @param sensorType String representation of a sensor type
     * @param eventListener Event Listener associated with mutable sensors and managers
     * @return
     */
    public static String read(PIPSensorEventListener eventListener, String sensorType){
        if(Objects.equals(getDataManagerName(sensorType), "SENSOR")){
            log.info("Reading from sensor");
            float[] values = eventListener.getValues();
            return getStringFromValues(getSensorTypeFromStringType(sensorType), values);
        } else if(Objects.equals(getDataManagerName(sensorType), "LOCATION")){
            log.info("Reading from location");
            Location location = eventListener.getLocation();
            if(sensorType.endsWith("LONGITUDE"))
                return Double.toString(location.getLongitude());
            else if(sensorType.endsWith("LATITUDE"))
                return Double.toString(location.getLatitude());
        } else if(Objects.equals(getDataManagerName(sensorType), "WIFI")){
            log.info("Reading from Wifi");
            if(sensorType.endsWith("SSID"))
                return eventListener.getSSID();
        } else if(Objects.equals(getDataManagerName(sensorType), "TIME")){
            Date currentTime = Calendar.getInstance().getTime();
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
            log.info("date format: " + dateFormat.format(currentTime) + ", time format: " + timeFormat.format(currentTime));
            if(sensorType.endsWith("TIME"))
                return timeFormat.format(currentTime);
            else if(sensorType.endsWith("DATE"))
                return dateFormat.format(currentTime);
        }
        return null;
    }
}
