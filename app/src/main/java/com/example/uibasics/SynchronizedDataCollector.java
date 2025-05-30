package com.example.uibasics;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

import androidx.core.app.ActivityCompat;

public class SynchronizedDataCollector implements SensorEventListener {
    private final SensorManager sensorManager;
    private final Sensor accelerometer;
    private final Sensor gyroscope;
    private final LocationManager locationManager;
    private final Context context;
    private final DataExport dataExport;
    private long recordingStartTime;

    private final boolean isAccelEnabled;
    private final boolean isGyroEnabled;
    private final boolean isGPSEnabled;
    private final PlotFragment plotFragment;
    // For synchronized data
    private float[] latestAccel = new float[3];
    private float[] latestGyro = new float[3];
    private double latitude = 0, longitude = 0;
    private boolean hasAccel = false, hasGyro = false, hasGPS = false;
    private boolean hasGPSFix = false;

    public SynchronizedDataCollector(Context context, DataExport dataExport,
                                     boolean isAccelEnabled, boolean isGyroEnabled, boolean isGPSEnabled,
                                     long recordingStartTime, PlotFragment plotFragment) {

        this.context = context;
        this.dataExport = dataExport;
        this.isAccelEnabled = isAccelEnabled;
        this.isGyroEnabled = isGyroEnabled;
        this.isGPSEnabled = isGPSEnabled;
        this.recordingStartTime = recordingStartTime;
        this.plotFragment = plotFragment;

        this.sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        this.accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        this.gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        this.locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
    }
    public DataExport getDataExport() {
        return dataExport;
    }
    public void start() {
        Log.d("SynchronizedDataCollector", "Starting data collection...");
        recordingStartTime = System.currentTimeMillis();

        // Register accelerometer if enabled
        if (isAccelEnabled && accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
            Log.d("SynchronizedDataCollector", "Accelerometer listener registered");
        }

        // Register gyroscope if enabled
        if (isGyroEnabled && gyroscope != null) {
            sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_GAME);
            Log.d("SynchronizedDataCollector", "Gyroscope listener registered");
        }

        // Start GPS updates if enabled
        if (isGPSEnabled) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                // If permission is not granted, request it
                Log.w("SynchronizedDataCollector", "Location permission not granted yet");
                ActivityCompat.requestPermissions(
                        (MainActivity) context,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        1
                );
                return; // Exit start() until permission granted
            }
            // Request location updates
            Log.d("SynchronizedDataCollector", "Requesting GPS location updates...");
            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    1000, // 1 second interval
                    0,    // 0m min distance
                    locationListener
            );
        }
    }

    public void stop() {
        sensorManager.unregisterListener(this);
        locationManager.removeUpdates(locationListener);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        long timestamp = System.currentTimeMillis() - recordingStartTime;

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            latestAccel = event.values.clone();
            hasAccel = true;
        }

        if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            latestGyro = event.values.clone();
            hasGyro = true;
        }

        // When both IMU sensors are updated, log them (no GPS yet)
        if (hasAccel && hasGyro) {
            addSynchronizedRow(timestamp);
            hasAccel = hasGyro = false;
        }
    }

    private void addSynchronizedRow(long timestamp) {
        String[] row = new String[10];
        row[0] = String.valueOf(timestamp);
        row[1] = String.valueOf(latestAccel[0]);
        row[2] = String.valueOf(latestAccel[1]);
        row[3] = String.valueOf(latestAccel[2]);
        row[4] = String.valueOf(latestGyro[0]);
        row[5] = String.valueOf(latestGyro[1]);
        row[6] = String.valueOf(latestGyro[2]);
        row[7] = String.valueOf(latitude);
        row[8] = String.valueOf(longitude);
        row[9] = "0"; // Default event column

        dataExport.addSensorRow("Synchronized", row);

        if (plotFragment != null && plotFragment.getActivity() != null) {
            plotFragment.getActivity().runOnUiThread(() -> {
                plotFragment.addAccelData(timestamp, latestAccel[0]);
                plotFragment.addGyroData(timestamp, latestGyro[0]);
            });
        }


        if (!hasGPSFix) {
            return; // skip logging if no GPS fix yet
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    // GPS updates
    private final LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            latitude = location.getLatitude();
            longitude = location.getLongitude();
            Log.d("SynchronizedDataCollector", "GPS updated: " + latitude + ", " + longitude);
            hasGPSFix = true;
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {}
        @Override
        public void onProviderEnabled(String provider) {}
        @Override
        public void onProviderDisabled(String provider) {}
    };

    // Method to add an event timestamp to the most recent row
    /*public void addEvent(long relativeTime) {
        // Find the last synchronized row and add the event timestamp
        if (!dataExport.getSensorData("Synchronized").isEmpty()) {
            String[] lastRow = dataExport.getSensorData("Synchronized").get(
                    dataExport.getSensorData("Synchronized").size() - 1
            );
            lastRow[9] = String.valueOf(relativeTime); // column 10
        }
    }*/
}
