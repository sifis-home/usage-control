package com.example.hookedapp;

import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import utility.FileLogger;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    Button button2, button3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        button2 = findViewById(R.id.button2);
        button3 = findViewById(R.id.button3);
        button2.setOnClickListener(this);
        button3.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.button2) {
            switch_color();
        } else if (v.getId() == R.id.button3) {
            switch_color_sensor();
        }
    }

    public void switch_color() {
        Toast.makeText(this, "Switch color being executed", Toast.LENGTH_LONG).show();
        button2.setBackgroundColor(Color.GREEN);
        button2.setText("ACCESS GRANTED");

    }

    public void switch_color_to_default() {
        Toast.makeText(this, "Switch color to default being executed", Toast.LENGTH_LONG).show();
        button2.setBackgroundColor(Color.RED);
        button2.setText("ACCESS DENIED");
    }

    public void switch_color_sensor() {
        Toast.makeText(this, "Switch color brutal being executed", Toast.LENGTH_LONG).show();
        button3.setBackgroundColor(Color.GREEN);
        button3.setText("ACCESS GRANTED (SENSOR)");
    }

    public void switch_color_sensor_to_default() {
        Toast.makeText(this, "Switch color to default being executed", Toast.LENGTH_LONG).show();
        button3.setBackgroundColor(Color.RED);
        button3.setText("ACCESS DENIED (SENSOR)");
    }
}