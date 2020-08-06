package de.dipf.edutec.thriller.experiencesampling.sensorservice;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.PowerManager;
import android.os.Process;
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

public class SensorDataService extends WearableListenerService {
    public static final int MULTIPLY_SENSOR_RECORDS_BY = 1;
    public static final int DELAY_MILLIS = 1000;
    public static final String LAST_SECOND_INTENT_EXTRA = "records-per-second";
    public static final String UPDATED_ANALYTICS_INTENT_ACTION = "updated-analytics";
    public static final String RECORDS_PER_SECOND_INTENT_EXTRA = "records-last-second";
    public static boolean isRunning = false;

    private static final String WEAR_WAKELOCKTAG = "wear:wakelock-service";
    private static final String TAG = "wear:" + SensorDataService.class.getSimpleName();
    private static final String ACC_TAG = "wear:" + AccelerometerListener.class.getSimpleName();
    private static final String ACCELEROMETER_RECEIVER_CAPABILITY = "accelerometer_receiver";
    private static final int[] ENABLED_SENSORS = new int[]{
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
    private HandlerThread sensorThread;
    private LocalBroadcastManager broadcastManager;

    private int messageCount = 0;
    private int messageCountTotal = 0;
    private long millisElapsed = 0;
    private long lastUpdate = -1;
    private long lastMessage;

    private final Handler statHandler = new Handler();
    private final Runnable stats = new Runnable() {
        @Override
        public void run() {
            long now = Instant.now().toEpochMilli();
            if (lastUpdate == -1) {
                lastUpdate = now;
            }
            long sinceLastUpdate = now - lastUpdate;

            int count = messageCount;
            messageCount = 0;

            millisElapsed += sinceLastUpdate;
            lastUpdate = now;
            long deltaMessage = now - lastMessage;
            if (deltaMessage > 5000) {
                millisElapsed = 0;
                messageCountTotal = 0;
            }

            float value = 0F;
            float v = 0F;
            if (millisElapsed != 0) {
                v = (float) millisElapsed / (float) 1000;
                value = messageCountTotal / v;
            }

            Log.d(TAG, String.format("total: %s, millisElapsed: %s, seconds elapsed: %s", messageCountTotal, millisElapsed, v));
            Log.d(TAG, String.format("Last second: %d, Per second avg: %f", count, value));

            sendBroadcastUpdate(count, value);

            long offset = millisElapsed % 1000;
            statHandler.postDelayed(this, DELAY_MILLIS - offset);
        }
    };

    private void sendBroadcastUpdate(int count, float value) {
        Intent intent = new Intent();
        intent.setAction(UPDATED_ANALYTICS_INTENT_ACTION);
        intent.putExtra(LAST_SECOND_INTENT_EXTRA, count);
        intent.putExtra(RECORDS_PER_SECOND_INTENT_EXTRA, value); // TODO: implement average records per second
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        isRunning = false;
        this.fgNotificationCreator = Objects.requireNonNull((CustomApplication) getApplication())
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

        if (isRunning && accelerometerNodeId != null) {
            openChannel();
        }
    }

    private void openChannel() {
        Log.i(TAG, "Opening channel to node id " + accelerometerNodeId);
        channelClient = Wearable.getChannelClient(getApplicationContext());
        channelClient.openChannel(accelerometerNodeId, "/accelerometer_data")
                .addOnSuccessListener(this::registerSensorListener);
    }

    public void registerSensorListener(ChannelClient.Channel channel) {
        channelClient.getOutputStream(channel).addOnSuccessListener(outputStream -> {
            if (accelerometerListener != null) {
                Log.d(TAG, "listener not null. set new outputstream");
                accelerometerListener.channelOutput = outputStream;
            } else {
                Log.d(TAG, "listener is null. instantiate with outputstream");
                accelerometerListener = new AccelerometerListener(outputStream);
            }

            statHandler.post(stats);

            sensorThread = new HandlerThread("Sensor thread", Process.THREAD_PRIORITY_MORE_FAVORABLE);
            sensorThread.start();

            Arrays.stream(ENABLED_SENSORS).forEach(value -> {
                Log.i(TAG, "Registering accelerometer listener.");
                if (sensorManager.registerListener(
                        accelerometerListener,
                        Objects.requireNonNull(sensorManager.getDefaultSensor(value)),
                        SensorManager.SENSOR_DELAY_FASTEST,
                        new Handler(sensorThread.getLooper()))) {
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

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy called.");
        super.onDestroy();
        isRunning = false;
        cleanup(accelerometerListener);

        Log.i(TAG, "Unregistered accelerometer listener.");

        Optional.ofNullable(accelerometerListener)
                .ifPresent(AccelerometerListener::closeStream);
        Optional.ofNullable(wakeLock)
                .filter(PowerManager.WakeLock::isHeld)
                .ifPresent(PowerManager.WakeLock::release);

        stopForeground(true);
        LocalBroadcastManager.getInstance(this)
                .sendBroadcast(new Intent("sensor-service-destroyed"));
    }

    private void cleanup(AccelerometerListener accelerometerListener) {
        Optional.ofNullable(accelerometerListener).ifPresent(AccelerometerListener::closeStream);
        sensorManager.unregisterListener(accelerometerListener);
        if (sensorThread != null) {
            sensorThread.quitSafely();
        }
        statHandler.removeCallbacks(stats);
        sendBroadcastUpdate(0, 0F);
    }

    private class AccelerometerListener implements SensorEventListener {
        // reuse these field values to avoid reinstantiation
        private final ByteBuffer byteBuffer = ByteBuffer.allocate(4);
        private final ByteBuffer byteBuffer8Byte = ByteBuffer.allocate(8);
        private final byte[] recordBytes = new byte[40];

        private long now;
        private OutputStream channelOutput;

        public AccelerometerListener(OutputStream channelOutput) {
            this.channelOutput = channelOutput;
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            for (int i = 0; i < MULTIPLY_SENSOR_RECORDS_BY; i++) {
                now = Instant.now().toEpochMilli();
                updateCount();

                if (channelOutput != null) {
                    try {
                        getRecordBytes(event.values, event.sensor.getType());
                        channelOutput.write(recordBytes);
                        channelOutput.flush();
                    } catch (IOException e) {
                        Log.i(TAG, "Could not write to channel stream: " + e.getCause());
                        cleanup(this);
                        channelOutput = null; // in case unregister takes some time to avoid repeated failures
                        Log.i(TAG, "Sensor listener unregistered and stream closed");

                        if (isRunning) {
                            Log.i(TAG, "Still running, reopening channel.");
                            openChannel();
                        }
                    }
                } else {
                    Log.i(TAG, "channelOutput is null");
                }
            }
        }

        private void updateCount() {
            lastMessage = now;
            messageCount++;
            messageCountTotal++;
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            Log.i(ACC_TAG, "Accuracy of Accelerometer changed.");
        }

        private void getRecordBytes(float[] accelerometerValues, int typeLinearAcceleration) {
            // 4*4 extra bytes --> sensortype (4 bytes), length (4 bytes) and timestamp (8 bytes)
            int offset = 0;
            byteBuffer8Byte.putLong(now).position(0);
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
                byteBuffer.get(recordBytes, i * byteBuffer.capacity() + offset, byteBuffer.capacity());
                byteBuffer.position(0);
            }
        }

        public void closeStream() {
            closeStream(channelOutput);
        }

        // method accepts outputstream in case multiple outputstreams are added in this class
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
