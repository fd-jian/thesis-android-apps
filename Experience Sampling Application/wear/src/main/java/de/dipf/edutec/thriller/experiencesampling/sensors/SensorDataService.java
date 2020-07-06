package de.dipf.edutec.thriller.experiencesampling.sensors;

import android.app.*;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.wearable.CapabilityClient;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;
import de.dipf.edutec.thriller.experiencesampling.R;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;


public class SensorDataService extends Service {

    public static final String SENSOR_ACTION_EXTRA = "sensor_action";
    public static final String START_ACTION = "start";
    public static final String STOP_ACTION = "stop";

    public static final String WEAR_WAKELOCKTAG = "wear:wakelocktag";
    private static final String TAG = "wear:" + SensorDataService.class.getSimpleName();
    private static final String ACCELEROMETER_RECEIVER_CAPABILITY = "accelerometer_receiver";

//    private AccelerometerListener accelerometerListener;
//    private SensorManager sensorManager;
//    private CapabilityClient capabilityClient;

    private final Map<String, Runnable> actions = new HashMap<>();
    private final CapabilityClient.OnCapabilityChangedListener updateNodeConfig = this::updateNodeConfig;

    public SensorDataService() {
        actions.put(START_ACTION, this::startStreaming);
        actions.put(STOP_ACTION, this::stopStreaming);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Creating sensordataservice");

        Helper.sensorManager = Optional
                .ofNullable((SensorManager) getApplicationContext().getSystemService(SENSOR_SERVICE))
                .orElseThrow(() -> new RuntimeException(String.format(
                        "Service '%s' not found.", SENSOR_SERVICE)));

        Helper.capabilityClient = Wearable.getCapabilityClient(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Handling intent");

        Optional.ofNullable((PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE))
                .map(pm -> pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WEAR_WAKELOCKTAG))
                .ifPresent(PowerManager.WakeLock::acquire);

        Optional.ofNullable(getSystemService(NotificationManager.class))
                .orElseThrow(() -> new RuntimeException("could not find Notification manager."))
                .createNotificationChannel(
                        new NotificationChannel(
                                "f1",
                                "foreground",
                                NotificationManager.IMPORTANCE_LOW));

        startForeground(1337, new Notification.Builder(this, "f1")
                .setOngoing(true)
                .setContentTitle("streaming sensor data")
//                .setSmallIcon(...)
//                .setTicker(...)
                .build());


        startStreaming();

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "calling onDestroy");
        super.onDestroy();
        stopStreaming();
        stopForeground(true);
        LocalBroadcastManager.getInstance(this)
                .sendBroadcast(new Intent("sensor-service-destroyed"));
    }


//    @Override
//    protected void onHandleIntent(@Nullable Intent intent) {
//        Log.d(TAG,"Handling intent");
//        if (intent == null || !intent.hasExtra(SENSOR_ACTION_EXTRA)) {
//            Log.w(TAG, "Sensor action must be provided through Intent extra.");
//            return;
//        }
//
//        actions.get(intent.getStringExtra("sensor_action")).run();
//    }

    private void startStreaming() {
        SensorDataFileLogger sensorDataFileLogger = SensorDataFileLogger.create(getApplicationContext());
        Helper.accelerometerListener = new AccelerometerListener(
                Wearable.getMessageClient(this.getApplicationContext()), sensorDataFileLogger);

        if (Helper.accelerometerListener.getAccelerometerNodeId() != null) {
            registerListener();
        } else {
            this.setupWearableDataTransmission()
                    .addOnSuccessListener(command -> registerListener());
        }

    }

    private void stopStreaming() {
        Helper.sensorManager.unregisterListener(Helper.accelerometerListener, Helper.accelerometer);

        Optional.ofNullable(Helper.accelerometerListener)
                .ifPresent(AccelerometerListener::closeStream);

        Helper.accelerometerListener = null;

        Log.d(TAG, "Unregistered accelerometer listener.");
    }

    private void registerListener() {
        Helper.accelerometer = Optional.ofNullable(Helper.sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION))
                .orElseThrow(() -> new RuntimeException("Could not get default Sensor."));
        if (!Helper.sensorManager.registerListener(
                Helper.accelerometerListener,
                Helper.accelerometer,
                SensorManager.SENSOR_DELAY_FASTEST)) {
            throw new RuntimeException("Could not register accelerometer listener");
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        // TODO
        return null;
    }

    private Task<CapabilityInfo> setupWearableDataTransmission() {
        // listener to update node config on changes to ACCELEROMETER_RECEIVER_CAPABILITY
        Helper.capabilityClient.addListener(updateNodeConfig, ACCELEROMETER_RECEIVER_CAPABILITY);
        // get current ACCELEROMETER_RECEIVER_CAPABILITY and update node config
        return Helper.capabilityClient
                .getCapability(ACCELEROMETER_RECEIVER_CAPABILITY, CapabilityClient.FILTER_REACHABLE)
                .addOnSuccessListener(this::updateNodeConfig);
    }

    private void updateNodeConfig(CapabilityInfo capabilityInfo) {
        Set<Node> connectedNodes = capabilityInfo.getNodes();
        Log.d(TAG, "Updating capability: " + capabilityInfo.getName() + ", nodes found: " + connectedNodes.size());
        Helper.accelerometerListener.setAccelerometerNodeId(pickBestNodeId(connectedNodes));
        Log.d(TAG, "AccelerometerNodeId is now " + Helper.accelerometerListener.getAccelerometerNodeId());
    }

    private String pickBestNodeId(Set<Node> nodes) {
        String bestNodeId = null;
        // Find a nearby node or pick one arbitrarily
        for (Node node : nodes) {
            if (node.isNearby()) {
                return node.getId();
            }
            bestNodeId = node.getId();
        }
        return bestNodeId;
    }

}
