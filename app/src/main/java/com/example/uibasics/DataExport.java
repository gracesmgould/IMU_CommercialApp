package com.example.uibasics;

import android.content.Context;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class DataExport {
    private ArrayList<String[]> sensorData;
    private ArrayList<Long> eventTime;

    public DataExport(ArrayList<String[]> sensorData, ArrayList<Long> eventTime) {
        this.sensorData = sensorData;
        this.eventTime = eventTime;
    }
    // NEW: Method to add an event timestamp
    public void addEvent(long timeMs) {
        eventTime.add(timeMs);
    }

    public String createCSV(){
        StringBuilder sb = new StringBuilder();

        // Sensor Data Section
        sb.append("# Sensor Data\n");
        sb.append("timestamp,accX,accY,accZ\n");
        for (String[] row : sensorData) {
            sb.append(String.join(",", row)).append("\n");
        }

        // Events Section
        sb.append("\n# Events\n");
        sb.append("event_time_ms\n");
        for (Long eventTime : eventTime) {
            sb.append(eventTime).append("\n");
        }

        return sb.toString();
    }
    public File exportAsZip(Context context, String fileName) throws IOException {
        // Create CSV file
        File csvFile = new File(context.getCacheDir(), fileName + ".csv");
        FileOutputStream fos = new FileOutputStream(csvFile);
        fos.write(createCSV().getBytes());
        fos.close();

        // Compress into ZIP
        File zipFile = new File(context.getCacheDir(), fileName + ".zip");
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

        return zipFile; // return the ZIP file for sharing/export
    }


}
