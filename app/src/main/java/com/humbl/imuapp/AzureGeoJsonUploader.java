package com.humbl.imuapp;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class AzureGeoJsonUploader {
    private static final String TAG = "GeoJsonUploader";
    private static long lastUploadTime = -1;  // Track last upload time

    public static void uploadEvent(Context context, long clockTime, double latitude, double longitude, String sasUrl) {
        long now = System.currentTimeMillis();
        boolean appendMode = (lastUploadTime != -1) && ((now - lastUploadTime) < 30 * 60 * 1000); // append if within 30 mins
        lastUploadTime = now;

        File geojsonFile = new File(context.getCacheDir(), "event_data.geojson");
        JSONArray featuresArray = new JSONArray();

        try {
            if (appendMode && geojsonFile.exists()) {
                // Read existing file
                StringBuilder sb = new StringBuilder();
                BufferedReader reader = new BufferedReader(new FileReader(geojsonFile));
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                reader.close();

                JSONObject root = new JSONObject(sb.toString());
                featuresArray = root.optJSONArray("features");
                if (featuresArray == null) {
                    featuresArray = new JSONArray();
                }
            }

            // Construct new feature
            JSONObject feature = new JSONObject();
            feature.put("type", "Feature");

            JSONObject geometry = new JSONObject();
            geometry.put("type", "Point");
            JSONArray coords = new JSONArray();
            coords.put(longitude);
            coords.put(latitude);
            geometry.put("coordinates", coords);

            JSONObject properties = new JSONObject();
            properties.put("time_Stamp", formatTimestamp(clockTime));

            feature.put("geometry", geometry);
            feature.put("properties", properties);

            featuresArray.put(feature);

            // Wrap into FeatureCollection
            JSONObject featureCollection = new JSONObject();
            featureCollection.put("type", "FeatureCollection");
            featureCollection.put("features", featuresArray);

            // Write to file
            FileWriter writer = new FileWriter(geojsonFile, false);
            writer.write(featureCollection.toString());
            writer.close();

            // Upload to Azure
            uploadToAzure(geojsonFile, sasUrl);

        } catch (Exception e) {
            Log.e(TAG, "Failed to upload GeoJSON event", e);
        }
    }

    public static void uploadToAzure(File file, String sasUrl) {
        new Thread(() -> {
            try {
                Log.d(TAG, "Starting upload to: " + sasUrl);
                Log.d(TAG, "Uploading file of size: " + file.length());

                URL url = new URL(sasUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                connection.setDoOutput(true);
                connection.setRequestMethod("PUT");
                connection.setRequestProperty("x-ms-blob-type", "BlockBlob");
                connection.setRequestProperty("Content-Type", "application/json"); // optional but good practice
                connection.setRequestProperty("Content-Length", String.valueOf(file.length()));

                OutputStream out = connection.getOutputStream();
                FileInputStream fis = new FileInputStream(file);
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
                fis.close();
                out.flush();
                out.close();

                int responseCode = connection.getResponseCode();
                Log.d(TAG, "Upload response: " + responseCode);

                if (responseCode >= 200 && responseCode < 300) {
                    Log.d(TAG, "GeoJSON upload successful");
                } else {
                    Log.w(TAG, "GeoJSON upload failed: " + responseCode);

                    // Try to read error response
                    try (BufferedReader errorReader = new BufferedReader(
                            new InputStreamReader(connection.getErrorStream()))) {
                        StringBuilder errorResponse = new StringBuilder();
                        String line;
                        while ((line = errorReader.readLine()) != null) {
                            errorResponse.append(line);
                        }
                        Log.w(TAG, "Azure error response: " + errorResponse.toString());
                    } catch (Exception readErr) {
                        Log.e(TAG, "Failed to read Azure error response", readErr);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error uploading GeoJSON to Azure", e);
            }
        }).start();
    }

    private static String formatTimestamp(long millis) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(new Date(millis));
    }
}
