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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAccessor;
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
    private long timestampSeconds = Instant.now().getEpochSecond();
    private float messagesPerSecond = 0F;

    private FileOutputStream fileOutputStream;

    @SuppressLint("InvalidWakeLockTag")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_main);

//        mTextView = (TextView) findViewById(R.id.text);
//

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

        this.setupAccelerometerTransmission();

        // TODO: on app start, screen is inactive and this still does not work without tapping the screen.
        //  unclear if wakelock even makes a difference....
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "My Tag");
        mWakeLock.acquire();

        SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);

        Date date = new Date();
        ZonedDateTime now = ZonedDateTime.ofInstant(
                Instant.ofEpochMilli(date.getTime()), ZoneId.systemDefault());

        File externalFilesDir = getApplicationContext()
                .getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);

        // Cleanup old files
        PathMatcher pathMatcher = FileSystems.getDefault().getPathMatcher(
                String.format(
                        "regex:.*%s(?!%s).*",
                        new SimpleDateFormat("yyyy-MM-", Locale.getDefault()).format(date),
                        now.getDayOfMonth()));

        try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(
                Paths.get(externalFilesDir.getPath()), pathMatcher::matches)) {
            dirStream.forEach(path -> {
                Log.w(TAG, String.format("Deleting %s", path.toString()));
                path.toFile().delete();
            });
        } catch (IOException e) {
            throw new RuntimeException();
        }

        String fileName = String.format("accelero_dump%s",
                new SimpleDateFormat("yyyy-MM-dd'T'hh-mm-ss", Locale.getDefault()).format(date));
        File file = new File(externalFilesDir, fileName);

        try {
            file.createNewFile();
            fileOutputStream = new FileOutputStream(file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        String absolutePath = file.getAbsolutePath();

        Log.w(TAG, String.format("Created FileOutputStream for file at path: %s", absolutePath));

        sensorManager.registerListener(new SensorEventListener() {

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
                lastMessageReceived = Instant.now().getEpochSecond();
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
                Log.w(TAG, String.format("Accuracy changed to %d", accuracy));
            }

        }, accelerometer, SensorManager.SENSOR_DELAY_FASTEST);

        //  Update UI with accelerometer data and send it to handheld device every 2 seconds
        handler = new Handler();
        int delay = 500;
        updateUi = new Runnable() {
            public void run() {
                long newTimestamp = Instant.now().getEpochSecond();
                secondsElapsed = newTimestamp - timestampSeconds;
                messagesPerSecond = secondsElapsed != 0 ? (float) messageCounter / (float) secondsElapsed : 0F;

                if (newTimestamp - lastMessageReceived > 5) {
                    secondsElapsed = 0;
                    messageCounter = 0;
                    timestampSeconds = newTimestamp;
                }

                accelerometerDataView.setText(IntStream.range(0, acc.length)
                        .mapToObj(value -> acc[value])
                        .map(aFloat -> (float) Math.round(aFloat * 1000) / 1000)
                        .map(Object::toString)
                        .collect(Collectors.joining("; ")));
                handler.postDelayed(this, delay);

                String messageCount = String.format("%d messages received",
                        messageCounter);
//                Log.w(TAG, messageCount);
                messageCountView.setText(messageCount);

                long secEl = secondsElapsed;
                String secondsElapsed = String.format("%d seconds elapsed",
                        secEl);
//                Log.w(TAG, secondsElapsed);
                secondsElapsedView.setText(secondsElapsed);

                float messPerSec = messagesPerSecond;
                String messagesPerSecond = String.format("%.2f messages/second",
                        messPerSec);
//                Log.w(TAG, messagesPerSecond);
                messagesPerSecondView.setText(messagesPerSecond);
            }
        };
        handler.postDelayed(updateUi, delay);

    }

    private void sendMessage(float[] acc) {
        Wearable.getMessageClient(this).sendMessage(accelerometerNodeId, ACCELEROMETER_MESSAGE_PATH, getAccBytes(acc));
    }

    private byte[] getAccBytes(float[] accelerometerValues) {
        byte[] res = new byte[12];
        for (int i = 0; i < accelerometerValues.length; i++) {
            byte[] buff = ByteBuffer.allocate(4).putFloat(accelerometerValues[i]).array();
            for (int j = 0; j < buff.length; j++) {
                res[i * 4 + j] = buff[j];
            }
        }
        return res;
    }

    private Task<CapabilityInfo> setupAccelerometerTransmission() {
        CapabilityClient capabilityClient = Wearable.getCapabilityClient(this);

        capabilityClient.addListener(this::updateAccelerometerCapability, ACCELEROMETER_RECEIVER_CAPABILITY);

        return capabilityClient.getCapability(ACCELEROMETER_RECEIVER_CAPABILITY, CapabilityClient.FILTER_REACHABLE)
                .addOnSuccessListener(this::updateAccelerometerCapability);
    }

    private void updateAccelerometerCapability(CapabilityInfo capabilityInfo) {
        Set<Node> connectedNodes = capabilityInfo.getNodes();
        Log.w(TAG, "Updating capability: " + capabilityInfo.getName() + ", nodes found: " + connectedNodes.size());
        updateNodeId(connectedNodes);
    }

    private void updateNodeId(Set<Node> connectedNodes) {
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
