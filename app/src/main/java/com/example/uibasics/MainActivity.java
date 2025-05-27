package com.example.uibasics;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements RecordFragment.OnRecordControlListener {

    private DataExport dataExport;
    private long recordingStartTime;
    private boolean isAccelEnabled, isGyroEnabled, isGPSEnabled;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ViewPager2 viewPager = findViewById(R.id.viewPager);
        TabLayout tabLayout = findViewById(R.id.tabLayout);

        ViewPagerAdapter adapter = new ViewPagerAdapter(this);
        adapter.addFragment(new RecordFragment(), "Record");
        adapter.addFragment(new PlotFragment(), "Plot");

        viewPager.setAdapter(adapter);

        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> tab.setText(adapter.getTitle(position))
        ).attach();
    }
    @Override
    public void onStartRecording(boolean accel, boolean gyro, boolean gps) {
        isAccelEnabled = accel;
        isGyroEnabled = gyro;
        isGPSEnabled = gps;

        recordingStartTime = System.currentTimeMillis();

        // Initialize dataExporter
        dataExport = new DataExport(new ArrayList<>(), new ArrayList<>()); // start with empty event list

        // Start data collection here (or in PlotFragment if you're simulating)
        if (isAccelEnabled) {
            // Start accelerometer data collection
        }
        if (isGyroEnabled) {
            // Start gyroscope data collection
        }
        if (isGPSEnabled) {
            // Start GPS data collection
        }

        // Start PlotFragment simulation (like before)
        ViewPagerAdapter adapter = (ViewPagerAdapter) ((ViewPager2) findViewById(R.id.viewPager)).getAdapter();
        if (adapter != null) {
            PlotFragment plotFragment = (PlotFragment) adapter.getFragment(1); // Second fragment
            if (plotFragment != null) {
                plotFragment.startSimulatingData();
            }
        }
    }
    @Override
    public void onEventRecorded() {
        // Record the event timestamp relative to the recording start
        long relativeTime = System.currentTimeMillis() - recordingStartTime;
        if (dataExport != null) {
            dataExport.addEvent(relativeTime);
        }
    }
    @Override
    public void onStopRecording() {

        // Stop sensor recordings here
        // (Might also want to flush buffers to dataExporter if needed)

        ViewPagerAdapter adapter = (ViewPagerAdapter) ((ViewPager2) findViewById(R.id.viewPager)).getAdapter();
        if (adapter != null) {
            PlotFragment plotFragment = (PlotFragment) adapter.getFragment(1);
            if (plotFragment != null) {
                plotFragment.stopSimulatingData();
            }
        }
    }



    @Override
    public void onExportRecording(String recordingName) {
        try {
            File zipFile = dataExport.exportAsZip(this, recordingName);
            // You might want to share the file or save it permanently here
            Toast.makeText(this, "Export complete: " + zipFile.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Export failed", Toast.LENGTH_SHORT).show();
        }
    }

}


