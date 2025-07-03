package com.humbl.imuapp;

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
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

public class SynchronizedDataCollector implements SensorEventListener {
    private final SensorManager sensorManager;
    private final Sensor accelerometer;
    private final Sensor gyroscope;
    private final LocationManager locationManager;
    private final Context context;
    private final DataExport dataExport;
    private long recordingStartTime;
    private long accelTimestamp = -1;
    private long gyroTimestamp = -1;
    private long gpsTimestamp = -1;
    private final boolean isAccelEnabled;
    private final boolean isGyroEnabled;
    private final boolean isGPSEnabled;
    private final PlotFragment plotFragment;
    // For synchronized data
    private float[] latestAccel = new float[3];
    private float[] latestGyro = new float[3];
    private double latitude = 0, longitude = 0;
    private boolean hasAccel = false, hasGyro = false;
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
    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public boolean hasValidGPSFix() {
        return hasGPSFix;
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
                        MainActivity.REQUEST_LOCATION_PERMISSION
                );
                return; // Exit start() until permission granted
            }
            // Request location updates
            Log.d("SynchronizedDataCollector", "Requesting GPS location updates...");
            locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    1000, //gps updates once every second
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
        long eventTime = System.currentTimeMillis() - recordingStartTime;

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            latestAccel = event.values.clone();
            accelTimestamp = eventTime;
            hasAccel = true;

            // Add plotting for individual axes
            if (plotFragment != null) {
                plotFragment.addAccelData(accelTimestamp, latestAccel[0], latestAccel[1], latestAccel[2]);
            }
        }

        if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            latestGyro = event.values.clone();
            gyroTimestamp = eventTime;
            hasGyro = true;

            // Add plotting for individual axes
            if (plotFragment != null) {
                plotFragment.addGyroData(gyroTimestamp, latestGyro[0], latestGyro[1], latestGyro[2]);
            }
        }

        if (hasAccel && hasGyro) {
            addCombinedRow(accelTimestamp, gyroTimestamp);
            hasAccel = hasGyro = false;
        }
    }

    private void addCombinedRow(long accelTime, long gyroTime) {
        String[] row = new String[13];
        row[0] = String.valueOf(accelTime);         // accel_time
        row[1] = String.valueOf(latestAccel[0]);    // ax
        row[2] = String.valueOf(latestAccel[1]);    // ay
        row[3] = String.valueOf(latestAccel[2]);    // az
        row[4] = String.valueOf(gyroTime);          // gyro_time
        row[5] = String.valueOf(latestGyro[0]);     // gx
        row[6] = String.valueOf(latestGyro[1]);     // gy
        row[7] = String.valueOf(latestGyro[2]);     // gz
        row[11] = "";
        row[12] = "";                               //Event description
        if (hasGPSFix) {
            row[8] = String.valueOf(gpsTimestamp);
            row[9] = String.valueOf(latitude);
            row[10] = String.valueOf(longitude);
        } else {
            row[8] = "0";     //
            row[9] = "0";
            row[10] = "0";
        }

        dataExport.addSensorRow("Synchronized", row);

        if (plotFragment != null && plotFragment.getActivity() != null) {
            plotFragment.getActivity().runOnUiThread(() -> {
                plotFragment.addAccelData(accelTime, latestAccel[0], latestAccel[1], latestAccel[2]);
                plotFragment.addGyroData(gyroTime, latestGyro[0], latestGyro[1], latestGyro[2]);
            });
        }
    }
    public void recordEventWithDialog() {
        ArrayList<String[]> rows = dataExport.getSensorData("Synchronized");

        if (rows == null || rows.isEmpty()) {
            Log.w("SynchronizedDataCollector", "No data rows to attach event to");
            return;
        }

        // Get the most recent row
        String[] lastRow = rows.get(rows.size() - 1);

        // Insert timestamp in column 12
        String timeFormatted = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
                .format(new Date(System.currentTimeMillis()));
        lastRow[11] = timeFormatted;

        // Show dialog to get description and store in column 13
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Describe the Event");

        final EditText input = new EditText(context);
        input.setHint("Enter a short description...");
        builder.setView(input);

        builder.setPositiveButton("OK", (dialog, which) -> {
            String description = input.getText().toString();
            lastRow[12] = description.replace(",", " "); // avoid commas in CSV
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }
    public void recordEventDescription(long timestamp, String description) {
        ArrayList<String[]> rows = dataExport.getSensorData("Synchronized");
        if (rows != null && !rows.isEmpty()) {
            String[] lastRow = rows.get(rows.size() - 1);

            if (lastRow.length < 13) {
                lastRow = Arrays.copyOf(lastRow, 13);
                rows.set(rows.size() - 1, lastRow);
            }

            lastRow[11] = String.valueOf(timestamp);              // event_time
            lastRow[12] = description.replace(",", " ");          // event_description
            Log.d("EventWrite", "Saved description to background export row: " + Arrays.toString(lastRow));
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
            gpsTimestamp = System.currentTimeMillis() - recordingStartTime;
            hasGPSFix = true;
            Log.d("SynchronizedDataCollector", "GPS updated: " + latitude + ", " + longitude);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {}
        @Override
        public void onProviderEnabled(@NonNull String provider) {}
        @Override
        public void onProviderDisabled(@NonNull String provider) {}
    };

}