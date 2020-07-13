package de.dipf.edutec.thriller.experiencesampling.sensorservice;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.PowerManager;
import android.util.Log;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import com.google.android.gms.wearable.*;
import de.dipf.edutec.thriller.experiencesampling.conf.CustomApplication;
import de.dipf.edutec.thriller.experiencesampling.foreground.ForegroundNotificationCreator;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

public class SensorDataService extends WearableListenerService {

    public static boolean isRunning = false;

    private static final String WEAR_WAKELOCKTAG = "wear:wakelock-service";
    private static final String TAG = "wear:" + SensorDataService.class.getSimpleName();
    private static final String ACC_TAG = "wear:" + AccelerometerListener.class.getSimpleName();
    private static final String ACCELEROMETER_RECEIVER_CAPABILITY = "accelerometer_receiver";
    private static final int[] ENABLED_SENSORS = new int[] {
            Sensor.TYPE_LINEAR_ACCELERATION
//            Sensor.TYPE_LINEAR_ACCELERATION,
//            Sensor.TYPE_ACCELEROMETER,
//            Sensor.TYPE_GYROSCOPE,
//            Sensor.TYPE_LIGHT
    };

    private PowerManager.WakeLock wakeLock;
    private AccelerometerListener accelerometerListener;
    private SensorManager sensorManager;
    private CapabilityClient capabilityClient;
    private String accelerometerNodeId;
    private ChannelClient channelClient;
    private ForegroundNotificationCreator fgNotificationCreator;

    @Override
    public void onCreate() {
        super.onCreate();

        this.fgNotificationCreator = Objects.requireNonNull( (CustomApplication) getApplication())
                .getContext().getForegroundNotificationCreator();

        Log.i(TAG, "Creating sensordataservice");

        sensorManager = Objects.requireNonNull((SensorManager)
                getApplicationContext().getSystemService(SENSOR_SERVICE));

        capabilityClient = Wearable.getCapabilityClient(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "Starting sensor data service.");

        isRunning = true;

        wakeLock = Objects.requireNonNull((PowerManager) getApplicationContext()
                .getSystemService(Context.POWER_SERVICE))
                .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WEAR_WAKELOCKTAG);

        if (!wakeLock.isHeld()) {
            wakeLock.acquire();
        }

        startForeground(fgNotificationCreator.getId(), fgNotificationCreator.getNotification());

        capabilityClient
                .getCapability(ACCELEROMETER_RECEIVER_CAPABILITY, CapabilityClient.FILTER_REACHABLE)
                .addOnSuccessListener(this::onCapabilityChanged);

