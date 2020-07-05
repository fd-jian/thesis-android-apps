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
    public static final String TAG = "wear:" + AccelerometerListener.class.getSimpleName().toLowerCase();

    private final MessageClient messageClient;
//    private float[] acc = {0.0F, 0.0F, 0.0F};

    @Setter
    @Getter
    private String accelerometerNodeId;

//    private int messageCounter = 0;
//    private long lastMessageReceived = 0;
//    private long secondsElapsed = 0;
//    private long prevTimestamp = Instant.now().getEpochSecond();
//    private float messagesPerSecond = 0F;

    private final SensorDataFileLogger sensorDataFileLogger;
    private OutputStream outputStream;

    public AccelerometerListener(MessageClient messageClient, SensorDataFileLogger sensorDataFileLogger) {
        this.messageClient = messageClient;
        this.sensorDataFileLogger = sensorDataFileLogger;
//        sensorDataFileLogger.log(DateTimeFormatter
//                .ofPattern("yyyy-MM-dd'T'hh:mm:ss")
//                .format(ZonedDateTime.now()) + " -- TEST\n");
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float[] values = event.values;
        messageClient.sendMessage(accelerometerNodeId,
                ACCELEROMETER_MESSAGE_PATH, getSensorBytes(values));

        Optional.ofNullable(outputStream).orElseGet(() ->
                outputStream = sensorDataFileLogger.getOutputStream());

        try {
            outputStream.write((Arrays.toString(values).replaceAll("[\\[\\] ]", "") + "\n").getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }

//        ++messageCounter;
//        lastMessageReceived = Instant.now().toEpochMilli();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Log.i(TAG, "Accuracy of Accelerometer changed.");
    }

    private byte[] getSensorBytes(float[] accelerometerValues) {
        byte[] res = new byte[12];
        for (int i = 0; i < accelerometerValues.length; i++) {
            byte[] buff = ByteBuffer.allocate(4).putFloat(accelerometerValues[i]).array();
            System.arraycopy(buff, 0, res, i * 4, buff.length);
        }
        return res;
    }

    public void closeStream() {
        if(outputStream == null) {
            return;
        }
        try {
            outputStream.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
