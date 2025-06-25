package com.humbl.imuapp;

//import static androidx.core.content.ContextCompat.getSystemService;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
//import android.util.Log; //Needed for Log debugging statements
import android.util.Log;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;


public class MainActivity extends AppCompatActivity implements RecordFragment.OnRecordControlListener {

    private DataExport dataLivePlot;
    private long recordingStartTime;
    private boolean isAccelEnabled, isGyroEnabled, isGPSEnabled;
    private static final int REQUEST_LOCATION_PERMISSION = 1; //Request code for GPS permissions
    private PlotFragment plotFragment; //fragment to plot live data
    private SynchronizedDataCollector synchronizedDataCollector;
    private static final int REQUEST_STORAGE_PERMISSION = 1001;
    private String pendingRecordingNameForExport = null;


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

        //Getting unique ID for the device for uploads to the cloud
        SharedPreferences prefs = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        String deviceId = prefs.getString("DEVICE_ID", null);

        if (deviceId == null) {
            deviceId = java.util.UUID.randomUUID().toString(); // Secure, anonymous
            prefs.edit().putString("DEVICE_ID", deviceId).apply();
        }

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
                ((EditText) findViewById(R.id.editRecordingName)).getText().toString().trim();

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

        // Start a local data collector for live plots (separate instance for live plotting)
        recordingStartTime = System.currentTimeMillis();
        dataLivePlot = new DataExport(); // Only for plotting (not exported)
        synchronizedDataCollector = new SynchronizedDataCollector(
                this, dataLivePlot, isAccelEnabled, isGyroEnabled, isGPSEnabled, recordingStartTime, plotFragment
        );
        synchronizedDataCollector.start();
        plotFragment.resetCharts(); //reset the live plotting charts so plot is current recording


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

        // Update local plotter data with event time
        if (dataLivePlot != null) {
            dataLivePlot.addEvent(relativeTime);

            ArrayList<String[]> rows = dataLivePlot.getSensorData("Synchronized");
            if (rows != null && !rows.isEmpty()) {
                String[] lastRow = rows.get(rows.size() - 1);
                // 10th column = index 9
                lastRow[11] = String.valueOf(relativeTime);

                // Prompt user for description
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Describe the Event");

                final EditText input = new EditText(this);
                input.setHint("Enter a short description...");
                builder.setView(input);

                builder.setPositiveButton("OK", (dialog, which) -> {
                    String description = input.getText().toString();
                    Intent descIntent = new Intent(MainActivity.this, SynchronizedData_BackgroundService.class);
                    descIntent.setAction("ACTION_ADD_EVENT_DESCRIPTION");
                    descIntent.putExtra("EVENT_DESCRIPTION", description);
                    descIntent.putExtra("EVENT_TIMESTAMP", relativeTime); // still relative
                    startService(descIntent);


                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.hideSoftInputFromWindow(input.getWindowToken(), 0);
                    }

                    Log.d("Export", "Row with description saved: " + Arrays.toString(lastRow));
                });

                builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
                builder.show();

            }
        }

        // Send the event time to the plot for visual marking
        if (plotFragment != null) {
            plotFragment.addEventMarker(relativeTime/1000f); // convert to seconds if X-axis uses seconds
        }
    }

    //Stop the background service from recording
    @Override
    public void onStopRecording() {
        // Stop only local collector (plotting)
        if (synchronizedDataCollector != null) {
            synchronizedDataCollector.stop();
        }

        // Stop background data collector (sensor capture)
        Intent pauseIntent = new Intent(this, SynchronizedData_BackgroundService.class);
        pauseIntent.setAction("ACTION_STOP_RECORDING");
        startService(pauseIntent); // Add handling in service
    }


    @Override
    public void onDeleteRecorded() {
        // 1. Delete background service file
        if (SynchronizedData_BackgroundService.lastRecordingZipPath != null) {
            File zipFile = new File(SynchronizedData_BackgroundService.lastRecordingZipPath);
            if (zipFile.exists()) {
                boolean deleted = zipFile.delete();
                Log.d("DeleteRecording", "Background file deleted: " + deleted);
            }
        }

        // 2. Delete live plotting file
        File externalDir = getExternalFilesDir(null);
        SharedPreferences prefs = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        String recordingName = prefs.getString("CURRENT_RECORDING_NAME", null);
        if (recordingName != null) {
            File liveFile = new File(externalDir, recordingName + ".csv"); // or .zip depending on your format
            if (liveFile.exists()) {
                boolean deletedLive = liveFile.delete();
                Log.d("DeleteRecording", "Live file deleted: " + deletedLive);
            }
        }

        // 3. Reset plot/chart data if needed
        if (plotFragment != null) {
            plotFragment.resetCharts();
        }

        Toast.makeText(this, "Recording deleted", Toast.LENGTH_SHORT).show();
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
    //Request permission to access GPS and storage permission
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                onStartRecording(isAccelEnabled, isGyroEnabled, isGPSEnabled);
            } else {
                Toast.makeText(this, "Location permission denied.", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQUEST_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (pendingRecordingNameForExport != null) {
                    exportAndStop(pendingRecordingNameForExport);
                    pendingRecordingNameForExport = null; // clear it
                }
            } else {
                Toast.makeText(this, "Storage permission denied. Cannot export recording.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void exportAndStop(String recordingName) {
        // Send export intent
        Intent exportIntent = new Intent(this, SynchronizedData_BackgroundService.class);
        exportIntent.setAction("ACTION_EXPORT_DATA");
        exportIntent.putExtra("RECORDING_NAME", recordingName);
        startService(exportIntent);

        // Stop background service
        Intent serviceIntent = new Intent(this, SynchronizedData_BackgroundService.class);
        stopService(serviceIntent);

        // Stop local plotting
        if (synchronizedDataCollector != null) {
            synchronizedDataCollector.stop();
        }

        // Unregister export receiver
        unregisterReceiver(exportReceiver);
    }


}


