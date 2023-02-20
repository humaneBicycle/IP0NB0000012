package com.humaneBicycle.stepCount.model;

public class Accelerometer {
    public float X;
    public float Y;
    public float Z;
    public double acceleration;


    public Accelerometer(float[] event) {
        X = event[0];
        Y = event[1];
        Z = event[2];
        acceleration = Math.sqrt(X*X + Y*Y + Z*Z);
    }

    public Number toNumber() {
        Number number = acceleration;
        return number;
    }
}
