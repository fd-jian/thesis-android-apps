package com.example.mywatch;

import android.annotation.SuppressLint;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.PowerManager;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.wearable.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class MainActivity extends WearableActivity {

    private static final String ACCELEROMETER_RECEIVER_CAPABILITY = "accelerometer_receiver";
    private static final String ACCELEROMETER_MESSAGE_PATH = "/accelerometer_data";
    private static final String TAG = MainActivity.class.getSimpleName();

    private TextView mTextView;
    private float[] acc = {0.0F, 0.0F, 0.0F};

    private String accelerometerNodeId = null;

    private Handler handler;
    private Runnable updateUi;

    private PowerManager.WakeLock mWakeLock;
    private int messageCounter = 0;
    private long lastMessageReceived = 0;
    private long secondsElapsed = 0;
    private long prevTimestamp = Instant.now().getEpochSecond();
    private float messagesPerSecond = 0F;

    private FileOutputStream fileOutputStream;

    @SuppressLint("InvalidWakeLockTag")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_main);
//        mTextView = (TextView) findViewById(R.id.text);
        LinearLayout linearLayout = new LinearLayout(this);
        setContentView(linearLayout);
        linearLayout.setOrientation(LinearLayout.VERTICAL);

        TextView accelerometerDataView = new TextView(this);
        accelerometerDataView.setText("Waiting for data to be received...");
        linearLayout.addView(accelerometerDataView);

        TextView messageCountView = new TextView(this);
        linearLayout.addView(messageCountView);

        TextView secondsElapsedView = new TextView(this);
        linearLayout.addView(secondsElapsedView);

        TextView messagesPerSecondView = new TextView(this);
        linearLayout.addView(messagesPerSecondView);

        // Enables Always-on
        setAmbientEnabled();

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

        SensorEventListener listener = new SensorEventListener() {

            @Override
            public void onSensorChanged(SensorEvent event) {
                // store accelerometer data in variable for later use, just for now.
                acc = event.values;
                sendMessage(acc);

                try {
                    fileOutputStream.write((Arrays.toString(acc).replaceAll("[\\[\\] ]", "") + "\n").getBytes());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                ++messageCounter;
                lastMessageReceived = Instant.now().toEpochMilli();
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
                Log.w(TAG, String.format("Accuracy changed to %d", accuracy));
            }

        };
        sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_FASTEST);

        // todo: on stop do:
//        sensorManager.unregisterListener(listener);

        //  Update UI with accelerometer data and send it to handheld device every 2 seconds
        handler = new Handler();
        int delay = 500;
        updateUi = new Runnable() {
            public void run() {
                long newTimestamp = Instant.now().toEpochMilli();
                secondsElapsed = newTimestamp - prevTimestamp;
                messagesPerSecond = secondsElapsed != 0 ? (float) messageCounter / (float) secondsElapsed : 0F;

                if (newTimestamp - lastMessageReceived > 5000) {
                    secondsElapsed = 0;
                    messageCounter = 0;
                    prevTimestamp = newTimestamp;
                }

                setAmbientEnabled();
                accelerometerDataView.setText(IntStream.range(0, acc.length)
                        .mapToObj(value -> acc[value])
                        .map(aFloat -> (float) Math.round(aFloat * 1000) / 1000)
                        .map(Object::toString)
                        .collect(Collectors.joining("; ")));

                @SuppressLint("DefaultLocale") String messageCount = String.format("%d messages received",
                        messageCounter);
//                Log.w(TAG, messageCount);
                messageCountView.setText(messageCount);

                long secEl = secondsElapsed;
                @SuppressLint("DefaultLocale") String secondsElapsed = String.format("%d seconds elapsed",
                        secEl);
//                Log.w(TAG, secondsElapsed);
                secondsElapsedView.setText(secondsElapsed);

                float messPerSec = messagesPerSecond;
                @SuppressLint("DefaultLocale") String messagesPerSecond = String.format("%.2f messages/second",
                        messPerSec);
//                Log.w(TAG, messagesPerSecond);
                messagesPerSecondView.setText(messagesPerSecond);

                handler.postDelayed(this, delay);
            }
        };
        handler.postDelayed(updateUi, delay);

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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            fileOutputStream.flush();
            fileOutputStream.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        handler.removeCallbacks(updateUi);
    }
}
