package com.humaneBicycle.stepCount;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;

public class GraphActivity extends AppCompatActivity {

    GraphView accelerationGraph;
    private SensorManager sensorManager;
    private Sensor sensor, stepSensor;
    float accelerationGraphXValue=0f;

    LineGraphSeries<DataPoint> series;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_graph);

        accelerationGraph = findViewById(R.id.graph);
        accelerationGraph.getViewport().setXAxisBoundsManual(true);
        accelerationGraph.getViewport().setMinX(0);
        accelerationGraph.getViewport().setMaxX(350);

        series = new LineGraphSeries<>();


        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);


        accelerationGraph.addSeries(series);

        sensorManager.registerListener(new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                float x_acc = event.values[0];
                float y_acc = event.values[1];
                float z_acc = event.values[2];

                double a = Math.sqrt(x_acc*x_acc + y_acc*y_acc + z_acc*z_acc);

                Log.d("abh", "called: ");
                accelerationGraphXValue+=1;
                series.appendData(new DataPoint(accelerationGraphXValue,(float)a),true,5000);
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {

            }
        },sensor,SensorManager.SENSOR_DELAY_UI);

    }



}