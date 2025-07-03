package com.humbl.imuapp;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.media.MediaScannerConnection;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SynchronizedData_BackgroundService extends Service {
    private SynchronizedDataCollector dataCollector;
    private final Map<Long, Integer> eventTimestampToRowIndex = new HashMap<>();
    public static String lastRecordingZipPath;
    public static final String ACTION_RECORD_EVENT = "ACTION_RECORD_EVENT";
    public static final String ACTION_ADD_EVENT_DESCRIPTION = "ACTION_ADD_EVENT_DESCRIPTION";
    public static final String ACTION_EXPORT_DATA = "ACTION_EXPORT_DATA";
    public static final String ACTION_STOP_RECORDING = "ACTION_STOP_RECORDING";

    private static final int LATITUDE_INDEX = 10;
    private static final int LONGITUDE_INDEX = 11;
    private static final int EVENT_DESC_INDEX = 12;

    private final List<GeoJsonHelper.EventPoint> recentPins = new ArrayList<GeoJsonHelper.EventPoint>();

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("SynchronizedDataService", "Service created");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            Log.d("SynchronizedDataService", "Intent action: " + intent.getAction());
        }

        if (intent != null && intent.getAction() == null) {
            Notification notification = createNotification();
            startForeground(1, notification);

            boolean isAccelEnabled = intent.getBooleanExtra("ACCEL_ENABLED", true);
            boolean isGyroEnabled = intent.getBooleanExtra("GYRO_ENABLED", true);
            boolean isGPSEnabled = intent.getBooleanExtra("GPS_ENABLED", true); //Not sure if this is supposed to be true or false here
            long recordingStartTime = System.currentTimeMillis();

            DataExport dataExport = new DataExport();

            dataCollector = new SynchronizedDataCollector(
                    this, dataExport, isAccelEnabled, isGyroEnabled, isGPSEnabled, recordingStartTime, null
            );
            dataCollector.start();
        }

        if (intent != null && intent.getAction() != null) {
            String action = intent.getAction();

            if (ACTION_RECORD_EVENT.equals(action)) {
                long eventTimestamp = intent.getLongExtra("EVENT_TIMESTAMP", -1);
                long clockTime = System.currentTimeMillis();

                if (eventTimestamp != -1 && dataCollector != null) {
                    DataExport dataExport = dataCollector.getDataExport();
                    dataExport.addEvent(eventTimestamp);

                    ArrayList<String[]> rows = dataExport.getSensorData("Synchronized");
                    if (rows != null && !rows.isEmpty()) {
                        int targetIndex = rows.size() - 1;
                        eventTimestampToRowIndex.put(eventTimestamp, targetIndex);

                        String[] lastRow = rows.get(targetIndex);
                        if (lastRow.length < 13) {
                            lastRow = java.util.Arrays.copyOf(lastRow, 13);
                            rows.set(targetIndex, lastRow);
                        }
                        lastRow[11] = String.valueOf(eventTimestamp);

                        double latitude = dataCollector.getLatitude();
                        double longitude = dataCollector.getLongitude();

                        if (!dataCollector.hasValidGPSFix() || (latitude == 0.0 && longitude == 0.0)) {
                            Log.w("GeoJSON", "Skipping GeoJSON pin: no valid GPS data");
                            return START_NOT_STICKY;
                        }

                        recentPins.add(new GeoJsonHelper.EventPoint(latitude, longitude, clockTime));
                        long cutoff = System.currentTimeMillis() - 30 * 60 * 1000;
                        recentPins.removeIf(p -> p.timestamp < cutoff);

                        String uniqueId = getAnonymousUserId(); // method below
                        String timestamp = String.valueOf(System.currentTimeMillis());
                        String filename = "event_" + uniqueId + "_" + timestamp + ".geojson";

                        SasTokenService.requestSasUrl("gpsdata", filename, new SasTokenService.SasTokenCallback() {
                            @Override
                            public void onSuccess(String sasUrl) {
                                GeoJsonHelper.uploadLatestGeoJson(getApplicationContext(), recentPins, sasUrl);
                                recentPins.clear();
                            }

                            @Override
                            public void onFailure(Exception e) {
                                Log.e("GeoJsonUpload", "Failed to get SAS URL", e);
                                recentPins.clear();
                            }
                        });
                    }
                }
                return START_NOT_STICKY;
            }
            if (ACTION_ADD_EVENT_DESCRIPTION.equals(action)) {
                String desc = intent.getStringExtra("EVENT_DESCRIPTION");
                long timestamp = intent.getLongExtra("EVENT_TIMESTAMP", -1);

                if (desc != null && timestamp != -1 && dataCollector != null) {
                    ArrayList<String[]> rows = dataCollector.getDataExport().getSensorData("Synchronized");
                    Integer targetIndex = eventTimestampToRowIndex.get(timestamp);

                    if (rows != null && targetIndex != null && targetIndex >= 0 && targetIndex < rows.size()) {
                        String[] targetRow = rows.get(targetIndex);
                        if (targetRow.length < 13) {
                            targetRow = java.util.Arrays.copyOf(targetRow, 13);
                            rows.set(targetIndex, targetRow);
                        }
                        targetRow[EVENT_DESC_INDEX] = desc.replace(",", " ");
                    }
                }
                return START_NOT_STICKY;
            }

            if (ACTION_STOP_RECORDING.equals(action)) {
                if (dataCollector != null) {
                    dataCollector.stop(); // Stop sensor updates, retain data
                    Log.d("SynchronizedDataService", "Recording stopped, data retained.");
                }
                return START_NOT_STICKY;
            }

            if (ACTION_EXPORT_DATA.equals(action)) {
                String recordingName = intent.getStringExtra("RECORDING_NAME");
                Log.d("SynchronizedDataService", "ACTION_EXPORT_DATA matched. Recording name: " + recordingName);
                exportRecording(recordingName);
                return START_NOT_STICKY;
            }
        }

        return START_STICKY;
    }

    private Notification createNotification() {
        String channelId = "recording_channel";
        NotificationManager manager = getSystemService(NotificationManager.class);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId, "Recording", NotificationManager.IMPORTANCE_LOW);
            manager.createNotificationChannel(channel);
        }

        return new NotificationCompat.Builder(this, channelId)
                .setContentTitle("Recording Active")
                .setContentText("Sensor data is being recorded in the background.")
                .setSmallIcon(R.drawable.ic_launcher_background)
                .build();
    }

    private void exportRecording(String recordingName) {
        if (dataCollector != null) {
            DataExport dataExport = dataCollector.getDataExport();
            File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            if (!downloadsDir.exists()) downloadsDir.mkdirs();

            File zipFile = getUniqueFile(downloadsDir, recordingName, ".zip");

            if (dataExport.exportAsZip(zipFile)) {
                lastRecordingZipPath = zipFile.getAbsolutePath();

                MediaScannerConnection.scanFile(
                        this,
                        new String[]{zipFile.getAbsolutePath()},
                        null,
                        (path, uri) -> Log.d("SynchronizedDataService", "Scanned to MediaStore: " + uri)
                );

                String filename = zipFile.getName();

                SasTokenService.requestSasUrl("appdata", filename, new SasTokenService.SasTokenCallback() {
                    @Override
                    public void onSuccess(String sasUrl) {
                        boolean success = AzureStorage.uploadCsvToBlob(zipFile.getAbsolutePath(), sasUrl);
                        Log.d("AzureUpload", "Upload success: " + success);

                        Intent doneIntent = new Intent("EXPORT_COMPLETED");
                        doneIntent.putExtra("ZIP_PATH", lastRecordingZipPath);
                        doneIntent.putExtra("UPLOAD_SUCCESS", success);
                        sendBroadcast(doneIntent);
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Log.e("AzureUpload", "Failed to get SAS URL", e);

                        Intent failedIntent = new Intent("EXPORT_COMPLETED");
                        failedIntent.putExtra("ZIP_PATH", lastRecordingZipPath);
                        failedIntent.putExtra("UPLOAD_SUCCESS", false);
                        sendBroadcast(failedIntent);

                        new Handler(Looper.getMainLooper()).post(() ->
                                Toast.makeText(getApplicationContext(),
                                        "Upload failed. File saved to Downloads.", Toast.LENGTH_LONG).show()
                        );
                    }
                });
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (dataCollector != null) {
            Log.w("SynchronizedDataService", "dataCollector is null — cannot export.");
            dataCollector.stop();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private File getUniqueFile(File dir, String baseName, String extension) {
        File file = new File(dir, baseName + extension);
        if (!file.exists()) return file;
        int counter = 1;
        File numberedFile;
        do {
            numberedFile = new File(dir, baseName + "(" + counter + ")" + extension);
            counter++;
        } while (numberedFile.exists());
        return numberedFile;
    }
    private String getAnonymousUserId() {
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String userId = prefs.getString("ANON_USER_ID", null);
        if (userId == null) {
            userId = UUID.randomUUID().toString(); // Generate a one-time anonymous ID
            prefs.edit().putString("ANON_USER_ID", userId).apply();
        }
        return userId;
    }


}
