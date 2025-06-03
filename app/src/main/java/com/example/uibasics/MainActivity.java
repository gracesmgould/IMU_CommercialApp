package com.example.uibasics;

import static androidx.core.content.ContextCompat.getSystemService;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
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
    private static final int REQUEST_LOCATION_PERMISSION = 1; //Request code for GPS permissions
    private PlotFragment plotFragment; //fragment to plot live data
    private SynchronizedDataCollector synchronizedDataCollector;

    private final BroadcastReceiver exportReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String zipPath = intent.getStringExtra("ZIP_PATH");
            if (zipPath != null) {
                shareZipFile(zipPath);
            } else {
                Toast.makeText(context, "Export failed or no file found.", Toast.LENGTH_SHORT).show();
            }
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Checking if gyroscope is on the device - toast message if not available
        SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        Sensor gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        if (gyroSensor == null) {
            //Log.d("MainActivity", "No gyroscope on this device!"); //Uncomment for debugging
            Toast.makeText(this, "No Gyroscope detected", Toast.LENGTH_SHORT).show();
        }

        // Initialize fragments - plot and record
        RecordFragment recordFragment = new RecordFragment();
        plotFragment = new PlotFragment();

        //ViewPager setup- allow for swiping between screens
        ViewPager2 viewPager = findViewById(R.id.viewPager);
        TabLayout tabLayout = findViewById(R.id.tabLayout);
        ViewPagerAdapter adapter = new ViewPagerAdapter(this);
        adapter.addFragment(recordFragment, "Record");
        adapter.addFragment(plotFragment, "Plot"); // Pass the same instance!
        viewPager.setAdapter(adapter);

        //Links tabs with ViewPager2 (old version (ViewPager) caused issues)
        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> tab.setText(adapter.getTitle(position))
        ).attach();
    }
    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override
    public void onStartRecording(boolean accel, boolean gyro, boolean gps) {
        //Log.d("MainActivity", "onStartRecording called with accel: " + accel + ", gyro: " + gyro + ", gps: " + gps); //Uncomment for debugging of user selection checkboxes
        String recordingName = // get this from your EditText
                ((EditText) findViewById(R.id.editRecordingName)).getText().toString();

        getSharedPreferences("MyPrefs", MODE_PRIVATE)
                .edit()
                .putString("CURRENT_RECORDING_NAME", recordingName)
                .apply();
        //User preferences for checkboxes
        isAccelEnabled = accel;
        isGyroEnabled = gyro;
        isGPSEnabled = gps;

        // Start the background service for recording
        Intent serviceIntent = new Intent(this, SynchronizedData_BackgroundService.class);
        serviceIntent.putExtra("ACCEL_ENABLED", isAccelEnabled);
        serviceIntent.putExtra("GYRO_ENABLED", isGyroEnabled);
        serviceIntent.putExtra("GPS_ENABLED", isGPSEnabled);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            // For API 26 and up
            startForegroundService(serviceIntent);
        } else {
            // For API 24–25
            startService(serviceIntent);
        }

        // Start a local data collector for live plots (does not duplicate exported data!)
        recordingStartTime = System.currentTimeMillis();
        dataExport = new DataExport(); // Only for plotting (not exported)
        synchronizedDataCollector = new SynchronizedDataCollector(
                this, dataExport, isAccelEnabled, isGyroEnabled, isGPSEnabled, recordingStartTime, plotFragment
        );
        synchronizedDataCollector.start();

        //Register receiver
        super.onStart();
        IntentFilter filter = new IntentFilter("EXPORT_COMPLETED");

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            registerReceiver(exportReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(exportReceiver, filter);
        }


    }
    @Override
    public void onEventRecorded() {
        long relativeTime = System.currentTimeMillis() - recordingStartTime;

        // Send event to the Service
        Intent eventIntent = new Intent(this, SynchronizedData_BackgroundService.class);
        eventIntent.setAction("ACTION_RECORD_EVENT");
        eventIntent.putExtra("EVENT_TIMESTAMP", relativeTime);
        startService(eventIntent);

        // Optional: update local plotter data
        if (dataExport != null) {
            dataExport.addEvent(relativeTime);
            ArrayList<String[]> rows = dataExport.getSensorData("Synchronized");
            if (rows != null && !rows.isEmpty()) {
                String[] lastRow = rows.get(rows.size() - 1);
                // 🟩 10th column = index 9
                lastRow[9] = String.valueOf(relativeTime);
            }
        }
    }

    //Stop the background service from recording
    @Override
    public void onStopRecording() {
        // Export first (while the Service is still alive and has dataCollector)
        String recordingName = ((EditText) findViewById(R.id.editRecordingName)).getText().toString();
        Intent exportIntent = new Intent(this, SynchronizedData_BackgroundService.class);
        exportIntent.setAction("ACTION_EXPORT_DATA");
        exportIntent.putExtra("RECORDING_NAME", recordingName);
        startService(exportIntent);

        // Give the Service time to finish export (optional: you could delay stopService with a Handler)
        // Then stop the Service
        Intent serviceIntent = new Intent(this, SynchronizedData_BackgroundService.class);
        stopService(serviceIntent);

        // Stop local plotter (live plotting only)
        if (synchronizedDataCollector != null) {
            synchronizedDataCollector.stop();
        }

        super.onStop();
        unregisterReceiver(exportReceiver);
    }
    @Override
    public void onExportRecording(String recordingName) {
        // Send an intent to the Service to trigger the export
        Intent exportIntent = new Intent(this, SynchronizedData_BackgroundService.class);
        exportIntent.setAction("ACTION_EXPORT_DATA");
        exportIntent.putExtra("RECORDING_NAME", recordingName);
        startService(exportIntent);
    }
    private void shareZipFile(String zipFilePath) {
        File zipFile = new File(zipFilePath);
        if (!zipFile.exists()) {
            Toast.makeText(this, "Export file does not exist.", Toast.LENGTH_SHORT).show();
            return;
        }

        Uri fileUri = FileProvider.getUriForFile(this, getPackageName() + ".provider", zipFile);
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("application/zip");
        shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(shareIntent, "Share ZIP file via:"));
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
}


