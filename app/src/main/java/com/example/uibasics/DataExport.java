package com.example.uibasics;

import android.content.Context;
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

    // Add a new row of data for a specific sensor
    public void addSensorRow(String sensorType, String[] row) {
        if (!sensorDataMap.containsKey(sensorType)) {
            sensorDataMap.put(sensorType, new ArrayList<>());
        }
        sensorDataMap.get(sensorType).add(row);
    }

    // Add an event timestamp
    public void addEvent(long timeMs) {
        eventTime.add(timeMs);
    }

    /*
    // Build CSV dynamically
    public String createCSV() {
        StringBuilder sb = new StringBuilder();

        // Sensor Data Sections
        for (String sensorType : sensorDataMap.keySet()) {
            sb.append("# ").append(sensorType).append(" Data\n");

            // Add header based on sensor type
            switch (sensorType) {
                case "Accelerometer":
                case "Gyroscope":
                    sb.append("timestamp,X,Y,Z\n");
                    break;
                case "GPS":
                    sb.append("timestamp,latitude,longitude,altitude\n");
                    break;
                default:
                    sb.append("timestamp,value1,value2,value3\n"); // generic header
            }

            for (String[] row : sensorDataMap.get(sensorType)) {
                sb.append(String.join(",", row)).append("\n");
            }
            sb.append("\n");
        }

        // Events Section
        sb.append("# Events\n");
        sb.append("event_time_ms\n");
        for (Long eventTime : eventTime) {
            sb.append(eventTime).append("\n");
        }

        return sb.toString();
    }
*/
    public String createCSV() {
        StringBuilder sb = new StringBuilder();

        // Unified header
        sb.append("timestamp,accX,accY,accZ,gyroX,gyroY,gyroZ,event_time\n");

        ArrayList<String[]> rows = sensorDataMap.get("Synchronized");
        if (rows != null) {
            for (String[] row : rows) {
                sb.append(String.join(",", row)).append("\n");
            }
        }

        return sb.toString();
    }
    public boolean exportAsZip(File zipFile) {
        try {
            Log.i("DataExport", "Starting exportAsZip...");

            File csvFile = new File(zipFile.getParent(), zipFile.getName().replace(".zip", ".csv"));
            Log.i("DataExport", "CSV file path: " + csvFile.getAbsolutePath());

            FileOutputStream fos = new FileOutputStream(csvFile);
            String csvContent = createCSV();
            Log.i("DataExport", "CSV content: \n" + csvContent);

            fos.write(csvContent.getBytes());
            fos.close();

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

            if (csvFile.exists()) {
                boolean deleted = csvFile.delete();
                Log.i("DataExport", "CSV file deleted after ZIP? " + deleted);
            }

            Log.i("DataExport", "ZIP file created successfully at: " + zipFile.getAbsolutePath());
            return true; // success
        } catch (IOException e) {
            Log.e("DataExport", "Failed to export zip file", e);
            return false; // error
        }
    }
    public ArrayList<String[]> getSensorData(String sensorType) {
        return sensorDataMap.get(sensorType);
    }

}