        return START_STICKY;
    }

    @Override
    public void onCapabilityChanged(CapabilityInfo capabilityInfo) {
        Log.i(TAG, "capability changed");

        Set<Node> connectedNodes = capabilityInfo.getNodes();
        Log.i(TAG, "Updating capability: " + capabilityInfo.getName() + ", nodes found: " + connectedNodes.size());

        accelerometerNodeId = pickBestNodeId(connectedNodes);
        Log.i(TAG, "AccelerometerNodeId is now " + accelerometerNodeId);

        if(accelerometerNodeId != null) {
            openChannel();
        }
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy called.");
        super.onDestroy();

        sensorManager.unregisterListener(accelerometerListener);
        Log.i(TAG, "Unregistered accelerometer listener.");

        Optional.ofNullable(accelerometerListener)
                .ifPresent(AccelerometerListener::closeStream);

        Optional.ofNullable(wakeLock)
                .filter(PowerManager.WakeLock::isHeld)
                .ifPresent(PowerManager.WakeLock::release);

        stopForeground(true);

        LocalBroadcastManager.getInstance(this)
                .sendBroadcast(new Intent("sensor-service-destroyed"));

        isRunning = false;
    }

    private void openChannel() {
        Log.i(TAG, "Opening channel to node id " + accelerometerNodeId);
        channelClient = Wearable.getChannelClient(getApplicationContext());
        channelClient.openChannel(accelerometerNodeId, "/accelerometer_data")
                .addOnSuccessListener(this::registerSensorListener);
    }

    public void registerSensorListener(ChannelClient.Channel channel) {
        Log.i(TAG, "channel '" + channel.getPath() + "' opened.");
        channelClient.getOutputStream(channel).addOnSuccessListener(outputStream -> {

            if (accelerometerListener != null) {
                Log.d(TAG, "listener not null. set new outputstream");
                accelerometerListener.channelOutput = outputStream;
            } else {
                Log.d(TAG, "listener is null. instantiate with outputstream");
                accelerometerListener = new AccelerometerListener(
                        SensorDataFileLogger.create(getApplicationContext()), outputStream);
            }

            Arrays.stream(ENABLED_SENSORS).forEach(value -> {
                Log.i(TAG, "Registering accelerometer listener.");
                if (sensorManager.registerListener(
                        accelerometerListener,
                        Objects.requireNonNull(sensorManager.getDefaultSensor(value)),
                        SensorManager.SENSOR_DELAY_FASTEST)) {
                    Log.i(TAG, "Registered accelerometer listener successfully.");
                } else {
                    Log.e(TAG, "Could not register accelerometer listener.");
                }
            });

        });
    }

    private String pickBestNodeId(Set<Node> nodes) {
        String bestNodeId = null;

        for (Node node : nodes) {
            if (node.isNearby()) {
                return node.getId();
            }
            bestNodeId = node.getId();
        }
        return bestNodeId;
    }

    private class AccelerometerListener implements SensorEventListener {

        private final SensorDataFileLogger sensorDataFileLogger;
        private OutputStream channelOutput;
        private OutputStream outputStream;

        // reuse these field values to avoid reinstantiation
        private final ByteBuffer byteBuffer = ByteBuffer.allocate(4);
        private final ByteBuffer byteBuffer8Byte = ByteBuffer.allocate(8);
        private final byte[] sensorTypeBytes = new byte[4];

        public AccelerometerListener(SensorDataFileLogger sensorDataFileLogger, OutputStream channelOutput) {
            this.sensorDataFileLogger = sensorDataFileLogger;
            this.channelOutput = channelOutput;
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            if (channelOutput != null) {
                try {
                    channelOutput.write(getRecordBytes(event.values, event.sensor.getType()));
                    channelOutput.flush();
                } catch (IOException e) {
                    Log.i(TAG, "Could not write to channel stream: " + e.getCause());
                    closeStream(channelOutput);
                    channelOutput = null; // in case unregister takes some time to avoid repeated failures
                    sensorManager.unregisterListener(this);
                    Log.i(TAG, "Sensor listener unregistered and stream closed, reopening channel.");

                    openChannel();
                }
            }

            String out = Arrays.toString(event.values).replaceAll("[\\[\\] ]", "") + "\n";
            Log.v(ACC_TAG, String.format("Sensor data record: %s", Arrays.toString(event.values)));

            try {
                Optional.ofNullable(outputStream)
                        .orElseGet(() -> outputStream = sensorDataFileLogger.getOutputStream())
                        .write(out.getBytes());
            } catch (IOException e) {
                Log.v(TAG, "Could not write to log file stream: " + e.getCause());
            }

        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            Log.i(ACC_TAG, "Accuracy of Accelerometer changed.");
        }

        private byte[] getRecordBytes(float[] accelerometerValues, int typeLinearAcceleration) {

            // 4*4 extra bytes --> sensortype (4 bytes), length (4 bytes) and timestamp (8 bytes)
            byte[] recordBytes = new byte[40];

            int offset = 0;
            byteBuffer8Byte.putLong(Instant.now().toEpochMilli()).position(0);
            offset += byteBuffer8Byte.get(recordBytes, offset, byteBuffer8Byte.capacity())
                    .position(0)
                    .capacity();

            byteBuffer.putInt(typeLinearAcceleration).position(0);
            offset += byteBuffer.get(recordBytes, offset, byteBuffer.capacity())
                    .position(0)
                    .capacity();

            byteBuffer.putInt(accelerometerValues.length).position(0);
            offset += byteBuffer.get(recordBytes, offset, byteBuffer.capacity())
                    .position(0)
                    .capacity();

            for (int i = 0; i < accelerometerValues.length; i++) {
                byteBuffer.putFloat(accelerometerValues[i]).position(0);
                byteBuffer.get(recordBytes, i*byteBuffer.capacity() + offset, byteBuffer.capacity());
                byteBuffer.position(0);
            }

            Log.d(TAG, "converted record bytes: " + Arrays.toString(recordBytes));
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
                outputStream.flush();
            } catch (IOException e) {
                Log.w(ACC_TAG, "Error flushing output stream");
            }

            try {
                outputStream.close();
            } catch (IOException e) {
                Log.w(ACC_TAG, "Error closing output stream");
            }
        }

    }

}
