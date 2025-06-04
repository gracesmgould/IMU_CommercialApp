package com.example.uibasics;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import java.io.File;
import java.util.ArrayList;
import android.os.Environment;
import android.media.MediaScannerConnection;


public class SynchronizedData_BackgroundService extends Service {
    private SynchronizedDataCollector dataCollector;
    public static String lastRecordingZipPath; // Static variable for easy access

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("SynchronizedDataService", "Service created");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("SynchronizedDataService", "onStartCommand called");

        if (intent != null) {
            String action = intent.getAction();

            if ("ACTION_EXPORT_DATA".equals(action)) {
                String recordingName = intent.getStringExtra("RECORDING_NAME");
                Log.d("SynchronizedDataService", "Received export request for: " + recordingName);
                exportRecording(recordingName);
                return START_NOT_STICKY;

            } else if ("ACTION_RECORD_EVENT".equals(action)) {
                long eventTimestamp = intent.getLongExtra("EVENT_TIMESTAMP", -1);
                if (eventTimestamp != -1 && dataCollector != null) {
                    Log.d("SynchronizedDataService", "Recording event at: " + eventTimestamp);

                    DataExport dataExport = dataCollector.getDataExport();
                    dataExport.addEvent(eventTimestamp);

                    ArrayList<String[]> rows = dataExport.getSensorData("Synchronized");
                    if (rows != null && !rows.isEmpty()) {
                        String[] lastRow = rows.get(rows.size() - 1);
                        // Save event timestamp in the 12th column (index 11)
                        lastRow[11] = String.valueOf(eventTimestamp);
                        Log.d("SynchronizedDataService", "Event time set in last row: " + String.join(",", lastRow));
                    }
                } else {
                    Log.d("SynchronizedDataService", "Event ignored: dataCollector is null or invalid timestamp.");
                }
                return START_NOT_STICKY;
            }
        }

        // Recording start logic
        Notification notification = createNotification();
        startForeground(1, notification);

        Log.d("SynchronizedDataService", "Recording service starting...");

        boolean isAccelEnabled = intent.getBooleanExtra("ACCEL_ENABLED", true);
        boolean isGyroEnabled = intent.getBooleanExtra("GYRO_ENABLED", true);
        boolean isGPSEnabled = intent.getBooleanExtra("GPS_ENABLED", false);
        Log.d("SynchronizedDataService", "Accel: " + isAccelEnabled + " Gyro: " + isGyroEnabled + " GPS: " + isGPSEnabled);

        long recordingStartTime = System.currentTimeMillis();
        DataExport dataExport = new DataExport();

        dataCollector = new SynchronizedDataCollector(
                this, dataExport, isAccelEnabled, isGyroEnabled, isGPSEnabled, recordingStartTime, null
        );
        dataCollector.start();

        Log.d("SynchronizedDataService", "DataCollector initialized: " + (dataCollector != null));

        return START_STICKY;
    }



    private Notification createNotification() {
        String channelId = "recording_channel";
        String channelName = "Recording";
        NotificationManager manager = getSystemService(NotificationManager.class);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    channelName,
                    NotificationManager.IMPORTANCE_LOW
            );
            manager.createNotificationChannel(channel);
        }

        return new NotificationCompat.Builder(this, channelId)
                .setContentTitle("Recording Active")
                .setContentText("Sensor data is being recorded in the background.")
                .setSmallIcon(R.drawable.ic_launcher_background) //HuMBL background icon
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }
    /*private void exportRecording(String recordingName) {
        if (dataCollector != null) {
            Log.d("SynchronizedDataService", "DataCollector is not null, proceeding to export.");
            DataExport dataExport = dataCollector.getDataExport();
            File externalDir = getExternalFilesDir(null);
            File zipFile = getUniqueFile(externalDir, recordingName, ".zip");

            Log.d("SynchronizedDataService", "Starting export to: " + zipFile.getAbsolutePath());
            if (dataExport.exportAsZip(zipFile)) {
                lastRecordingZipPath = zipFile.getAbsolutePath();

                // Notify MainActivity that the export is done!
                Intent doneIntent = new Intent("EXPORT_COMPLETED");
                doneIntent.putExtra("ZIP_PATH", lastRecordingZipPath);
                sendBroadcast(doneIntent);

                Log.d("SynchronizedDataService", "Export complete: " + lastRecordingZipPath);
            } else {
                Log.e("SynchronizedDataService", "Export failed");
            }
        } else {Log.d("SynchronizedDataService", "DataCollector is NULL! Cannot export.");}
    }*/
    private void exportRecording(String recordingName) {
        if (dataCollector != null) {
            Log.d("SynchronizedDataService", "DataCollector is not null, proceeding to export.");
            DataExport dataExport = dataCollector.getDataExport();

            // Public Downloads directory
            File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            if (!downloadsDir.exists()) downloadsDir.mkdirs();

            // Create unique filename in Downloads
            File zipFile = getUniqueFile(downloadsDir, recordingName, ".zip");

            Log.d("SynchronizedDataService", "Starting export to: " + zipFile.getAbsolutePath());

            if (dataExport.exportAsZip(zipFile)) {
                lastRecordingZipPath = zipFile.getAbsolutePath();

                // Scan file so it's visible to system and file browsers
                MediaScannerConnection.scanFile(
                        this,
                        new String[]{zipFile.getAbsolutePath()},
                        null,
                        (path, uri) -> Log.d("SynchronizedDataService", "Scanned to MediaStore: " + uri)
                );

                // Notify MainActivity export is done
                Intent doneIntent = new Intent("EXPORT_COMPLETED");
                doneIntent.putExtra("ZIP_PATH", lastRecordingZipPath);
                sendBroadcast(doneIntent);

                Log.d("SynchronizedDataService", "Export complete: " + lastRecordingZipPath);
            } else {
                Log.e("SynchronizedDataService", "Export failed");
            }
        } else {
            Log.d("SynchronizedDataService", "DataCollector is NULL! Cannot export.");
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (dataCollector != null) {
            dataCollector.stop();
            Log.d("SynchronizedDataService", "Recording stopped, no file saved yet.");
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        // Not a bound service
        return null;
    }
    //getUniqueFile to prevent overwriting of the same file name
    private File getUniqueFile(File dir, String baseName, String extension) {
        File file = new File(dir, baseName + extension);
        Log.d("MainActivity", "Checking for file: " + file.getAbsolutePath());

        // Check if the original file exists
        if (!file.exists()) {
            Log.d("MainActivity", "Original file does not exist. Using: " + file.getName());
            return file;
        }

        // If it does exist, start numbering
        int counter = 1;
        File numberedFile;
        do {
            numberedFile = new File(dir, baseName + "(" + counter + ")" + extension);
            Log.d("MainActivity", "Trying numbered file: " + numberedFile.getName());
            counter++;
        } while (numberedFile.exists());

        Log.d("MainActivity", "Final unique filename: " + numberedFile.getName());
        return numberedFile;
    }
}
