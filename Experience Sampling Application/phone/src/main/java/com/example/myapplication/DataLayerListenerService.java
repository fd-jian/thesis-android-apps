package com.example.myapplication;

import android.content.Intent;
import android.os.Handler;
import android.util.Log;
import com.google.android.gms.wearable.*;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class DataLayerListenerService extends WearableListenerService {
    private static final String TAG = DataLayerListenerService.class.getSimpleName();
    private static final String ACCELEROMETER_MESSAGE_PATH = "/accelerometer_data";
    public static final String ACCELEROMETER = "ACCELEROMETER";
    public static final String MESSAGE_COUNT = "MESSAGE_COUNT";
    public static final String SECONDS_ELAPSED = "SECONDS_ELAPSED";
    public static final String MESSAGES_PER_SECOND = "MESSAGES_PER_SECOND";
    private static final String LOG = DataLayerListenerService.class.getSimpleName();
    private float[] cachedAccData = { 0, 0, 0 };
    private int messageCounter = 0;
    private long timestampSeconds = Instant.now().getEpochSecond();
    private long lastMessageReceived;

    private Handler handler = new Handler();

    private int delay = 500;
    private Runnable sendBroadcastMessage = new Runnable() {
        public void run() {

            long newTimestamp = Instant.now().getEpochSecond();
            long secondsElapsed = newTimestamp - timestampSeconds;
            float messagesPerSecond = secondsElapsed != 0 ? (float) messageCounter / (float) secondsElapsed : 0F;

            if(newTimestamp - lastMessageReceived > 5) {
                secondsElapsed = 0;
                messageCounter = 0;
                timestampSeconds = newTimestamp;
            }

            Log.d(TAG, "sending broadcast message for ui");
            Intent local = new Intent();
            local.setAction("com.hello.action");
            local.putExtra(ACCELEROMETER, Arrays.toString(cachedAccData));
            local.putExtra(MESSAGE_COUNT, Integer.toString(messageCounter));
            local.putExtra(SECONDS_ELAPSED, Long.toString(secondsElapsed));
            local.putExtra(MESSAGES_PER_SECOND, Float.toString(messagesPerSecond));
            sendBroadcast(local);

            handler.postDelayed(this, delay);
        }
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "on start command");
        //  Update UI with accelerometer data and send it to handheld device every 2 seconds
        handler.postDelayed(sendBroadcastMessage, delay);

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(sendBroadcastMessage);
        super.onDestroy();
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {

        byte[] data = messageEvent.getData();

        float[] floats = new float[3];
        byte[] tempChunk = new byte[4];
        for (int i = 0; i < 12; i+=4) {
            System.arraycopy(data, i, tempChunk, 0, 4);
            floats[i/4] = ByteBuffer.wrap(tempChunk).getFloat();
        }

        cachedAccData = floats;
        ++messageCounter;
        lastMessageReceived = Instant.now().getEpochSecond();

//        String topic        = "MQTT Examples";
//        String content      = "Message from MqttPublishSample";
//        int qos             = 2;
//        String broker       = "tcp://mqtt.eclipse.org:1883";
//        String clientId     = "JavaSample";
//        MemoryPersistence persistence = new MemoryPersistence();
//
//        try {
//            MqttClient sampleClient = new MqttClient(broker, clientId, persistence);
//            MqttConnectOptions connOpts = new MqttConnectOptions();
//            connOpts.setCleanSession(true);
//            System.out.println("Connecting to broker: "+broker);
//            sampleClient.connect(connOpts);
//            System.out.println("Connected");
//            System.out.println("Publishing message: "+content);
//            MqttMessage message = new MqttMessage(content.getBytes());
//            message.setQos(qos);
//            sampleClient.publish(topic, message);
//            System.out.println("Message published");
//            sampleClient.disconnect();
//            System.out.println("Disconnected");
//            System.exit(0);
//        } catch(MqttException me) {
//            System.out.println("reason "+me.getReasonCode());
//            System.out.println("msg "+me.getMessage());
//            System.out.println("loc "+me.getLocalizedMessage());
//            System.out.println("cause "+me.getCause());
//            System.out.println("excep "+me);
//            me.printStackTrace();
//        }

    }

}
