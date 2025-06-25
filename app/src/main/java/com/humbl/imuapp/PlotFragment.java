package com.humbl.imuapp;

import android.graphics.Color;
import android.os.Bundle;
//import android.util.Log; //Uncomment for Log debugging statements
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import androidx.fragment.app.Fragment;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import java.util.ArrayList;

public class PlotFragment extends Fragment {
    private LineChart lineChartAccel, lineChartGyro;
    private LineDataSet accelDataSetX, accelDataSetY, accelDataSetZ;
    private LineDataSet gyroDataSetX, gyroDataSetY, gyroDataSetZ;

    private CheckBox checkboxAccelX, checkboxAccelY, checkboxAccelZ;
    private CheckBox checkboxGyroX, checkboxGyroY, checkboxGyroZ;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_plot, container, false);

        checkboxAccelX = view.findViewById(R.id.checkboxX_Accel);
        checkboxAccelY = view.findViewById(R.id.checkboxY_Accel);
        checkboxAccelZ = view.findViewById(R.id.checkboxZ_Accel);
        checkboxGyroX = view.findViewById(R.id.checkboxX_Gyro);
        checkboxGyroY = view.findViewById(R.id.checkboxY_Gyro);
        checkboxGyroZ = view.findViewById(R.id.checkboxZ_Gyro);
        setupCheckboxListeners();

        lineChartAccel = view.findViewById(R.id.lineChartAccel);
        lineChartGyro = view.findViewById(R.id.lineChartGyro);

        setupChart(lineChartAccel, true);
        setupChart(lineChartGyro, false); //false for boolean of isAccel

        return view;
    }

    private void setupChart(LineChart chart, boolean isAccel) {
        LineData data = new LineData();

        if (isAccel) {
            accelDataSetX = createDataSet("Acc X", Color.RED);
            accelDataSetY = createDataSet("Acc Y", Color.GREEN);
            accelDataSetZ = createDataSet("Acc Z", Color.BLUE);
            data.addDataSet(accelDataSetX);
            data.addDataSet(accelDataSetY);
            data.addDataSet(accelDataSetZ);
        } else {
            gyroDataSetX = createDataSet("Gyro X", Color.MAGENTA);
            gyroDataSetY = createDataSet("Gyro Y", Color.CYAN);
            gyroDataSetZ = createDataSet("Gyro Z", Color.DKGRAY);
            data.addDataSet(gyroDataSetX);
            data.addDataSet(gyroDataSetY);
            data.addDataSet(gyroDataSetZ);
        }

        chart.setData(data);
        chart.getXAxis().setEnabled(true);
        chart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        chart.getXAxis().setGranularity(0.1f);
        chart.getXAxis().setValueFormatter(new UnitValueFormatter());
        chart.getAxisLeft().setValueFormatter(new UnitValueFormatter());
        chart.getDescription().setEnabled(false);
    }

    private LineDataSet createDataSet(String label, int color) {
        LineDataSet dataSet = new LineDataSet(new ArrayList<>(), label);
        dataSet.setColor(color);
        dataSet.setLineWidth(2f);
        dataSet.setDrawCircles(false);
        dataSet.setDrawValues(false);
        return dataSet;
    }

    public void addAccelData(long timestamp, float accX, float accY, float accZ) {
        if (lineChartAccel == null) return; // prevent null crash
        float time = timestamp / 1000f;
        LineData dataAccel = lineChartAccel.getData();
        if (dataAccel == null) return;

        // Always add data regardless of checkbox state
        accelDataSetX.addEntry(new Entry(time, accX));
        accelDataSetY.addEntry(new Entry(time, accY));
        accelDataSetZ.addEntry(new Entry(time, accZ));

        dataAccel.notifyDataChanged();
        lineChartAccel.notifyDataSetChanged();
        updateWindow(lineChartAccel, time);
    }

    public void addGyroData(long timestamp, float gyroX, float gyroY, float gyroZ) {
        if(lineChartGyro == null) return;
        float time = timestamp / 1000f;
        LineData dataGyro = lineChartGyro.getData();
        if (dataGyro == null) return;

        // Always add data regardless of checkbox state
        gyroDataSetX.addEntry(new Entry(time, gyroX));
        gyroDataSetY.addEntry(new Entry(time, gyroY));
        gyroDataSetZ.addEntry(new Entry(time, gyroZ));

        dataGyro.notifyDataChanged();
        lineChartGyro.notifyDataSetChanged();
        updateWindow(lineChartGyro, time);
    }

    private void updateWindow(LineChart chart, float time) {
        float windowSize = 5.0f;
        chart.getXAxis().setAxisMinimum(time - windowSize);
        chart.getXAxis().setAxisMaximum(time);
        chart.invalidate();
    }

    private void setupCheckboxListeners() {
        CompoundButton.OnCheckedChangeListener listener = (buttonView, isChecked) -> {
            updateDatasetVisibility();
        };

        checkboxAccelX.setOnCheckedChangeListener(listener);
        checkboxAccelY.setOnCheckedChangeListener(listener);
        checkboxAccelZ.setOnCheckedChangeListener(listener);
        checkboxGyroX.setOnCheckedChangeListener(listener);
        checkboxGyroY.setOnCheckedChangeListener(listener);
        checkboxGyroZ.setOnCheckedChangeListener(listener);
    }

    private void updateDatasetVisibility() {
        accelDataSetX.setVisible(checkboxAccelX.isChecked());
        accelDataSetY.setVisible(checkboxAccelY.isChecked());
        accelDataSetZ.setVisible(checkboxAccelZ.isChecked());

        gyroDataSetX.setVisible(checkboxGyroX.isChecked());
        gyroDataSetY.setVisible(checkboxGyroY.isChecked());
        gyroDataSetZ.setVisible(checkboxGyroZ.isChecked());

        lineChartAccel.invalidate();
        lineChartGyro.invalidate();
    }

    public void resetCharts() {
        if (lineChartAccel != null) {
            if (accelDataSetX != null) accelDataSetX.clear();
            if (accelDataSetY != null) accelDataSetY.clear();
            if (accelDataSetZ != null) accelDataSetZ.clear();
            lineChartAccel.getXAxis().removeAllLimitLines();
            lineChartAccel.getData().notifyDataChanged();
            lineChartAccel.notifyDataSetChanged();
            lineChartAccel.invalidate();
        }

        if (lineChartGyro != null) {
            if (gyroDataSetX != null) gyroDataSetX.clear();
            if (gyroDataSetY != null) gyroDataSetY.clear();
            if (gyroDataSetZ != null) gyroDataSetZ.clear();
            lineChartGyro.getXAxis().removeAllLimitLines();
            lineChartGyro.getData().notifyDataChanged();
            lineChartGyro.notifyDataSetChanged();
            lineChartGyro.invalidate();
        }
    }

    public void addEventMarker(float eventTimeX) {
        if (lineChartAccel != null && lineChartAccel.getXAxis() != null) {
            LimitLine eventLine = new LimitLine(eventTimeX, "Event");
            eventLine.setLineColor(Color.BLACK);
            eventLine.setLineWidth(2f);
            eventLine.setTextColor(Color.BLACK);
            eventLine.setTextSize(10f);

            XAxis x1 = lineChartAccel.getXAxis();
            x1.addLimitLine(eventLine);
            x1.setDrawLimitLinesBehindData(true);
            lineChartAccel.invalidate();
        } else {
            android.util.Log.w("PlotFragment", "lineChartAccel or its XAxis is null in addEventMarker()");
        }

        if (lineChartGyro != null && lineChartGyro.getXAxis() != null) {
            LimitLine eventLine2 = new LimitLine(eventTimeX, "Event");
            eventLine2.setLineColor(Color.BLACK);
            eventLine2.setLineWidth(2f);
            eventLine2.setTextColor(Color.BLACK);
            eventLine2.setTextSize(10f);

            XAxis x2 = lineChartGyro.getXAxis();
            x2.addLimitLine(eventLine2);
            x2.setDrawLimitLinesBehindData(true);
            lineChartGyro.invalidate();
        } else {
            android.util.Log.w("PlotFragment", "lineChartGyro or its XAxis is null in addEventMarker()");
        }
    }
    public static class UnitValueFormatter extends ValueFormatter {
        @Override
        public String getFormattedValue(float value) {
            return String.format("%.1f", value);
        }
    }

}
