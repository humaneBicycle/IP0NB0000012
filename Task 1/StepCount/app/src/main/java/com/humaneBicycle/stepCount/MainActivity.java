package com.humaneBicycle.stepCount;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;

import android.widget.TextView;

import com.humaneBicycle.stepCount.model.Accelerometer;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;


public class MainActivity extends AppCompatActivity {

    TextView x, y, z, androidSensorCount, implementedCount;
    float stepAndroidSensor=0f, initSteps=0f, flag=0f;
    private LineGraphSeries<DataPoint> rawData;
    private LineGraphSeries<DataPoint> lpData;
    private int rawPoints = 0;
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

        //initializing the views and graph
        x=findViewById(R.id.x_acc);
        y=findViewById(R.id.y_acc);
        z=findViewById(R.id.z_acc);
        androidSensorCount=findViewById(R.id.build_in_sensor);
        implementedCount=findViewById(R.id.implemented_number);

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

        //initializing the sensors and sensor manager
        SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        Sensor sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        Sensor stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);

        //initializing the series.
        rawData=new LineGraphSeries<>();
        lpData=new LineGraphSeries<>();

        //adding the series to the graph
        accelerationGraph.addSeries(rawData);
        lowPassFilterGraph.addSeries(lpData);

        //registering the sensors
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

                //passing the data through the low pass filter
                prev = lowPassFilter(event.values,prev);

                //creating the accelerometer object
                Accelerometer raw = new Accelerometer(event.values);
                Accelerometer data = new Accelerometer(prev);

                //appending the values to graph.
                rawData.appendData(new DataPoint(rawPoints++,raw.acceleration), true,1000);
                lpData.appendData(new DataPoint(rawPoints, data.acceleration), true, 1000);

                //detecting the peak
                if(data.acceleration > 10.5f){
                    CURRENT_STATE = ABOVE;
                    if(PREVIOUS_STATE != CURRENT_STATE) {
                        streakStartTime = System.currentTimeMillis();
                        if ((streakStartTime - streakPrevTime) <= 250f) {
                            streakPrevTime = System.currentTimeMillis();
                            return;
                        }
                        streakPrevTime = streakStartTime;
                        stepCount++;
                        implementedCount.setText(Integer.toString(stepCount));
                    }
                    PREVIOUS_STATE = CURRENT_STATE;
                }
                else if(data.acceleration < 10.5f) {
                    CURRENT_STATE = BELOW;
                    PREVIOUS_STATE = CURRENT_STATE;
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {

            }
        }, sensor,SensorManager.SENSOR_DELAY_UI);

        //registering the sensor which counts steps which is pre-implemented by Android
        sensorManager.registerListener(new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
                //Note: The step count is not reset when the activity is exited. So
                //resetting it to zero every time application opens using flag variable.
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
        }, stepSensor,SensorManager.SENSOR_DELAY_FASTEST);
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