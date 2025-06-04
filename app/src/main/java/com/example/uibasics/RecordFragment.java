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
    public interface OnRecordControlListener {
        void onStartRecording(boolean accel, boolean gyro, boolean gps);
        void onStopRecording();
        void onEventRecorded();
        void onExportRecording(String recordingName);
    }
    // Attach the interface to the activity (listener set-up)
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnRecordControlListener) {
            recordingControlListener = (OnRecordControlListener) context;
        } else {
            throw new RuntimeException(context + " must implement OnRecordControlListener");
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
        checkBoxAccel = view.findViewById(R.id.checkboxLinAccelerometer);
        checkBoxGyro = view.findViewById(R.id.checkboxGyroscope);
        checkBoxGPS = view.findViewById(R.id.checkboxGPS);
        recordingID = view.findViewById(R.id.editRecordingName);

        //Collapse keyboard after entering recording name by clicking return
        recordingID.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
                android.view.inputmethod.InputMethodManager imm =
                        (android.view.inputmethod.InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(recordingID.getWindowToken(), 0);
                recordingID.clearFocus();
                return true;
            }
            return false;
        });

        //Setup buttons
        startBtn = view.findViewById(R.id.startBtn);
        stopBtn = view.findViewById(R.id.stopBtn);
        exportBtn = view.findViewById(R.id.exportBtn);
        eventBtn = view.findViewById(R.id.eventBtn);

        //Setup click listeners
        startBtn.setOnClickListener(this);
        stopBtn.setOnClickListener(this);
        exportBtn.setOnClickListener(this);
        eventBtn.setOnClickListener(this);

        //Disable all buttons except for start button
        startBtn.setEnabled(true);
        eventBtn.setEnabled(false);
        stopBtn.setEnabled(false);
        exportBtn.setEnabled(false);

        return view;
    }
    @Override
    public void onClick(View v) { //Once click has been registered by the onClickListener - need to figure out what the click is meant for

        String recordingName = recordingID.getText().toString().trim(); //Used to check if recording name is empty before proceeding

        if (recordingName.isEmpty()){
            Toast.makeText(getContext(), "Please enter recording name before starting", Toast.LENGTH_SHORT).show();

        } else if (v.getId() == R.id.startBtn) {
            txtRecProgress.setText("Recording in Progress");

            //Disable EditText to prevent user from changing recording name
            recordingID.setEnabled(false);

            //Enable event button and stop button, disable start button once pressed
            startBtn.setEnabled(false);
            eventBtn.setEnabled(true);
            stopBtn.setEnabled(true);

            // Disable checkboxes to lock selections
            checkBoxAccel.setEnabled(false);
            checkBoxGyro.setEnabled(false);
            checkBoxGPS.setEnabled(false);

            if (recordingControlListener != null){ //Signal Main Activity to start recording and live plotting appropriate selections
                recordingControlListener.onStartRecording(
                        checkBoxAccel.isChecked(),
                        checkBoxGyro.isChecked(),
                        checkBoxGPS.isChecked()
                );
            }

        } else if (v.getId() == R.id.eventBtn) {
            Toast.makeText(getActivity(), "Event time point has been recorded", Toast.LENGTH_SHORT).show(); //To test if time-point being recorded
            if(recordingControlListener !=null){
                recordingControlListener.onEventRecorded(); //Tells MainActivity to record event
            }

        } else if (v.getId() == R.id.stopBtn) {
            txtRecProgress.setText("Recording has stopped");

            //Enable export and start buttons, disable event and stop.
            startBtn.setEnabled(false);
            eventBtn.setEnabled(false);
            stopBtn.setEnabled(false);
            exportBtn.setEnabled(true);

            if (recordingControlListener != null){ //Signal Main Activity to stop live plotting
                recordingControlListener.onStopRecording();
            }

        } else if (v.getId() == R.id.exportBtn) {
            if (recordingControlListener != null) {
                recordingControlListener.onExportRecording(recordingID.getText().toString());
            }
            Toast.makeText(getActivity(), recordingID.getText().toString() + " saved in Downloads folder", Toast.LENGTH_SHORT).show();
            startBtn.setEnabled(true);
            exportBtn.setEnabled(false);

            //Enable EditText to enter new recording name
            recordingID.setEnabled(true);

            //Enable checkboxes to lock selections
            checkBoxAccel.setEnabled(true);
            checkBoxGyro.setEnabled(true);
            checkBoxGPS.setEnabled(true);
        }
    }
}
