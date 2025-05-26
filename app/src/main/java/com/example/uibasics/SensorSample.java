package com.example.uibasics;

//This needs to be the layout of the data for Coralie to start with
public class SensorSample {
    public long timestamp;
    public float accelX, accelY, accelZ;
    public float gyroX, gyroY, gyroZ;

    public SensorSample(long timestamp, float ax, float ay, float az, float gx, float gy, float gz) {
        this.timestamp = timestamp;
        this.accelX = ax;
        this.accelY = ay;
        this.accelZ = az;
        this.gyroX = gx;
        this.gyroY = gy;
        this.gyroZ = gz;
    }
}
