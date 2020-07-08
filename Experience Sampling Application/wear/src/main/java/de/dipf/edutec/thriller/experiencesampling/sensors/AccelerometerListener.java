package de.dipf.edutec.thriller.experiencesampling.sensors;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.util.Log;
import com.google.android.gms.wearable.MessageClient;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Optional;

public class AccelerometerListener implements SensorEventListener {

    private static final String ACCELEROMETER_MESSAGE_PATH = "/accelerometer_data";
    public static final String TAG = "wear:" + AccelerometerListener.class.getSimpleName();

    private final MessageClient messageClient;
    private final SensorDataFileLogger sensorDataFileLogger;
    private OutputStream outputStream;

    // reuse these field values to avoid reinstantiation
    private final ByteBuffer byteBuffer = ByteBuffer.allocate(4);
    private final byte[] recordBytes = new byte[12];
    private final byte[] floatBytes = new byte[4];
    private float[] values;

    @Setter
    @Getter
    private String accelerometerNodeId;

    public AccelerometerListener(MessageClient messageClient, SensorDataFileLogger sensorDataFileLogger) {
        this.messageClient = messageClient;
        this.sensorDataFileLogger = sensorDataFileLogger;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        values = event.values;
        messageClient.sendMessage(accelerometerNodeId,
                ACCELEROMETER_MESSAGE_PATH, getRecordBytes(values));

        String out = Arrays.toString(values).replaceAll("[\\[\\] ]", "") + "\n";
        Log.v(TAG, String.format("sensor data record: %s", Arrays.toString(values)));

        try {
            Optional.ofNullable(outputStream)
                    .orElseGet(() -> outputStream = sensorDataFileLogger.getOutputStream())
                    .write(out.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Log.i(TAG, "Accuracy of Accelerometer changed.");
    }

    private byte[] getRecordBytes(float[] accelerometerValues) {
        for (int i = 0; i < accelerometerValues.length; i++) {
            byteBuffer.putFloat(accelerometerValues[i]).position(0);
            byteBuffer.get(floatBytes);
            System.arraycopy(floatBytes, 0, recordBytes, i * 4, floatBytes.length);
            byteBuffer.position(0);
        }

        return recordBytes;
    }

    public void closeStream() {
        if (outputStream == null) {
            return;
        }
        try {
            outputStream.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
