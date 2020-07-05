package de.dipf.edutec.thriller.experiencesampling.sensors;

import android.hardware.Sensor;
import android.hardware.SensorManager;
import com.google.android.gms.wearable.CapabilityClient;

public class Helper {

    public static SensorManager sensorManager;
    public static CapabilityClient capabilityClient;
    public static AccelerometerListener accelerometerListener;
    public static Sensor accelerometer;
}
