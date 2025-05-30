package com.example.uibasics;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public class IMUDataCollector implements SensorEventListener {
    final SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor gyroscope;
    final DataExport dataExport;
    final long recordingStartTime;
    final PlotFragment plotFragment;

    // For synchronizing readings
    final float[] latestAccel = new float[3];
    final float[] latestGyro = new float[3];
    private long latestAccelTime = -1;
    private long latestGyroTime = -1;

    public IMUDataCollector(Context context, DataExport dataExport,
                            boolean isAccelEnabled, boolean isGyroEnabled, PlotFragment plotFragment) {
        this.sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        this.dataExport = dataExport;
        this.plotFragment = plotFragment;
        this.recordingStartTime = System.currentTimeMillis();

        if (isAccelEnabled) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }
        if (isGyroEnabled) {
            gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        }
    }

    public void start() {
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        }
        if (gyroscope != null) {
            sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_GAME);
        }
    }

    public void stop() {
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        long timestamp = System.currentTimeMillis() - recordingStartTime;

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, latestAccel, 0, 3);
            latestAccelTime = timestamp;
            plotFragment.addAccelData(timestamp, event.values[0]);
        }

        if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            System.arraycopy(event.values, 0, latestGyro, 0, 3);
            latestGyroTime = timestamp;
            plotFragment.addGyroData(timestamp, event.values[0]);
        }

        // Only write synchronized row when we have fresh data from both
        if (latestAccelTime > 0 && latestGyroTime > 0) {
            long syncTime = Math.max(latestAccelTime, latestGyroTime);

            String[] row = {
                    String.valueOf(syncTime),
                    String.valueOf(latestAccel[0]),
                    String.valueOf(latestAccel[1]),
                    String.valueOf(latestAccel[2]),
                    String.valueOf(latestGyro[0]),
                    String.valueOf(latestGyro[1]),
                    String.valueOf(latestGyro[2]),
                    ""  // Event column left blank
            };
            dataExport.addSensorRow("Synchronized", row);

            // Reset timestamps so next row is only created with fresh data from both
            latestAccelTime = -1;
            latestGyroTime = -1;
        }
    }
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}
}

