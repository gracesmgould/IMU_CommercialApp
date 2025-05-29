package com.example.uibasics;

import static android.app.ProgressDialog.show;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;
import android.widget.Toast;

public class IMUDataCollector implements SensorEventListener {
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor gyroscope;
    private DataExport dataExport;
    private long recordingStartTime;
    private boolean isAccelEnabled, isGyroEnabled;
    private PlotFragment plotFragment;
    private Context context;

    private float[] lastAccel = new float[3];
    private float[] lastGyro = new float[3];


    public IMUDataCollector(Context context, DataExport dataExport,
                            boolean isAccelEnabled, boolean isGyroEnabled, PlotFragment plotFragment) { //Constructor
        this.sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        this.dataExport = dataExport;
        this.isAccelEnabled = isAccelEnabled;
        this.isGyroEnabled = isGyroEnabled;
        this.recordingStartTime = System.currentTimeMillis();
        this.plotFragment = plotFragment;
        this.context = context;

        if (isAccelEnabled) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }
        if (isGyroEnabled) {
            gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        }
    }

    public void start() {
        //Log.d("IMUDataCollector", "start() called. isAccelEnabled: " + isAccelEnabled + ", isGyroEnabled: " + isGyroEnabled);
        if (isAccelEnabled && accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        }
        if (isGyroEnabled && gyroscope != null) {
            sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_GAME);
        }
    }

    public void stop() {
        //Log.d("IMUDataCollector", "stop() called. isAccelEnabled: " + isAccelEnabled + ", isGyroEnabled: " + isGyroEnabled);
        sensorManager.unregisterListener(this);
    }
/*
    @Override
    public void onSensorChanged(SensorEvent event) {

        long timestamp = System.currentTimeMillis() - recordingStartTime;

        // TEMPORARY: show a toast to confirm this runs
        //Toast.makeText(context , "Sensor data!", Toast.LENGTH_SHORT).show();

        //Log.d("IMUDataCollector", "Accel data: " + event.values[0] + ", " + event.values[1] + ", " + event.values[2]);
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER && isAccelEnabled) {
            String[] accRow = {
                    String.valueOf(timestamp),
                    String.valueOf(event.values[0]),
                    String.valueOf(event.values[1]),
                    String.valueOf(event.values[2])
            };
            dataExport.addSensorRow("Accelerometer", accRow);
            if (plotFragment != null) {
                plotFragment.addAccelData(timestamp, event.values[0]); // Only X for now
            }
        }

        if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE && isGyroEnabled) {
           Log.d("IMUDataCollector", "Gyro data: " + event.values[0] + ", " + event.values[1] + ", " + event.values[2]);
            String[] gyroRow = {
                    String.valueOf(timestamp),
                    String.valueOf(event.values[0]),
                    String.valueOf(event.values[1]),
                    String.valueOf(event.values[2])
            };
            dataExport.addSensorRow("Gyroscope", gyroRow);
            if (plotFragment != null) {
                plotFragment.addGyroData(timestamp, event.values[0]); // Only X for now
            }
        }
    }

 */
@Override
public void onSensorChanged(SensorEvent event) {
    long timestamp = System.currentTimeMillis() - recordingStartTime;

    // Update the latest sensor data
    if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER && isAccelEnabled) {
        lastAccel = event.values.clone();
    }
    if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE && isGyroEnabled) {
        lastGyro = event.values.clone();
    }

    // Write a combined row whenever either sensor updates
    String[] row = {
            String.valueOf(timestamp),
            String.valueOf(lastAccel[0]),
            String.valueOf(lastAccel[1]),
            String.valueOf(lastAccel[2]),
            String.valueOf(lastGyro[0]),
            String.valueOf(lastGyro[1]),
            String.valueOf(lastGyro[2]),
            "" // event_time placeholder
    };
    dataExport.addSensorRow("Synchronized", row);

    // Update the plots
    if (plotFragment != null) {
        plotFragment.addAccelData(timestamp, lastAccel[0]);
        plotFragment.addGyroData(timestamp, lastGyro[0]);
    }
}
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}
}

