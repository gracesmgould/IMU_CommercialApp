package com.humbl.imuapp;

import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class ArcGISUpload {

    private static final String UPLOAD_URL = "https://services2.arcgis.com/NlsizNmbMFiinWw4/arcgis/rest/services/FallEvents/FeatureServer/0/addFeatures";

    /**
     * Uploads a fall or near-fall event to ArcGIS with location and timestamp.
     *
     * @param eventType      "Fall" or "Near-fall"
     * @param eventTimestamp timestamp in milliseconds
     * @param latitude       decimal degrees
     * @param longitude      decimal degrees
     */
    public static void uploadEvent(String eventType, long eventTimestamp, double latitude, double longitude) {

        Log.d("ArcGISUploader", "uploadEvent() called with type=" + eventType);

        // Validate GPS values
        if ((latitude == 0.0 && longitude == 0.0) || Math.abs(latitude) > 90 || Math.abs(longitude) > 180) {
            Log.w("ArcGISUploader", "Invalid GPS coordinates, skipping upload.");
            return;
        }

        String timestampIso = formatTimestamp(eventTimestamp);

        // Construct JSON payload
        String json = "{ \"features\": [ { " +
                "\"attributes\": { " +
                "\"eventType\": \"" + eventType + "\", " +
                "\"time_Stamp\": \"" + timestampIso + "\" }, " +
                "\"geometry\": { " +
                "\"x\": " + longitude + ", " +
                "\"y\": " + latitude + ", " +
                "\"spatialReference\": { \"wkid\": 4326 } } } ], " +
                "\"f\": \"json\" }";

        new Thread(() -> {
            try {
                URL url = new URL(UPLOAD_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                conn.setDoOutput(true);

                String payload;
                try {
                    payload = "features=" + URLEncoder.encode(json, "UTF-8") + "&f=json";
                } catch (UnsupportedEncodingException e) {
                    Log.e("ArcGISUploader", "Encoding error", e);
                    return;
                }

                OutputStream os = conn.getOutputStream();
                os.write(payload.getBytes());
                os.flush();
                os.close();

                int responseCode = conn.getResponseCode();
                InputStream is = conn.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                StringBuilder response = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                Log.d("ArcGISUploader", "Upload response (" + responseCode + "): " + response.toString());

            } catch (Exception e) {
                Log.e("ArcGISUploader", "Error uploading to ArcGIS", e);
            }
        }).start();
    }

    /**
     * Converts a millisecond timestamp to ISO 8601 format.
     */
    private static String formatTimestamp(long millis) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(new Date(millis));
    }
}
