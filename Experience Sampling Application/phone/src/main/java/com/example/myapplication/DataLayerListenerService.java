package com.example.myapplication;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.PowerManager;
import android.util.Log;
import com.google.android.gms.wearable.ChannelClient;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;

public class DataLayerListenerService extends WearableListenerService {
    public static final String ACCELEROMETER = "ACCELEROMETER";
    public static final String MESSAGE_COUNT = "MESSAGE_COUNT";
    public static final String SECONDS_ELAPSED = "SECONDS_ELAPSED";
    public static final String MESSAGES_PER_SECOND = "MESSAGES_PER_SECOND";

    private static final String WEAR_WAKELOCKTAG = "wear:wakelocktag";
    private static final String TAG = "wear:" + DataLayerListenerService.class.getSimpleName();
    private float[] cachedAccData = {0, 0, 0};
    private int messageCounter = 0;
    private long timestampSeconds = Instant.now().getEpochSecond();
    private long lastMessageReceived;

    private MqttService mqttService;
    private Handler handler = new Handler();

    private static final int BROADCAST_INTERVAL = 500;
    private Runnable sendBroadcastMessage = new Runnable() {
        public void run() {

            long newTimestamp = Instant.now().getEpochSecond();
            long secondsElapsed = newTimestamp - timestampSeconds;
            float messagesPerSecond = secondsElapsed != 0 ? (float) messageCounter / (float) secondsElapsed : 0F;

            if (newTimestamp - lastMessageReceived > 5) {
                secondsElapsed = 0;
                messageCounter = 0;
                timestampSeconds = newTimestamp;
            }

            Log.v(TAG, "sending broadcast message for ui");
            Intent local = new Intent();
            local.setAction("com.hello.action");
            local.putExtra(ACCELEROMETER, Arrays.toString(cachedAccData));
            local.putExtra(MESSAGE_COUNT, Integer.toString(messageCounter));
            local.putExtra(SECONDS_ELAPSED, Long.toString(secondsElapsed));
            local.putExtra(MESSAGES_PER_SECOND, Float.toString(messagesPerSecond));
            sendBroadcast(local);

            handler.postDelayed(this, BROADCAST_INTERVAL);
        }
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "on start command");

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
                .build());

        mqttService.connect();

        //  Update UI with accelerometer data and send it to handheld device every 2 seconds
        handler.postDelayed(sendBroadcastMessage, BROADCAST_INTERVAL);

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(sendBroadcastMessage);
        this.mqttService.disconnect();
        stopForeground(true);
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        byte[] data = messageEvent.getData();

        handleRecord(data);
    }

    private void handleRecord(byte[] data) {
        float[] floats = getFloats(data);

        cachedAccData = floats;
        ++messageCounter;
        Instant now = Instant.now();
        lastMessageReceived = now.getEpochSecond();

        if (!mqttService.isConnected()) {
            Log.d(TAG, "mqtt broker is not connected");
            return;
        }

        JSONObject sample = new JSONObject();
        try {
            sample.put("time", now.toEpochMilli());
            sample.put("x", floats[0]);
            sample.put("y", floats[1]);
            sample.put("z", floats[2]);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        String message = sample.toString();
        Log.v(TAG, String.format("sending MQTT message with payload: %s", message));
        mqttService.sendMessage("123", message.getBytes());
    }

    private float[] getFloats(byte[] data) {
        float[] floats = new float[3];
        byte[] tempChunk = new byte[4];
        for (int i = 0; i < 12; i += 4) {
            System.arraycopy(data, i, tempChunk, 0, 4);
            floats[i / 4] = ByteBuffer.wrap(tempChunk).getFloat();
        }
        return floats;
    }

    @Override
    public void onChannelOpened(ChannelClient.Channel channel) {
        Log.d(TAG, "channel opened!");

        Wearable.getChannelClient(this)
                .getInputStream(channel)
                .addOnSuccessListener(command -> {

                    // without new thread, UI hangs. but why? this is supposed to run on a separate thread!
                    // TODO: consider finding a more elegant solution
                    new Thread(() -> {
                        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
                        Log.v(TAG, "input stream retrieved succesfully. Reading in new thread.");
                        try {
                            readStream(command);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }).start();
                });
    }

    private void readStream(InputStream command) throws IOException {
        byte[] data = new byte[12];
        ByteArrayOutputStream tempBaos = new ByteArrayOutputStream(data.length);
        int c;
        Log.d(TAG, "start reading input stream");
        while ((c = command.read(data, 0, data.length)) != -1) {
            tempBaos.write(data, 0, c);
            handleRecord(tempBaos.toByteArray());
            tempBaos.reset();
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        this.mqttService = ((CustomApplication) getApplication()).getContext().getMqttService();

        if (mqttService == null) {
            throw new RuntimeException();
        }

    }
}
