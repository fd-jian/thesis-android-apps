package de.dipf.edutec.thriller.experiencesampling.sensorservice;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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

/**
 *
 * <p>
 * Listens to records of a specified set of sensors and publishes them through a channel for other wearable nodes.
 * </p>
 * <p>
 * When started, the service registers a {@link SensorEventListener} for each desired {@link Sensor}. Registering is initiated only if
 * started explicitly through the {@link #onStartCommand(Intent, int, int)} callback. {@link CapabilityClient} is then
 * used to acquire all available wearable nodes, and a suitable node is picked. A new {@link ChannelClient.Channel}
 * for the current node ID is opened through {@link ChannelClient} and passed to all instances of
 * {@link AccelerometerListener} that are to be registered. Finally, a {@link HandlerThread} is created and an
 * {@code AccelerometerListener} is registered for each sensor in the created thread. The separate thread prevents the
 * UI performance from being impacted by the listeners.
 * </p>
 * <p>
 * If the service is not started explicitly, it will still listen to changes in capabilities through
 * {@link #onCapabilityChanged(CapabilityInfo)} to keep the node ID up to date with the most suitable node, but no
 * records will be recorded. If capabilities change whilst the service is already running, sensor listeners will
 * be re-registered to publish to a newly opened channel for the updated node ID. This feature allows for automatic
 * resume of recording if nodes are momentarily disconnected and then reconnected. As long as {@code running} is set to
 * {@code true} in {@link SharedPreferences}, changes in capability will always re-register the listeners and start recording.
 * </p>
 * <p>
 * Sensor data is transmitted in as a stream of {@code byte} through the provided {@link OutputStream} of the channel.
 * To be able to deserialize the records on the receiving end, a simple protocol is used: timestamp (8 bytes), sensor type (4 bytes),
 * length of the record array (4 bytes) and the record array itself (n bytes) are serialized to bytes arrays and merged into one byte array in
 * succession. The start of the byte array is always 16 bytes long, whereas the size of the actual records may vary
 * depending on the sensor type. The protocol assumes a maximum length of 40 bytes, which leaves 24 bytes for sensor
 * data per message. 24 bytes equates to 6 float values, the maximum length of a sensor record according to the documentation
 * (<a href="https://developer.android.com/guide/topics/sensors/sensors_motion">https://developer.android.com/guide/topics/sensors/sensors_motion</a>).
 * </p>
 */
public class SensorDataService extends WearableListenerService {
    public static final int MULTIPLY_SENSOR_RECORDS_BY = 1;
    public static final int DELAY_MILLIS = 1000;
    public static final String LAST_SECOND_INTENT_EXTRA = "records-per-second";
    public static final String UPDATED_ANALYTICS_INTENT_ACTION = "updated-analytics";
    public static final String RECORDS_PER_SECOND_INTENT_EXTRA = "records-last-second";

    private static final String WEAR_WAKELOCKTAG = "wear:wakelock-service";
    private static final String TAG = "wear:" + SensorDataService.class.getSimpleName();
    private static final String ACC_TAG = "wear:" + AccelerometerListener.class.getSimpleName();
    public static final String ACCELEROMETER_RECEIVER_CAPABILITY = "accelerometer_receiver";
    private static final int[] ENABLED_SENSORS = new int[]{
//            Sensor.TYPE_LINEAR_ACCELERATION
            Sensor.TYPE_LINEAR_ACCELERATION,
            Sensor.TYPE_ACCELEROMETER,
            Sensor.TYPE_GYROSCOPE,
            Sensor.TYPE_LIGHT
    };

    private PowerManager.WakeLock wakeLock;
    private AccelerometerListener accelerometerListener;
    private SensorManager sensorManager;
    private CapabilityClient capabilityClient;
    private String accelerometerNodeId;
    private ChannelClient channelClient;
    private ForegroundNotificationCreator fgNotificationCreator;
    private HandlerThread sensorThread;
    private SharedPreferences.Editor edit;
    private SharedPreferences sharedPreferences;
    private ChannelClient.Channel channel;

    private int messageCount = 0;
    private int messageCountTotal = 0;
    private long millisElapsed = 0;
    private long lastUpdate = -1;
    private long lastMessage;

    private final Handler statHandler = new Handler();
    /**
     * <p>
     * Calculates the current statistics for the sensor recording session. Invoked through an instance of {@link Handler},
     * posts itself at the end of the method to run repeatedly every {@link SensorDataService#DELAY_MILLIS} ms.
     * </p>
     * <p>
     * The current timestamp is used with every run to check the time elapsed since the last update to accumulate
     * the total time running. Messages per second are then calculated by dividing the total time by the message count.
     * Total message count and message count for the last interval are updated in the
     * {@link AccelerometerListener#onSensorChanged(SensorEvent)} callback. The message count for the last interval
     * is set back to 0 during each runnable execution.
     * </p>
     * <p>
     * {@link Handler#postDelayed(Runnable, long)} does not guarantee that the runnable is executed exactly in the
     * specified interval. To make sure the runnable is executed on full seconds, the offset from the next full second
     * is calculated and subtracted from {@code DELAY_MILLIS} so that the next timer runs at a full second again.
     * </p>
     */
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

