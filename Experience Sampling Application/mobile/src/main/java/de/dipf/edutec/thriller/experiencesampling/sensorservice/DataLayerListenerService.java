package de.dipf.edutec.thriller.experiencesampling.sensorservice;

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
import de.dipf.edutec.thriller.experiencesampling.conf.CustomApplication;
import de.dipf.edutec.thriller.experiencesampling.sensorservice.transport.MqttService;
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

    private static final String WEAR_WAKELOCKTAG = "wear:wakelocktag";
    private static final String TAG = "wear:" + DataLayerListenerService.class.getSimpleName();

    private MqttService mqttService;

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

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
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

        Instant now = Instant.now();

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
