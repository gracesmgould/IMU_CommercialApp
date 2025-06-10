package com.humbl.imuapp;


import android.util.Log;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;


public class DataExport {
    private final Map<String, ArrayList<String[]>> sensorDataMap; // Dynamic data for each sensor type
    private final ArrayList<Long> eventTime;

    public DataExport() {
        sensorDataMap = new HashMap<>();
        eventTime = new ArrayList<>();
    }

    // Add a new row of data for a specific sensor type
    public void addSensorRow(String sensorType, String[] row) {
        if (!sensorDataMap.containsKey(sensorType)) {
            sensorDataMap.put(sensorType, new ArrayList<>());
        }
        sensorDataMap.get(sensorType).add(row);
    }

    // Add an event timestamp when event button clicked
    public void addEvent(long timeMs) {
        eventTime.add(timeMs);
    }

    // Build the CSV content string for the dataset
    public String createCSV() {
        StringBuilder sb = new StringBuilder();

        // Unified header
        sb.append("timeStampAcc,accX,accY,accZ,timeStampGyro,gyroX,gyroY,gyroZ,timeStampGPS,latitude,longitude,event_time, event_description\n");

        ArrayList<String[]> rows = sensorDataMap.get("Synchronized");
        if (rows != null) {
            for (String[] row : rows) {
                if (row == null) continue;
                sb.append(String.join(",", row)).append("\n");
            }
        }
        return sb.toString();
    }

    // Export CSV as zip file to specified location
    public boolean exportAsZip(File zipFile) {
        try {
            Log.d("DataExport", "Starting exportAsZip to: " + zipFile.getAbsolutePath());

            // Create CSV file in the same directory as zip
            File csvFile = new File(zipFile.getParent(), zipFile.getName().replace(".zip", ".csv"));
            Log.d("DataExport", "CSV file path: " + csvFile.getAbsolutePath());

            // Create the CSV content
            String csvContent = createCSV();
            Log.d("DataExport", "CSV content length: " + (csvContent != null ? csvContent.length() : 0));

            // Write CSV content to the file
            FileOutputStream fos = new FileOutputStream(csvFile);
            fos.write(csvContent.getBytes());
            fos.close();

            // Confirm CSV file exists
            Log.d("DataExport", "CSV file created: " + csvFile.exists() + " at " + csvFile.getAbsolutePath());

            // Create a ZIP output stream
            ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile));
            FileInputStream fis = new FileInputStream(csvFile);
            ZipEntry entry = new ZipEntry(csvFile.getName());
            zos.putNextEntry(entry);

            byte[] buffer = new byte[1024];
            int len;
            while ((len = fis.read(buffer)) > 0) {
                zos.write(buffer, 0, len);
            }

            zos.closeEntry();
            zos.close();
            fis.close();

            // Confirm ZIP file was created
            Log.d("DataExport", "ZIP file created: " + zipFile.exists() + " at " + zipFile.getAbsolutePath());

            // Delete the temporary CSV file
            if (csvFile.exists()) {
                boolean deleted = csvFile.delete();
                Log.d("DataExport", "CSV file deleted after ZIP? " + deleted);
            }

            return true; // success
        } catch (IOException e) {
            Log.e("DataExport", "Failed to export zip file", e);
            return false; // error
        }
    }
    //Method to get sensor data from specific sensor type
    public ArrayList<String[]> getSensorData(String sensorType) {
        return sensorDataMap.get(sensorType);
    }

}