    /**
     * Broadcast the statistics for the UI with {@link LocalBroadcastManager}. The statistics sent to the UI are
     * average message count per second (rate) and message count for the last interval.
     *
     * @param count message count for the last interval
     * @param value average message count per second (rate)
     */
    private void sendBroadcastUpdate(int count, float value) {
        Intent intent = new Intent();
        intent.setAction(UPDATED_ANALYTICS_INTENT_ACTION);
        intent.putExtra(LAST_SECOND_INTENT_EXTRA, count);
        intent.putExtra(RECORDS_PER_SECOND_INTENT_EXTRA, value);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    /**
     * Initializes variables needed by other callbacks when the service is created.
     */
    @Override
    public void onCreate() {
        super.onCreate();
        sharedPreferences = getSharedPreferences(getPackageName(), MODE_PRIVATE);
        edit = sharedPreferences.edit();
        edit.putBoolean("running", false).apply();
        this.fgNotificationCreator = Objects.requireNonNull((CustomApplication) getApplication())
                .getContext().getForegroundNotificationCreator();

        Log.i(TAG, "Creating sensordataservice");

        sensorManager = Objects.requireNonNull((SensorManager)
                getApplicationContext().getSystemService(SENSOR_SERVICE));
        capabilityClient = Wearable.getCapabilityClient(this);
    }

    /**
     * <p>
     * Starts the service as a foreground service.
     * </p>
     * <p>
     * {@code running} is set to true in {@link SharedPreferences}.
     * </p>
     * A wake lock is acquired to make sure the service
     *
     * @param intent intent for the start command
     * @param flags flags for the start command
     * @param startId start id for the start command
     * @return
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "Starting sensor data service.");
        edit.putBoolean("running", true).apply();
        wakeLock = Objects.requireNonNull((PowerManager) getApplicationContext()
                .getSystemService(Context.POWER_SERVICE))
                .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WEAR_WAKELOCKTAG);

        if (!wakeLock.isHeld()) {
            wakeLock.acquire();
        }

        startForeground(fgNotificationCreator.getId(), fgNotificationCreator.getNotification());

        capabilityClient
                .getCapability(ACCELEROMETER_RECEIVER_CAPABILITY, CapabilityClient.FILTER_REACHABLE)
                .addOnSuccessListener(this::applyCapability);

        return START_STICKY;
    }

    public void applyCapability(CapabilityInfo capabilityInfo) {
        Set<Node> connectedNodes = capabilityInfo.getNodes();
        Log.i(TAG, "Updating capability: " + capabilityInfo.getName() + ", nodes found: " + connectedNodes.size());
        accelerometerNodeId = pickBestNodeId(connectedNodes);
        Log.i(TAG, "AccelerometerNodeId is now " + accelerometerNodeId);

        if (sharedPreferences.getBoolean("running", false)) {
            if (accelerometerNodeId != null) {
                sensorManager.unregisterListener(accelerometerListener);
                openChannel();
            } else {
                // TODO: listeners are also registered when no node is connected and no channelOutput is avaiable.
                //  this is probably just useful for debugging/development purposes.
                registerSensorListeners(null, null);
            }
        }
    }

    @Override
    public void onCapabilityChanged(CapabilityInfo capabilityInfo) {
        Log.i(TAG, "capability changed");
        applyCapability(capabilityInfo);
    }

    private void openChannel() {
        Log.i(TAG, "Opening channel to node id " + accelerometerNodeId);
        channelClient = Wearable.getChannelClient(getApplicationContext());
        channelClient.openChannel(accelerometerNodeId, "/accelerometer_data")
                .addOnSuccessListener(this::setupChannelConnection);
    }

    public void setupChannelConnection(ChannelClient.Channel channel) {
        this.channel = channel;
        channelClient.getOutputStream(channel).addOnCompleteListener(command -> {
            boolean withOutputStream = command.isSuccessful();
            OutputStream result = null;
            String msg = null;
            if (withOutputStream) {
               result = command.getResult();
            } else {
                msg = command.getException().getCause().toString();
            }
            registerSensorListeners(result, msg);
        });
    }

    private void registerSensorListeners(OutputStream channelOutput, String err) {
        Log.d(TAG, "Instantiate Listenere");
        if (channelOutput != null) {
            if (accelerometerListener != null) {
                Log.d(TAG, "listener not null. set new outputstream");
                accelerometerListener.channelOutput = channelOutput;
            } else {
                Log.d(TAG, "listener is null. instantiate with outputstream");
                accelerometerListener = new AccelerometerListener(channelOutput);
            }
            edit.putBoolean("mobile_connected", true);
        } else {
            if (err != null) {
                Log.e(TAG, err);
            }

            if (accelerometerListener == null) {
                Log.d(TAG, "Intantiating Listener without Output Stream.");
                accelerometerListener = new AccelerometerListener();
            }
            edit.putBoolean("mobile_connected", false);
        }

        edit.apply();

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
        edit.putBoolean("running", false).apply();
        cleanup(accelerometerListener);

        Optional.ofNullable(channel).ifPresent(channel -> channelClient.close(channel));
        getSharedPreferences(getPackageName(), MODE_PRIVATE).edit().remove("mobile_connected").apply();
//        getSharedPreferences(getPackageName(), MODE_PRIVATE).edit().putBoolean("mobile_connected", false).apply();
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

        public AccelerometerListener() {

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

                        if (sharedPreferences.getBoolean("running", false)) {
                            Log.i(TAG, "Still running, reopening channel.");
                            openChannel();
                        }
                    }
                } else {
//                    Log.i(TAG, "channelOutput is null");
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
