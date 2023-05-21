package com.humanebicycle.estimateheartrate;

import static android.content.ContentValues.TAG;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.camera2.interop.Camera2CameraInfo;
import androidx.camera.camera2.interop.ExperimentalCamera2Interop;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.core.impl.ImageFormatConstants;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.video.FallbackStrategy;
import androidx.camera.video.Quality;
import androidx.camera.video.QualitySelector;
import androidx.camera.video.Recorder;
import androidx.camera.video.VideoCapture;
import androidx.camera.video.impl.VideoCaptureConfig;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraCharacteristics;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.util.concurrent.ListenableFuture;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends AppCompatActivity {
    Button startMeasureButton, stopMeasureButton;
    TextView heartRateTextView;
    GraphView graphView;
    PreviewView mPreviewView;
    Camera camera;
    int CAMERA_PERMISSION_REQUEST_CODE = 1098;
    ProcessCameraProvider cameraProvider;
    private static final AtomicBoolean processing = new AtomicBoolean(false);
    private static final int averageArraySize = 4;
    private static final int[] averageArray = new int[averageArraySize];
    public enum TYPE {
        GREEN, RED
    }
    private static TYPE currentType = TYPE.GREEN;
    private static double beats = 0;
    private static int averageIndex = 0;
    private static long startTime = 0;
    private static int beatsIndex = 0;
    private static final int beatsArraySize = 3;
    private static final int[] beatsArray = new int[beatsArraySize];
    private LineGraphSeries<DataPoint> rawData;
    private int rawPoints = 0;
    ImageAnalysis imageAnalysis;
    boolean isMeasuring = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        startMeasureButton = findViewById(R.id.start_measure);
        stopMeasureButton = findViewById(R.id.stop_measure);
        heartRateTextView = findViewById(R.id.heart_rate);
        graphView = findViewById(R.id.graph);
        mPreviewView = findViewById(R.id.camera_view);

        if(checkCameraPermission()){
            startCamera();
        }else{
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
        }
        rawData=new LineGraphSeries<>();
        graphView.addSeries(rawData);
        graphView.getGridLabelRenderer().setVerticalLabelsVisible(false);
        graphView.getGridLabelRenderer().setHorizontalLabelsVisible(false);

        startMeasureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(checkCameraPermission()) {
                    startRecording();
                }else{
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{android.Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
                }
            }
        });

        stopMeasureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isMeasuring=false;
                camera.getCameraControl().enableTorch(false);
                graphView.removeAllSeries();

                //TODO reset data
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    public boolean checkCameraPermission() {
        return ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Camera permission granted, proceed with camera usage
                startRecording();
            } else {
                // Camera permission denied, handle accordingly (e.g., show an error message)
                Toast.makeText(this, "Camera permission denied, Can't use app without camera!!!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void startRecording() {
        try {
            camera.getCameraControl().enableTorch(true);
            isMeasuring = true;
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "startRecording: "+e );
        }
    }

    private void startCamera() {
        final ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    cameraProvider = cameraProviderFuture.get();
                    bindPreview(cameraProvider);
                } catch (ExecutionException | InterruptedException e) {
                    // No errors need to be handled for this Future.
                    // This should never be reached.
                    Log.d(TAG, "unable to bind the camera with the preview");
                }
            }
        }, ContextCompat.getMainExecutor(this));
    }

    void bindPreview(@NonNull ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder()
                .build();

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();


        imageAnalysis = new ImageAnalysis.Builder()
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                .build();
        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), analyzer);


        Recorder recorder = new Recorder.Builder().
                setQualitySelector(QualitySelector.from(Quality.SD, FallbackStrategy.lowerQualityThan(Quality.HD))).
                build();
        VideoCapture<Recorder> videoCapture = VideoCapture.withOutput(recorder);

        preview.setSurfaceProvider(mPreviewView.getSurfaceProvider());
        camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis, videoCapture);
    }

    ImageAnalysis.Analyzer analyzer = new ImageAnalysis.Analyzer() {
        @SuppressLint("UnsafeOptInUsageError")
        @Override
        public void analyze(@NonNull ImageProxy image) {
            byte[] data = ImageProcessing.getNV21(image.getWidth(),image.getHeight(),image.toBitmap());

            image.close();
            processFrame(data,image.getHeight(),image.getWidth());
        }
    };

    void processFrame(byte[] data, int height, int width){
        if(isMeasuring) {

            if (!processing.compareAndSet(false, true)) return;

            int imgAvg = ImageProcessing.decodeYUV420SPtoRedAvg(data.clone(),width, height);

            rawPoints++;
            rawData.appendData(new DataPoint(rawPoints, imgAvg*10), false, 2);


            if (imgAvg == 0 || imgAvg == 255) {
                processing.set(false);
                return;
            }

            int averageArrayAvg = 0;
            int averageArrayCnt = 0;
            for (int k : averageArray) {
                if (k > 0) {
                    averageArrayAvg += k;
                    averageArrayCnt++;
                }
            }

            int rollingAverage = (averageArrayCnt > 0) ? (averageArrayAvg / averageArrayCnt) : 0;
            TYPE newType = currentType;
            if (imgAvg < rollingAverage) {
                newType = TYPE.RED;
                if (newType != currentType) {
                    beats++;
                    Log.d(TAG, "BEAT!! beats=" + beats);
                }
            } else if (imgAvg > rollingAverage) {
                newType = TYPE.GREEN;
            }

            if (averageIndex == averageArraySize) averageIndex = 0;
            averageArray[averageIndex] = imgAvg;
            averageIndex++;

            if (newType != currentType) {
                currentType = newType;
            }

            long endTime = System.currentTimeMillis();
            double totalTimeInSecs = (endTime - startTime) / 1000d;
            if (totalTimeInSecs >= 10) {
                double bps = (beats / totalTimeInSecs);
                int dpm = (int) (bps * 60d);
                if (dpm < 30 || dpm > 180) {
                    startTime = System.currentTimeMillis();
                    beats = 0;
                    processing.set(false);
                    return;
                }

                if (beatsIndex == beatsArraySize) beatsIndex = 0;
                beatsArray[beatsIndex] = dpm;
                beatsIndex++;

                int beatsArrayAvg = 0;
                int beatsArrayCnt = 0;
                for (int j : beatsArray) {
                    if (j > 0) {
                        beatsArrayAvg += j;
                        beatsArrayCnt++;
                    }
                }
                int beatsAvg = (beatsArrayAvg / beatsArrayCnt);
                heartRateTextView.setText(String.valueOf(beatsAvg));
                startTime = System.currentTimeMillis();
                beats = 0;
            }
            processing.set(false);
        }
    }
}