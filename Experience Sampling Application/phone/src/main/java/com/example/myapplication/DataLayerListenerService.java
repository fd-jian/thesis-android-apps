package com.example.myapplication;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.PowerManager;
import android.util.Log;
import com.google.android.gms.wearable.*;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.json.JSONException;
import org.json.JSONObject;

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
//                .setSmallIcon(...)
//                .setTicker(...)
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

        float[] floats = new float[3];
        byte[] tempChunk = new byte[4];
        for (int i = 0; i < 12; i += 4) {
            System.arraycopy(data, i, tempChunk, 0, 4);
            floats[i / 4] = ByteBuffer.wrap(tempChunk).getFloat();
        }

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

        mqttService.sendMessage("123", sample.toString().getBytes());
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
