package com.example.uibasics;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements RecordFragment.OnRecordControlListener {

    private DataExport dataExport;
    private long recordingStartTime;
    private boolean isAccelEnabled, isGyroEnabled, isGPSEnabled;
    private static final int REQUEST_LOCATION_PERMISSION = 1;
    private IMUDataCollector imuDataCollector;
    private GPSCollector gpsCollector;
    private PlotFragment plotFragment;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        Sensor gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        if (gyroSensor != null) {
            Log.d("MainActivity", "Gyroscope IS available on this device.");
        } else {
            Log.d("MainActivity", "No gyroscope on this device!");
        }

        // Initialize fragments
        RecordFragment recordFragment = new RecordFragment();
        plotFragment = new PlotFragment();

        ViewPager2 viewPager = findViewById(R.id.viewPager);
        TabLayout tabLayout = findViewById(R.id.tabLayout);

        ViewPagerAdapter adapter = new ViewPagerAdapter(this);
        adapter.addFragment(recordFragment, "Record");
        adapter.addFragment(plotFragment, "Plot"); // Pass the same instance!

        viewPager.setAdapter(adapter);

        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> tab.setText(adapter.getTitle(position))
        ).attach();
    }
    @Override
    public void onStartRecording(boolean accel, boolean gyro, boolean gps) {
        Log.d("MainActivity", "onStartRecording called with accel: " + accel + ", gyro: " + gyro + ", gps: " + gps);
        isAccelEnabled = accel;
        isGyroEnabled = gyro;
        isGPSEnabled = gps;

        recordingStartTime = System.currentTimeMillis();

        // Initialize dataExporter
        dataExport = new DataExport();

        // Debug: Check plotFragment
        if (plotFragment != null) {
            Log.d("MainActivity", "plotFragment is initialized and ready!");
        } else {
            Log.e("MainActivity", "plotFragment is NULL — no data will be plotted.");
        }
        // Start IMU data collection with plotFragment
        imuDataCollector = new IMUDataCollector(this, dataExport, isAccelEnabled, isGyroEnabled, plotFragment);
        imuDataCollector.start();

        // Start GPS data collection
        if (isGPSEnabled) {
            gpsCollector = new GPSCollector(this, dataExport, recordingStartTime);
            gpsCollector.start();
        }
    }

    @Override
    public void onEventRecorded() {
        // Record the event timestamp relative to the recording start
        long relativeTime = System.currentTimeMillis() - recordingStartTime;
        /*if (dataExport != null) {
            dataExport.addEvent(relativeTime);
        }*/
        ArrayList<String[]> rows = dataExport.getSensorData("Synchronized");
        if (rows != null && !rows.isEmpty()) {
            String[] lastRow = rows.get(rows.size() - 1);
            lastRow[7] = String.valueOf(relativeTime);
        }
    }
    @Override
    public void onStopRecording() {
        if (imuDataCollector != null) {
            imuDataCollector.stop();
        }
        if (gpsCollector != null) {
            gpsCollector.stop();
        }
    }
    //Request permission to access GPS
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, start GPS updates
                onStartRecording(isAccelEnabled, isGyroEnabled, isGPSEnabled);
            } else {
                Toast.makeText(this, "Location permission denied.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onExportRecording(String recordingName) {
        // Save the file in app-specific external storage (no permissions needed!)
        File externalDir = getExternalFilesDir(null);
        File zipFile = new File(externalDir, recordingName + ".zip");

        if (dataExport.exportAsZip(zipFile)) {
            Toast.makeText(this, "Export complete: " + zipFile.getAbsolutePath(), Toast.LENGTH_LONG).show();

            // Share the file
            Uri fileUri = FileProvider.getUriForFile(this, getPackageName() + ".provider", zipFile);
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("application/zip");
            shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(shareIntent, "Share ZIP file via:"));
        } else {
            Toast.makeText(this, "Export failed: Could not write zip", Toast.LENGTH_SHORT).show();
        }
    }


}


