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

    private PowerManager.WakeLock wakeLock;
    private AccelerometerListener accelerometerListener;
    private Sensor accelerometer;
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

        Log.d(TAG, "Creating sensordataservice");

        sensorManager = Objects.requireNonNull((SensorManager)
                getApplicationContext().getSystemService(SENSOR_SERVICE));

        capabilityClient = Wearable.getCapabilityClient(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Handling intent");

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
        Log.d(TAG, "capability changed");

        Set<Node> connectedNodes = capabilityInfo.getNodes();
        Log.d(TAG, "Updating capability: " + capabilityInfo.getName() + ", nodes found: " + connectedNodes.size());

        accelerometerNodeId = pickBestNodeId(connectedNodes);
        Log.d(TAG, "AccelerometerNodeId is now " + accelerometerNodeId);

        if(accelerometerNodeId != null) {
            openChannel();
        }
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "calling onDestroy");
        super.onDestroy();

        sensorManager.unregisterListener(accelerometerListener, accelerometer);
        Log.d(TAG, "Unregistered accelerometer listener.");

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
        Log.d(TAG, "Opening channel to node id " + accelerometerNodeId);
        channelClient = Wearable.getChannelClient(getApplicationContext());
        channelClient.openChannel(accelerometerNodeId, "/accelerometer_data")
                .addOnSuccessListener(this::registerSensorListener);
    }

    public void registerSensorListener(ChannelClient.Channel channel) {
        Log.d(TAG, "channel opened.");
        channelClient.getOutputStream(channel).addOnSuccessListener(outputStream -> {

            if (accelerometerListener != null) {
                Log.d(TAG, "listener not null. set new outputstream");
                accelerometerListener.channelOutput = outputStream;
            } else {
                Log.d(TAG, "listener is null. instantiate with outputstream");
                accelerometerListener = new AccelerometerListener(
                        SensorDataFileLogger.create(getApplicationContext()), outputStream);
            }

            accelerometer = Objects.requireNonNull(
                    sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION));

            Log.d(TAG, "registering accelerometer listener");
            if (!sensorManager.registerListener(
                    accelerometerListener,
                    accelerometer,
                    SensorManager.SENSOR_DELAY_FASTEST)) {
                Log.e(TAG, "Could not register accelerometer listener");
            }
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
        private final byte[] recordBytes = new byte[12];
        private final byte[] floatBytes = new byte[4];

        public AccelerometerListener(SensorDataFileLogger sensorDataFileLogger, OutputStream channelOutput) {
            this.sensorDataFileLogger = sensorDataFileLogger;
            this.channelOutput = channelOutput;
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            if (channelOutput != null) {
                try {
                    channelOutput.write(getRecordBytes(event.values));
                    channelOutput.flush();
                } catch (IOException e) {
                    Log.d(TAG, "Could not write to channel stream: " + e.getCause());
                    closeStream(channelOutput);
                    channelOutput = null; // in case unregister takes some time to avoid repeated failures
                    sensorManager.unregisterListener(this, accelerometer);
                    Log.d(TAG, "listener unregistered and stream closed, reopening channel.");

                    openChannel();
                }
            }

            String out = Arrays.toString(event.values).replaceAll("[\\[\\] ]", "") + "\n";
            Log.v(ACC_TAG, String.format("sensor data record: %s", Arrays.toString(event.values)));

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
