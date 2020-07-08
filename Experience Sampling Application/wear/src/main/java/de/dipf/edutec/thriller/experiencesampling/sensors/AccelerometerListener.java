package de.dipf.edutec.thriller.experiencesampling.sensors;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.util.Log;
import com.google.android.gms.wearable.ChannelClient;
import com.google.android.gms.wearable.MessageClient;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

// TODO: consider moving logic of this class to SensorDataService for ease of management (e.g. close streams).
public class AccelerometerListener implements SensorEventListener {

    public static final String TAG = "wear:" + AccelerometerListener.class.getSimpleName();

    @Setter
    private OutputStream channelOutput;
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

    public AccelerometerListener(SensorDataFileLogger sensorDataFileLogger) {
        this.sensorDataFileLogger = sensorDataFileLogger;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        values = event.values;

        // TODO: what to do if stream is closed from other device? how to reopen?
        try {
            if(channelOutput != null) {
                channelOutput.write(getRecordBytes(values));
                channelOutput.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

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
        Stream.of(outputStream, channelOutput).forEach(this::closeStream);
    }

    private void closeStream(OutputStream outputStream) {
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
