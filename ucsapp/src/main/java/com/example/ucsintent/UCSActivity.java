package com.example.ucsintent;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.util.logging.Level;
import java.util.logging.Logger;

import ucs.pep.LocalPep;
import utility.AndroidFileUtility;

/**
 * Main Activity for UCS on Android. Its main duty is to start UCSService as a foreground service.
 * Buttons can be used for debugging purposes to test the communication with UCSService.
 */
public class UCSActivity extends AppCompatActivity implements View.OnClickListener {
    private static Logger log = Logger.getLogger(UCSActivity.class.getName());
    private static final int REQUEST_PERMISSIONS = 100;
    private LocalPep pep;

    Button button, button2, button3, button4, button5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ucs2);

        //Initialize UCSService
        Intent service = new Intent(this, UCSService.class);
        startForegroundService(service);

        pep = new LocalPep(getApplicationContext());

        button = findViewById(R.id.button);
        button2 = findViewById(R.id.button2);
        button3 = findViewById(R.id.button3);
        button4 = findViewById(R.id.button4);
        button5 = findViewById(R.id.button5);
        button.setOnClickListener(this);
        button2.setOnClickListener(this);
        button3.setOnClickListener(this);
        button4.setOnClickListener(this);
        button5.setOnClickListener(this);

        check_permission();
    }

    private void check_permission() {
        if ((ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)) {
            if ((ActivityCompat.shouldShowRequestPermissionRationale(UCSActivity.this, android.Manifest.permission.ACCESS_FINE_LOCATION))) {

            } else {
                ActivityCompat.requestPermissions(UCSActivity.this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, android.Manifest.permission.ACCESS_FINE_LOCATION},
                        REQUEST_PERMISSIONS);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_PERMISSIONS: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    log.info("Permission were granted");
                else
                    Toast.makeText(getApplicationContext(), "Please allow the permission",
                            Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.button){
            log.log(Level.INFO, "TRY ACCESS");
            pep.tryAccess(getApplicationContext());
        } else if(v.getId() == R.id.button2){
            log.log(Level.INFO, "START ACCESS");
            pep.startAccess(getApplicationContext());
        } else if(v.getId() == R.id.button3){
            log.log(Level.INFO, "END ACCESS");
            pep.endAccess(getApplicationContext());
        } else if(v.getId() == R.id.button4){
            AndroidFileUtility.writeStringToFile("10", "pips/light.txt", getApplicationContext());
            Toast.makeText(this, "Sample policy result switched to DENY", Toast.LENGTH_SHORT).show();
        } else if(v.getId() == R.id.button5){
            AndroidFileUtility.writeStringToFile("0", "pips/light.txt", getApplicationContext());
            Toast.makeText(this, "Sample policy result switched to PERMIT", Toast.LENGTH_SHORT).show();
        }
    }
}