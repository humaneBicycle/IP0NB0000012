package com.humaneBicycle.stepCount;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;

import android.widget.TextView;
import android.widget.Toast;

import com.humaneBicycle.stepCount.model.Accelerometer;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;


public class MainActivity extends AppCompatActivity {

    TextView x, y, z, androidSensorCount, implementedCount;

    private SensorManager sensorManager;
    private Sensor sensor, stepSensor;
    float stepAndroidSensor=0f, initSteps=0f, flag=0f;
    private LineGraphSeries<DataPoint> rawData;
    private LineGraphSeries<DataPoint> lpData;
    private int rawPoints = 0;

    boolean SAMPLING_ACTIVE = true;

    private int sampleCount = 0;

    private float[] prev = {0f,0f,0f};

    private int stepCount = 0;
    private static final int ABOVE = 1;
    private static final int BELOW = 0;
    private static int CURRENT_STATE = 0;
    private static int PREVIOUS_STATE = BELOW;
    private long streakStartTime;
    private long streakPrevTime;
    GraphView accelerationGraph, lowPassFilterGraph;
    private long startTime;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        accelerationGraph = findViewById(R.id.accelerationGraph);
        accelerationGraph.setTitle("Acceleration Magnitude");
        accelerationGraph.getViewport().setXAxisBoundsManual(true);
        accelerationGraph.getViewport().setMinX(0);
        accelerationGraph.getViewport().setMaxX(350);

        lowPassFilterGraph = findViewById(R.id.lowPassFilterGraph);
        lowPassFilterGraph.setTitle("Low Pass Filter Graph");
        lowPassFilterGraph.getViewport().setXAxisBoundsManual(true);
        lowPassFilterGraph.getViewport().setMinX(0);
        lowPassFilterGraph.getViewport().setMaxX(350);


        x=findViewById(R.id.x_acc);
        y=findViewById(R.id.y_acc);
        z=findViewById(R.id.z_acc);

        androidSensorCount=findViewById(R.id.build_in_sensor);
        implementedCount=findViewById(R.id.implemented_number);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        stepSensor=sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);

        rawData=new LineGraphSeries<>();
        lpData=new LineGraphSeries<>();

        accelerationGraph.addSeries(rawData);
        lowPassFilterGraph.addSeries(lpData);
        sensorManager.registerListener(new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                float x_acc = event.values[0];
                float y_acc = event.values[1];
                float z_acc = event.values[2];
                //android values
                x.setText(Float.toString(x_acc));
                y.setText(Float.toString(y_acc));
                z.setText(Float.toString(z_acc));

                handleEvent(event);
                if(SAMPLING_ACTIVE) {
                    sampleCount++;
                    long now = System.currentTimeMillis();
                    if (now >= startTime + 5000) {
                        double samplingRate = sampleCount / ((now - startTime) / 1000.0);
                        SAMPLING_ACTIVE = false;
                        Toast.makeText(getApplicationContext(), "Sampling rate of your device is " + samplingRate + "Hz", Toast.LENGTH_LONG).show();

                    }
                }
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


    private void handleEvent(SensorEvent event) {
        prev = lowPassFilter(event.values,prev);
        Accelerometer raw = new Accelerometer(event.values);
        Accelerometer data = new Accelerometer(prev);
        StringBuilder text = new StringBuilder();
        text.append("X: " + data.X);
        text.append("Y: " + data.Y);
        text.append("Z: " + data.Z);
        text.append("R: " + data.acceleration);
        rawData.appendData(new DataPoint(rawPoints++,raw.acceleration), true,1000);
        lpData.appendData(new DataPoint(rawPoints, data.acceleration), true, 1000);


        if(data.acceleration > 10.5f){
            CURRENT_STATE = ABOVE;
            if(PREVIOUS_STATE != CURRENT_STATE) {
                streakStartTime = System.currentTimeMillis();
                if ((streakStartTime - streakPrevTime) <= 250f) {
                    streakPrevTime = System.currentTimeMillis();
                    return;
                }
                streakPrevTime = streakStartTime;
                Log.d("STATES:", "" + streakPrevTime + " " + streakStartTime);
                stepCount++;
            }
            PREVIOUS_STATE = CURRENT_STATE;
        }
        else if(data.acceleration < 10.5f) {
            CURRENT_STATE = BELOW;
            PREVIOUS_STATE = CURRENT_STATE;
        }
        implementedCount.setText(Integer.toString(stepCount));;

    }


    private float[] lowPassFilter(float[] input, float[] prev) {
        float ALPHA = 0.1f;
        if(input == null || prev == null) {
            return null;
        }
        for (int i=0; i< input.length; i++) {
            prev[i] = prev[i] + ALPHA * (input[i] - prev[i]);
        }
        return prev;
    }


}