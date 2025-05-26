package com.example.uibasics;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.util.ArrayList;
import java.util.Random;

public class PlotFragment extends Fragment {

    private LineChart lineChartAccel, lineChartGyro;
    private LineDataSet accelDataSet, gyroDataSet;
    private Handler handler = new Handler(Looper.getMainLooper());
    private boolean isSimulating = false;
    private int timeIndex = 0;
    private boolean shouldStartSimulating = false; // NEW

    private Runnable simulationRunnable = new Runnable() {
        @Override
        public void run() {
            if (isSimulating) {
                // Generate random values
                float randomAccel = -10 + new Random().nextFloat() * 20;
                float randomGyro = -200 + new Random().nextFloat() * 400;

                float currentTime = timeIndex * 0.1f; // 0.1s sampling period

                // Update charts
                accelDataSet.addEntry(new Entry(currentTime, randomAccel));
                lineChartAccel.getData().notifyDataChanged();
                lineChartAccel.notifyDataSetChanged();

                gyroDataSet.addEntry(new Entry(currentTime, randomGyro));
                lineChartGyro.getData().notifyDataChanged();
                lineChartGyro.notifyDataSetChanged();

                // Rolling window (5s)
                float windowSize = 5.0f;
                lineChartAccel.getXAxis().setAxisMinimum(currentTime - windowSize);
                lineChartAccel.getXAxis().setAxisMaximum(currentTime);

                lineChartGyro.getXAxis().setAxisMinimum(currentTime - windowSize);
                lineChartGyro.getXAxis().setAxisMaximum(currentTime);

                // Redraw
                lineChartAccel.invalidate();
                lineChartGyro.invalidate();

                timeIndex++;
                handler.postDelayed(this, 100);
            }
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_plot, container, false);

        lineChartAccel = view.findViewById(R.id.lineChartAccel);
        lineChartGyro = view.findViewById(R.id.lineChartGyro);

        // Accel chart setup
        accelDataSet = new LineDataSet(new ArrayList<>(), "Simulated Accel");
        accelDataSet.setColor(Color.parseColor("#5900b3"));
        accelDataSet.setLineWidth(2f);
        accelDataSet.setDrawCircles(false);
        accelDataSet.setDrawValues(false);
        lineChartAccel.setData(new LineData(accelDataSet));
        lineChartAccel.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        lineChartAccel.getXAxis().setValueFormatter(new UnitValueFormatter());
        lineChartAccel.getAxisLeft().setValueFormatter(new UnitValueFormatter());
        lineChartAccel.getDescription().setEnabled(false);

        // Gyro chart setup
        gyroDataSet = new LineDataSet(new ArrayList<>(), "Simulated Gyro");
        gyroDataSet.setColor(Color.parseColor("#3264a8"));
        gyroDataSet.setLineWidth(2f);
        gyroDataSet.setDrawCircles(false);
        gyroDataSet.setDrawValues(false);
        lineChartGyro.setData(new LineData(gyroDataSet));
        lineChartGyro.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        lineChartGyro.getXAxis().setValueFormatter(new UnitValueFormatter());
        lineChartGyro.getAxisLeft().setValueFormatter(new UnitValueFormatter());
        lineChartGyro.getDescription().setEnabled(false);

        if (shouldStartSimulating) {
            startSimulatingData(); // Safely start now that charts are initialized
        }
        return view;


    }

    public void startSimulatingData() {
        if (lineChartAccel == null || lineChartGyro == null) {
            // Fragment not ready yet → defer start until onCreateView()
            shouldStartSimulating = true;
            return;
        }
        if (!isSimulating) {
            isSimulating = true;
            timeIndex = 0;

            // Clear and refresh logic
            accelDataSet.clear();
            gyroDataSet.clear();

            lineChartAccel.setData(new LineData(accelDataSet));
            lineChartGyro.setData(new LineData(gyroDataSet));

            lineChartAccel.notifyDataSetChanged();
            lineChartAccel.invalidate();
            lineChartGyro.notifyDataSetChanged();
            lineChartGyro.invalidate();

            handler.postDelayed(simulationRunnable, 100);
        }
    }
    public void stopSimulatingData() {
        isSimulating = false;
        handler.removeCallbacks(simulationRunnable);
    }

    public static class UnitValueFormatter extends ValueFormatter {
        @Override
        public String getFormattedValue(float value) {
            return String.format("%.1f", value);
        }
    }
}
