package com.humaneBicycle.stepCount;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.MenuProvider;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    TextView x, y, z, androidSensorCount, implementedCount;

    private SensorManager sensorManager;
    private Sensor sensor, stepSensor;
    float stepAndroidSensor=0f, initSteps=0f, flag=0f;
    ;


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu,menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar myToolbar = findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);
        MenuProvider menuProvider = new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {

            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                switch (menuItem.getItemId()){
                    case R.id.graph:
                        startActivity(new Intent(MainActivity.this,GraphActivity.class));
                        break;


                }
                return false;
            }
        };
        myToolbar.addMenuProvider(menuProvider);

        x=findViewById(R.id.x_acc);
        y=findViewById(R.id.y_acc);
        z=findViewById(R.id.z_acc);

        androidSensorCount=findViewById(R.id.build_in_sensor);
        implementedCount=findViewById(R.id.implemented_number);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        stepSensor=sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);

        sensorManager.registerListener(new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                float x_acc = event.values[0];
                float y_acc = event.values[1];
                float z_acc = event.values[2];
                x.setText(Float.toString(x_acc));
                y.setText(Float.toString(y_acc));
                z.setText(Float.toString(z_acc));
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {

            }
        },sensor,SensorManager.SENSOR_DELAY_UI);

        sensorManager.registerListener(new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
                //Note: The step count is not reset when the activity is exited. So resetting it to zero every time application opens using flag variable.
                if(flag==0){
                    initSteps=sensorEvent.values[0];
                    flag++;
                }

                stepAndroidSensor=sensorEvent.values[0];
                androidSensorCount.setText(Float.toString(stepAndroidSensor-initSteps));
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {


            }
        },stepSensor,SensorManager.SENSOR_DELAY_FASTEST);

    }




}