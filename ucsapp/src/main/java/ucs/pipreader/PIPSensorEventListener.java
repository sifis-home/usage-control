package ucs.pipreader;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.widget.Toast;

import com.example.ucsintent.UCSActivity;
import com.example.ucsintent.UCSApplication;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * This class is responsible for managing the update of data that comes from physical sensors or system services
 */
public class PIPSensorEventListener implements SensorEventListener, LocationListener {
    private static Logger log = Logger.getLogger(PIPSensorEventListener.class.getName());
    private SensorManager sensorManager;
    private Sensor sensor;
    private float[] values;

    private LocationManager locationManager;
    private Location location;

    private WifiManager wifiManager;
    private BatteryManager batteryManager;

    public PIPSensorEventListener(Context context) {
        log.info("PIPSensorEventListener");
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        batteryManager = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
    }

    @Override
    public void onAccuracyChanged(Sensor arg0, int arg1){

    }

    @Override
    public void onSensorChanged(SensorEvent event){
        log.info("onSensorChanged");
        if (sensor.getType() == event.sensor.getType()) {
            values = event.values.clone();
            log.info("Sensor changed");
//            Toast.makeText(UCSApplication.getContext(), "LightSensor changed: " + values[0], Toast.LENGTH_SHORT).show();
            for(float value: values){
                log.info("Sensor values: " + value);
            }
        }
    }

    public float[] getValues() {
        return values;
    }

    public Location getLocation() {
            return location;
    }

    public String getSSID(){
        return wifiManager.getConnectionInfo().getSSID().replaceAll("\"","");
    }
    public void startRegisteringSensor(int sensorType){
        log.info("startRegisteringSensor with id : " + sensorType);
        sensor = sensorManager.getDefaultSensor(sensorType);
        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    public void startRegisteringLocation(){
        log.info("startRegisteringLocation");
        if (ActivityCompat.checkSelfPermission(UCSApplication.getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(UCSApplication.getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            log.severe("Access Fine Location is not granted");
            return;
        }
        location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 10, this);
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 2000, 10, this);

    }

    @Override
    public void onLocationChanged(Location location) {
//        Toast.makeText(UCSApplication.getContext(), "Location changed: Latitude: " + location.getLatitude() + ", Longitude: " + location.getLongitude(), Toast.LENGTH_SHORT).show();
        log.info("onLocationChanged. Latitude: " + location.getLatitude() + ", Longitude: " + location.getLongitude());
        this.location = location;
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }
}
