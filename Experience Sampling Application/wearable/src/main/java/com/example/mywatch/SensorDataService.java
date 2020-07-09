package com.example.mywatch;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Environment;
import android.os.PowerManager;
import android.util.Log;
import androidx.annotation.Nullable;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.wearable.CapabilityClient;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.Wearable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class SensorDataService extends IntentService {


    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     * @param name Used to name the worker thread, important only for debugging.
     */
    public SensorDataService(String name) {
        super(name);
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        // TODO: on app start, screen is inactive and this still does not work without tapping the screen.
        //  unclear if wakelock even makes a difference....
        Optional.ofNullable((PowerManager) getSystemService(Context.POWER_SERVICE))
                .map(pm -> pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "My Tag"))
                .ifPresent(PowerManager.WakeLock::acquire);

        this.setupWearableDataTransmission().addOnSuccessListener(command -> {
            // TODO: maybe rest here?
        });

        setupFileLogging();

        SensorManager sensorManager = Optional
                .ofNullable((SensorManager) getSystemService(SENSOR_SERVICE))
                .orElseThrow(() -> new RuntimeException(String.format(
                        "Service '%s' not found.", SENSOR_SERVICE)));

        Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);


    }

    private void setupFileLogging() {
        Instant date = Instant.now();
        ZonedDateTime now = ZonedDateTime.ofInstant(
                date, ZoneId.systemDefault());

        File externalFilesDir = Optional.ofNullable(getApplicationContext()
                .getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS))
                .orElseThrow(() -> new RuntimeException("Local Download directory not found/inaccessible."));

        // Cleanup old files
        PathMatcher pathMatcher = FileSystems.getDefault().getPathMatcher(
                String.format(
                        "regex:.*%s(?!%s).*",
                        DateTimeFormatter.ofPattern("yyyy-MM-").format(date),
                        now.getDayOfMonth()));

        try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(
                Paths.get(externalFilesDir.getPath()), pathMatcher::matches)) {
            dirStream.forEach(path -> {
                Log.w(TAG, String.format("Deleting %s", path.toString()));
                if(!path.toFile().delete()){
                    Log.w(TAG, "Failed to delete old log file.");
                }
            });
        } catch (IOException e) {
            throw new RuntimeException();
        }

        String fileName = String.format("accelero_dump%s",
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'hh-mm-ss").format(date));
        File file = new File(externalFilesDir, fileName);

        try {
            if(!file.createNewFile()) {
                Log.w(TAG, String.format(
                        "Log file '%s' already exists. Appending new log output to it.",
                        file.getAbsolutePath()));
            };
            fileOutputStream = new FileOutputStream(file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        String absolutePath = file.getAbsolutePath();

        Log.w(TAG, String.format("Created FileOutputStream for file at path: %s", absolutePath));
    }

    private void sendMessage(float[] acc) {
        Wearable.getMessageClient(this)
                .sendMessage(accelerometerNodeId,
                        ACCELEROMETER_MESSAGE_PATH, getSensorBytes(acc));
    }

    private byte[] getSensorBytes(float[] accelerometerValues) {
        byte[] res = new byte[12];
        for (int i = 0; i < accelerometerValues.length; i++) {
            byte[] buff = ByteBuffer.allocate(4).putFloat(accelerometerValues[i]).array();
            System.arraycopy(buff, 0, res, i * 4, buff.length);
        }
        return res;
    }

    private Task<CapabilityInfo> setupWearableDataTransmission() {
        CapabilityClient capabilityClient = Wearable.getCapabilityClient(this);
        // listener to update node config on changes to ACCELEROMETER_RECEIVER_CAPABILITY
        capabilityClient.addListener(this::updateNodeConfig, ACCELEROMETER_RECEIVER_CAPABILITY);
        // get current ACCELEROMETER_RECEIVER_CAPABILITY and update node config
        return capabilityClient.getCapability(ACCELEROMETER_RECEIVER_CAPABILITY, CapabilityClient.FILTER_REACHABLE)
                .addOnSuccessListener(this::updateNodeConfig);
    }

    private void updateNodeConfig(CapabilityInfo capabilityInfo) {
        Set<Node> connectedNodes = capabilityInfo.getNodes();
        Log.w(TAG, "Updating capability: " + capabilityInfo.getName() + ", nodes found: " + connectedNodes.size());
        accelerometerNodeId = pickBestNodeId(connectedNodes);
        Log.w(TAG, "accelerometerNodeId is now " + accelerometerNodeId);
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
