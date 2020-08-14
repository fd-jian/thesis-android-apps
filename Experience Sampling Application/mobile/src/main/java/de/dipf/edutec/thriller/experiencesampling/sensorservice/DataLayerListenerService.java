package de.dipf.edutec.thriller.experiencesampling.sensorservice;

import android.content.Intent;
import android.hardware.Sensor;
import android.os.*;
import android.os.Process;
import android.util.Log;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import com.google.android.gms.wearable.ChannelClient;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;
import de.dipf.edutec.thriller.experiencesampling.conf.CustomApplication;
import de.dipf.edutec.thriller.experiencesampling.conf.Globals;
import de.dipf.edutec.thriller.experiencesampling.sensorservice.transport.MqttService;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static de.dipf.edutec.thriller.experiencesampling.conf.Globals.loginScreenActive;

public class DataLayerListenerService extends WearableListenerService {

    public static final String LAST_SECOND_INTENT_EXTRA = "records-per-second";
    public static final String UPDATED_ANALYTICS_INTENT_ACTION = "updated-analytics";
    public static final String RECORDS_PER_SECOND_INTENT_EXTRA = "records-last-second";
    public static final int DELAY_MILLIS = 1000;

    private static final String TAG = "wear:" + DataLayerListenerService.class.getSimpleName();
    private static final Map<Integer, String> sensorTopicNames;

    static {
        sensorTopicNames = new HashMap<>();
        sensorTopicNames.put(
                Sensor.TYPE_LINEAR_ACCELERATION,
                getSensorSimpleName(Sensor.STRING_TYPE_LINEAR_ACCELERATION));
        sensorTopicNames.put(
                Sensor.TYPE_ACCELEROMETER,
                getSensorSimpleName(Sensor.STRING_TYPE_ACCELEROMETER));
        sensorTopicNames.put(
                Sensor.TYPE_GYROSCOPE,
                getSensorSimpleName(Sensor.STRING_TYPE_GYROSCOPE));
        sensorTopicNames.put(
                Sensor.TYPE_LIGHT,
                getSensorSimpleName(Sensor.STRING_TYPE_LIGHT));
//        sensorTopicNames.put(
//                Sensor.TYPE_ACCELEROMETER,
//                getSensorSimpleName(Sensor.STRING_TYPE_LINEAR_ACCELERATION));
//        sensorTopicNames.put(
//                Sensor.TYPE_GYROSCOPE,
//                getSensorSimpleName(Sensor.STRING_TYPE_LINEAR_ACCELERATION));
//        sensorTopicNames.put(
//                Sensor.TYPE_LIGHT,
//                getSensorSimpleName(Sensor.STRING_TYPE_LINEAR_ACCELERATION));
    }

    private MqttService mqttService;
    private ChannelClient.Channel channel;
    private HandlerThread readByteDataThread;

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
    public void onChannelOpened(ChannelClient.Channel channel) {
        Log.i(TAG, "channel opened!");
        this.channel = channel;
        this.readByteDataThread = new HandlerThread("Read byte data", Process.THREAD_PRIORITY_MORE_FAVORABLE);
        this.readByteDataThread.start();

        // TODO: generate new session uuid

        AccountConnector.connect(this, true, false, !loginScreenActive, mqttService);

        Wearable.getChannelClient(this)
                .getInputStream(channel)
                .addOnSuccessListener(this::startProcessing);
    }

    public void startProcessing(InputStream command) {
        // without new thread, UI hangs. but why? this is supposed to run on a separate thread!
        new Handler(readByteDataThread.getLooper()).post(() -> {
            Log.i(TAG, "Input stream for channel " + channel.getPath() + " retrieved succesfully. Reading in new thread.");
            statHandler.removeCallbacks(stats);
            statHandler.postDelayed(stats, DELAY_MILLIS);
            try {
                readStream(command);
            } catch (IOException ignored) { // ignored because finally handles stream closing
                Log.i(TAG, "Exception reading channel stream: " + ignored.getMessage());
            } finally {
                Log.d(TAG, "reading stream done, cleaning up");
                try {
                    command.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                statHandler.removeCallbacks(stats);
                readByteDataThread.quitSafely();
            }
        });
    }

    private void readStream(InputStream command) throws IOException {
        // 24 byte max length of the values array between all sensor types (6 float numbers).
        // + 8 byte timestamp + 4 byte sensor type + 4 byte length value
        byte[] data = new byte[40];
        ByteArrayOutputStream tempBaos = new ByteArrayOutputStream(data.length);
        int c;
        Log.i(TAG, "Start reading channel input stream");
        while ((c = command.read(data, 0, data.length)) != -1) {
            tempBaos.write(data, 0, c);
            handleRecord(tempBaos.toByteArray());
            tempBaos.reset();
        }
    }

    private void handleRecord(byte[] data) {
        updateCount(Instant.now().toEpochMilli());

        ByteBuffer buf = ByteBuffer.wrap(data);
        long timestamp = buf.getLong();
        int sensorType = buf.getInt();
        String sensorName = sensorTopicNames.get(sensorType);
        int payloadLength = buf.getInt();
        float[] floats = getFloats(buf, payloadLength);

        if (!mqttService.isConnected()) {
            return;
        }

        if (sensorName == null) {
            Log.e(TAG, String.format("Topic name not defined for sensor type %s", sensorType));
            return;
        }

        JSONObject sample = new JSONObject();
        Instant now = Instant.ofEpochMilli(timestamp);
        try {
            sample.put("time", now.toEpochMilli());
            sample.put("values", new JSONArray(floats));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // TODO: use session id instead of 123
        String topic = String.format("sensors/%s/123", sensorName);
        String message = sample.toString();

        mqttService.sendMessage(topic, message.getBytes());
    }

    private void updateCount(long now) {
        lastMessage = now;
        messageCount++;
        messageCountTotal++;
    }

    private static String getSensorSimpleName(String sensorType) {
        return Optional.of(sensorType.split("\\."))
                .map(strings -> strings[strings.length - 1])
                .orElse(null);
    }

    private float[] getFloats(ByteBuffer buf, int payloadLength) {
        float[] floats = new float[payloadLength];
        for (int i = 0; i < payloadLength; i++) {
            floats[i] = buf.getFloat();
        }
        return floats;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        this.mqttService = ((CustomApplication) getApplication()).getContext().getMqttService();
    }


}
