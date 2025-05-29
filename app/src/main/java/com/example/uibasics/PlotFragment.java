package com.example.uibasics;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
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

public class PlotFragment extends Fragment {

    private LineChart lineChartAccel, lineChartGyro;
    private LineDataSet accelDataSet, gyroDataSet;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_plot, container, false);

        //Log.d("PlotFragment", "onCreateView: PlotFragment is initialized!"); //Check if plot has been initialized - uncomment for debugging

        lineChartAccel = view.findViewById(R.id.lineChartAccel);
        lineChartGyro = view.findViewById(R.id.lineChartGyro);

        // Accel chart setup
        accelDataSet = new LineDataSet(new ArrayList<>(), "Acceleration vs Time");
        accelDataSet.setColor(Color.parseColor("#5900b3"));
        XAxis xAxisAccel = lineChartAccel.getXAxis();
        xAxisAccel.setGranularity(0.1f);
        xAxisAccel.setDrawGridLines(false);
        xAxisAccel.setLabelRotationAngle(0f);
        accelDataSet.setLineWidth(2f);
        accelDataSet.setDrawCircles(false);
        accelDataSet.setDrawValues(false);
        lineChartAccel.setData(new LineData(accelDataSet));
        lineChartAccel.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        lineChartAccel.getXAxis().setValueFormatter(new UnitValueFormatter());
        lineChartAccel.getAxisLeft().setValueFormatter(new UnitValueFormatter());
        lineChartAccel.getDescription().setEnabled(false);

        // Gyro chart setup
        gyroDataSet = new LineDataSet(new ArrayList<>(), "Angular Velocity vs Time");
        XAxis xAxisGyro = lineChartGyro.getXAxis();
        xAxisGyro.setGranularity(0.1f);
        xAxisGyro.setDrawGridLines(false);
        xAxisGyro.setLabelRotationAngle(0f);
        gyroDataSet.setColor(Color.parseColor("#3264a8"));
        gyroDataSet.setLineWidth(2f);
        gyroDataSet.setDrawCircles(false);
        gyroDataSet.setDrawValues(false);
        lineChartGyro.setData(new LineData(gyroDataSet));
        lineChartGyro.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        lineChartGyro.getXAxis().setValueFormatter(new UnitValueFormatter());
        lineChartGyro.getAxisLeft().setValueFormatter(new UnitValueFormatter());
        lineChartGyro.getDescription().setEnabled(false);

        return view;
    }

    //Add accel data to the live chart
    public void addAccelData(long timestamp, float accX) {
        //Log.d("PlotFragment", "Adding Accel Data: " + accX); //Check if the accel data is being added to the live plot - uncomment for debugging
        if (accelDataSet != null) {
            float currentTime = timestamp / 1000.0f; // convert ms → s
            accelDataSet.addEntry(new Entry(currentTime, accX));
            lineChartAccel.getData().notifyDataChanged();
            lineChartAccel.notifyDataSetChanged();

            // Rolling window - Only show last 5 seconds worth of data on a rolling basis
            float windowSize = 5.0f;
            lineChartAccel.getXAxis().setAxisMinimum(currentTime - windowSize);
            lineChartAccel.getXAxis().setAxisMaximum(currentTime);

            lineChartAccel.invalidate();
        }
    }

    //Add gyro data to the live chart
    public void addGyroData(long timestamp, float gyroX) {
        if (gyroDataSet != null) {
            float currentTime = timestamp / 1000.0f; // convert ms → s
            gyroDataSet.addEntry(new Entry(currentTime, gyroX));
            lineChartGyro.getData().notifyDataChanged();
            lineChartGyro.notifyDataSetChanged();

            // Rolling window - Only show last 5 seconds worth of data on a rolling basis
            float windowSize = 5.0f;
            lineChartGyro.getXAxis().setAxisMinimum(currentTime - windowSize);
            lineChartGyro.getXAxis().setAxisMaximum(currentTime);

            lineChartGyro.invalidate();
        }
    }

    //Formatter for displaying time on the Xaxis of the live plots
    public static class UnitValueFormatter extends ValueFormatter {
        @Override
        public String getFormattedValue(float value) {
            return String.format("%.1f", value);
        }
    }
}
