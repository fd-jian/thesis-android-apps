package de.dipf.edutec.thriller.experiencesampling.sensorservice;

import android.hardware.Sensor;
import android.util.Log;
import com.google.android.gms.wearable.ChannelClient;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;
import de.dipf.edutec.thriller.experiencesampling.conf.CustomApplication;
import de.dipf.edutec.thriller.experiencesampling.sensorservice.transport.MqttService;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class DataLayerListenerService extends WearableListenerService {

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
    }

    private MqttService mqttService;

    @Override
    public void onChannelOpened(ChannelClient.Channel channel) {
        Log.i(TAG, "channel opened!");

        if(!mqttService.isConnected()) {
            mqttService.connect();
        }

        Wearable.getChannelClient(this)
                .getInputStream(channel)
                .addOnSuccessListener(command -> {

                    // without new thread, UI hangs. but why? this is supposed to run on a separate thread!
                    // TODO: consider finding a more elegant solution
                    new Thread(() -> {
                        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
                        Log.i(TAG, "Input stream for channel " + channel.getPath() + " retrieved succesfully. Reading in new thread.");
                        try {
                            readStream(command);
                        } catch (IOException e) {
                            try {
                                command.close();
                            } catch (IOException ioException) {
                                ioException.printStackTrace();
                            }
                        }
                    }).start();
                });
    }

    private void handleRecord(byte[] data) {
        Log.v(TAG, "received record bytes: " + Arrays.toString(data));
        ByteBuffer buf = ByteBuffer.wrap(data);

        long timestamp = buf.getLong();
        Log.v(TAG, "Sensor record timestamp: " + timestamp);

        int sensorType = buf.getInt();
        Log.v(TAG, "Sensor type id: " + sensorType);

        String sensorName = sensorTopicNames.get(sensorType);
        Log.v(TAG, "Sensor name is " + sensorName);

        int payloadLength = buf.getInt();
        Log.v(TAG, "Sensor record payload length: " + payloadLength);

        float[] floats = getFloats(buf, payloadLength);
        Log.v(TAG, "converted payload to float array: " + Arrays.toString(floats));

        if (!mqttService.isConnected()) {
            Log.i(TAG, "mqtt broker is not connected");
            return;
        }

        if (sensorName == null) {
            Log.e(TAG, String.format("Topic name not defined for sensor type %s", sensorType));
            return;
        }

        // TODO: seems like new thread helps against blocking channel stream for json serialization and mqtt transmission? verify!
        new Thread(() -> {
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
            Log.v(TAG, String.format("sending MQTT message to topic %s with payload: %s", topic, message));

            mqttService.sendMessage(topic, message.getBytes());
        }).start();

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

    @Override
    public void onCreate() {
        super.onCreate();
        this.mqttService = ((CustomApplication) getApplication()).getContext().getMqttService();
    }
}
