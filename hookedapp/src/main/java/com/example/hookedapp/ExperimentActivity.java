package com.example.hookedapp;

import android.content.Intent;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import java.util.logging.Logger;

import utility.FileLogger;

/**
 * Activity meant to set up experiment for calculating timing overhead
 * due to UCS
 */
public class ExperimentActivity extends AppCompatActivity {
    String policyPath = "default";
    private static Logger log = Logger.getLogger( ExperimentActivity.class.getName() );

    private TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_experiment2);

        textView = findViewById(R.id.textView);

        Intent i = getIntent();
        String intentPolicyPath = i.getStringExtra("policyPath");
        String accessType = i.getStringExtra("accessType");

        if(accessType.equals("tryAccess")){
            FileLogger.appendLog(System.currentTimeMillis() + "\t" + "ENTER" + "\t" + intentPolicyPath);
            if(intentPolicyPath != null)
                policyPath = intentPolicyPath;
            method_to_hook(policyPath);
        } else if(accessType.equals("startAccess")) {
            if(intentPolicyPath != null)
                policyPath = intentPolicyPath;
            method_to_hook_start_access(policyPath);
        }
    }

    @Override
    protected void onResume(){
        super.onResume();
    }

    protected void method_to_hook(String policyPath){
        FileLogger.appendLog(System.currentTimeMillis() + "\t" + "EXIT" + "\t" + policyPath);
        Toast.makeText(getApplicationContext(), policyPath, Toast.LENGTH_SHORT).show();
    }


    /**
     * Method that will be called when a start access requests returns Permit.
     */
    public void method_to_hook_start_access(String policyPath){
        textView.setText("Access Granted");
        textView.setTextColor(Color.GREEN);
    }

    /**
     * Method that will be called when a ongoing session will be evaluated as Deny. Useful for measuring
     * the Inconsistency time.
     */
    public void method_to_hook_revoke_access(){
        FileLogger.appendLog(System.currentTimeMillis() + "\t" + "EXIT" + "\t" + policyPath);
        textView.setText("Access Denied");
        textView.setTextColor(Color.RED);
    }
}