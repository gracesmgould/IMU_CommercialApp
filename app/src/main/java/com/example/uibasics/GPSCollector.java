package com.example.uibasics;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

public class GPSCollector {

    private final Context context;
    private final DataExport dataExport;
    private final long recordingStartTime;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private static final int REQUEST_LOCATION_PERMISSION = 1;

    public GPSCollector(Context context, DataExport dataExport, long recordingStartTime) {
        this.context = context;
        this.dataExport = dataExport;
        this.recordingStartTime = recordingStartTime;
    }

    public void start() {
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                long timestamp = System.currentTimeMillis() - recordingStartTime;
                String[] gpsRow = {
                        String.valueOf(timestamp),
                        String.valueOf(location.getLatitude()),
                        String.valueOf(location.getLongitude()),
                        String.valueOf(location.getAltitude())
                };
                dataExport.addSensorRow("GPS", gpsRow);
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {}

            @Override
            public void onProviderEnabled(String provider) {}

            @Override
            public void onProviderDisabled(String provider) {}
        };

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Request permission
            ActivityCompat.requestPermissions(
                    (MainActivity) context,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_LOCATION_PERMISSION
            );
            return;
        }

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, locationListener);
    }

    public void stop() {
        if (locationManager != null && locationListener != null) {
            locationManager.removeUpdates(locationListener);
        }
    }
}

