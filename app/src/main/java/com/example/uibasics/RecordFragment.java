package com.example.uibasics;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class RecordFragment extends Fragment implements View.OnClickListener {

    private TextView txtRecProgress;
    private EditText recordingID;
    private CheckBox checkBoxAccel, checkBoxGyro, checkBoxGPS;
    private Button startBtn, stopBtn, exportBtn, eventBtn;
    private OnRecordControlListener recordingControlListener; //Interface

    public RecordFragment() {
        // Required empty public constructor
    }

    public interface OnRecordControlListener { //Define interface for notifying Main Activity when start and stop button pressed
        void onStartRecording();
        void onStopRecording();

    }
    // Attach the interface to the activity (listener set-up)
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnRecordControlListener) {
            recordingControlListener = (OnRecordControlListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement OnRecordControlListener");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_record, container, false);

        // Initialize UI references
        txtRecProgress = view.findViewById(R.id.txtRecordingProgress);
        recordingID = view.findViewById(R.id.editRecordingName);
        checkBoxAccel = view.findViewById(R.id.checkboxLinAccelerometer);
        checkBoxGyro = view.findViewById(R.id.checkboxGyroscope);
        checkBoxGPS = view.findViewById(R.id.checkboxGPS);

        //Set buttons
        startBtn = view.findViewById(R.id.startBtn);
        stopBtn = view.findViewById(R.id.stopBtn);
        exportBtn = view.findViewById(R.id.exportBtn);
        eventBtn = view.findViewById(R.id.eventBtn);

        // Set click listeners
        startBtn.setOnClickListener(this);
        stopBtn.setOnClickListener(this);
        exportBtn.setOnClickListener(this);
        eventBtn.setOnClickListener(this);

        return view;
    }

    @Override
    public void onClick(View v) {

        if (recordingControlListener != null){ //Signal Main Activity to start live plotting
            recordingControlListener.onStartRecording();
        }

        if (v.getId() == R.id.startBtn) {
            Toast.makeText(getActivity(), "Recording: " + recordingID.getText().toString() + " has started", Toast.LENGTH_LONG).show();
            txtRecProgress.setText("Recording in Progress");

            startBtn.setEnabled(false);
            eventBtn.setEnabled(true);
            stopBtn.setEnabled(true);

            if (checkBoxAccel.isChecked()) {
                // TODO: Start recording linear accelerometer data
            }
            if (checkBoxGyro.isChecked()) {
                // TODO: Start recording gyroscope data
            }
            if (checkBoxGPS.isChecked()) {
                // TODO: Start recording GPS data
            }

        } else if (v.getId() == R.id.eventBtn) {
            Toast.makeText(getActivity(), "Event time point has been recorded", Toast.LENGTH_SHORT).show();
            // TODO: Log event timestamp

        } else if (v.getId() == R.id.stopBtn) {
            Toast.makeText(getActivity(), "Recording: " + recordingID.getText().toString() + " has stopped", Toast.LENGTH_SHORT).show();
            txtRecProgress.setText("Recording has stopped");

            startBtn.setEnabled(true);
            eventBtn.setEnabled(false);
            stopBtn.setEnabled(false);
            // TODO: Stop recording

            if (recordingControlListener != null){ //Signal Main Activity to stop live plotting
                recordingControlListener.onStopRecording();
            }

        } else if (v.getId() == R.id.exportBtn) {
            Toast.makeText(getActivity(), "Exporting " + recordingID.getText().toString() + " data", Toast.LENGTH_SHORT).show();
            // TODO: Save or upload CSV
        }
    }



}
