package com.humbl.imuapp;

import android.util.Log;
import java.io.*;
import java.net.*;

public class AzureStorage {

    public static boolean uploadCsvToBlob(String filePath, String sasUrl) {
        try {
            File file = new File(filePath);
            FileInputStream fileInputStream = new FileInputStream(file);

            URL url = new URL(sasUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            connection.setDoOutput(true);
            connection.setRequestMethod("PUT");
            connection.setRequestProperty("x-ms-blob-type", "BlockBlob");
            connection.setRequestProperty("Content-Length", String.valueOf(file.length()));

            OutputStream out = connection.getOutputStream();
            byte[] buffer = new byte[4096];
            int bytesRead;

            while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }

            fileInputStream.close();
            out.flush();
            out.close();

            int responseCode = connection.getResponseCode();
            connection.disconnect();
            if (responseCode == 201) {
                Log.d("AzureStorage", "✅ Upload successful");
                return true;
            } else {
                Log.e("AzureStorage", "❌ Upload failed. Code: " + responseCode);
                return false;
            }

        } catch (Exception e) {
            Log.e("AzureStorage", "❌ Upload failed", e);
            return false;
        }
    }
}

