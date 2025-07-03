package com.humbl.imuapp;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

public class GeoJsonHelper {
    private static final String TAG = "GeoJsonHelper";

    public static class EventPoint {
        public double latitude;
        public double longitude;
        public long timestamp;

        public EventPoint(double lat, double lon, long time) {
            this.latitude = lat;
            this.longitude = lon;
            this.timestamp = time;
        }
    }

    public static void uploadLatestGeoJson(Context context, List<EventPoint> recentPins, String sasUrl) {
        try {
            // Build GeoJSON FeatureCollection
            JSONArray featuresArray = new JSONArray();

            for (EventPoint pin : recentPins) {
                JSONObject feature = new JSONObject();
                feature.put("type", "Feature");

                // Geometry
                JSONObject geometry = new JSONObject();
                geometry.put("type", "Point");
                JSONArray coords = new JSONArray();
                coords.put(pin.longitude);
                coords.put(pin.latitude);
                geometry.put("coordinates", coords);

                // Properties
                JSONObject properties = new JSONObject();
                properties.put("event_time", formatTimestamp(pin.timestamp));

                feature.put("geometry", geometry);
                feature.put("properties", properties);

                featuresArray.put(feature);
            }

            JSONObject featureCollection = new JSONObject();
            featureCollection.put("type", "FeatureCollection");
            featureCollection.put("features", featuresArray);


            // Write to cache file
            File geojsonFile = new File(context.getCacheDir(), "latest.geojson");
            try (FileWriter writer = new FileWriter(geojsonFile)) {
                writer.write(featureCollection.toString());
            }

            // Upload to Azure
            uploadToAzure(geojsonFile, sasUrl);
        } catch (Exception e) {
            Log.e(TAG, "Error building or uploading GeoJSON", e);
        }
    }

    private static void uploadToAzure(File file, String sasUrl) {
        new Thread(() -> {
            try {
                Log.d(TAG, "Uploading file of size: " + file.length());

                URL url = new URL(sasUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                conn.setDoOutput(true);
                conn.setRequestMethod("PUT");
                conn.setRequestProperty("x-ms-blob-type", "BlockBlob");
                conn.setRequestProperty("Content-Length", String.valueOf(file.length()));

                OutputStream out = conn.getOutputStream();
                FileInputStream fis = new FileInputStream(file);
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
                fis.close();
                out.flush();
                out.close();

                int responseCode = conn.getResponseCode();
                Log.d(TAG, "Upload response: " + responseCode);

                if (responseCode >= 200 && responseCode < 300) {
                    Log.d(TAG, "GeoJSON upload successful");
                } else {
                    Log.w(TAG, "GeoJSON upload failed with response code: " + responseCode);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error during upload to Azure", e);
            }
        }).start();
    }

    private static String formatTimestamp(long millis) {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
        return sdf.format(new java.util.Date(millis));
    }
}
