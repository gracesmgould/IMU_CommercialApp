package com.humbl.imuapp;

import android.util.Log;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.*;

public class SasTokenService {

    public interface SasTokenCallback {
        void onSuccess(String sasUrl);
        void onFailure(Exception e);
    }

    public static void requestSasUrl(String filename, SasTokenCallback callback) {
        new Thread(() -> {
            int maxRetries = 3;
            int attempts = 0;
            boolean success = false;

            while (attempts < maxRetries && !success) {
                try {
                    attempts++;

                    String encodedFilename = URLEncoder.encode(filename, "UTF-8");
                    String urlStr = "https://imu-sas-api.azurewebsites.net/api/GetSasToken?filename=" + encodedFilename;
                    URL url = new URL(urlStr);

                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(5000); // 5 seconds
                    conn.setReadTimeout(5000);    // 5 seconds

                    int responseCode = conn.getResponseCode();
                    Log.d("SasTokenService", "HTTP response code: " + responseCode);

                    if (responseCode != 200) {
                        throw new RuntimeException("Failed to get SAS token. HTTP " + responseCode);
                    }

                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;

                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    JSONObject json = new JSONObject(response.toString());
                    String sasUrl = json.getString("sasUrl");

                    success = true;
                    callback.onSuccess(sasUrl);

                } catch (Exception e) {
                    Log.e("SasTokenService", "Attempt " + attempts + " failed to fetch SAS URL", e);
                    if (attempts >= maxRetries) {
                        callback.onFailure(e);
                    }

                    try {
                        Thread.sleep(1000); // wait 1 sec before retrying
                    } catch (InterruptedException ignored) {
                    }
                }
            }
        }).start();
    }
}
