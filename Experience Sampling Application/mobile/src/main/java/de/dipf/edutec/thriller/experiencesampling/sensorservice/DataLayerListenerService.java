package de.dipf.edutec.thriller.experiencesampling.sensorservice;

import android.accounts.AccountManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.*;
import android.os.Handler;
import android.os.HandlerThread;
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
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static de.dipf.edutec.thriller.experiencesampling.conf.Globals.loginScreenActive;
import static de.dipf.edutec.thriller.experiencesampling.util.SharedPreferencesUtil.loadListPreference;

/**
 * <p>
 * Listens for data published through a {@link ChannelClient.Channel} by wearable nodes. Received data is published to
 * an MQTT broker in real-time.
 * </p>
 * <p>
 * The service is started in the background whenever {@link ChannelClient} opens up a channel to the node on which the
 * service is installed. It is likewise stopped when the channel is closed, see {@link WearableListenerService}. The
 * channel is either closed by the publishing side or if an error occurs during data processing in this service.
 * </p>
 * <p>
 * Messages are separated out of the data stream using a simple custom format. This format is necessary because
 * {@code ChannelClient} communicates through {@link InputStream} and {@link OutputStream}, and therefore does not have
 * the notion of single "messages", but rather operates on a continuous stream of data. See
 * {@link #onChannelOpened(ChannelClient.Channel)} for more details.
 * </p>
 *
 */
public class DataLayerListenerService extends WearableListenerService {

    public static final String LAST_SECOND_INTENT_EXTRA = "records-per-second";
    public static final String UPDATED_ANALYTICS_INTENT_ACTION = "updated-analytics";
    public static final String RECORDS_PER_SECOND_INTENT_EXTRA = "records-last-second";
    public static final int DELAY_MILLIS = 1000;

    private static final String TAG = "wear:" + DataLayerListenerService.class.getSimpleName();

    private static String userId;
    private static String sessionId;

    private MqttService mqttService;
    private ChannelClient.Channel channel;
    private HandlerThread readByteDataThread;

    private int messageCount = 0;
    private int messageCountTotal = 0;
    private long millisElapsed = 0;
    private long lastUpdate = -1;
    private long lastMessage;

    private final Handler statHandler = new Handler();

    /**
     * <p>
     * Calculates the current statistics for the sensor recording session. Invoked through an instance of {@link
     * Handler}, posts itself at the end of the method to run repeatedly every {@link #DELAY_MILLIS} ms.
     * </p>
     * <p>
     * See the wearable application documentation for more details, which uses the same logic.
     * </p>
     */
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
        intent.putExtra(RECORDS_PER_SECOND_INTENT_EXTRA, value);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    /**
     * Callback that is executed when the {@link ChannelClient.Channel} is closed. Removes the current session ID from
     * {@link SharedPreferences}, and adds it to the list of past sessions.
     *
     * @param channel See {@link WearableListenerService#onChannelClosed(ChannelClient.Channel, int, int)}
     * @param closeReason See {@link WearableListenerService#onChannelClosed(ChannelClient.Channel, int, int)}
     * @param appSpecificErrorCode See {@link WearableListenerService#onChannelClosed(ChannelClient.Channel, int, int)}
     *
     * @see WearableListenerService
     * @see SharedPreferences
     * @see ChannelClient
     */
    @Override
    public void onChannelClosed(ChannelClient.Channel channel, int closeReason, int appSpecificErrorCode) {
        super.onChannelClosed(channel, closeReason, appSpecificErrorCode);
        Log.d(TAG, "on channel closed");
        SharedPreferences sharedPreferences =
                getApplicationContext().getSharedPreferences(getApplicationContext().getPackageName(),
                        Context.MODE_PRIVATE);
        SharedPreferences.Editor edit = sharedPreferences.edit();
        edit.remove("session_id").apply();

        List<String> past_sessions = loadListPreference(sharedPreferences, "past_sessions");

        if (past_sessions != null) {
            past_sessions.add(0, sessionId);
        } else {
            past_sessions = new ArrayList<>(Collections.singletonList(sessionId));
        }

        edit.putString("past_sessions", new JSONArray(past_sessions).toString());
        edit.apply();
        sessionId = null;
    }

    /**
     * <p>
     * Callback that is executed when the {@link ChannelClient.Channel} is opened. Connects to the MQTT broker and
     * initiates processing of the data published by the other node through {@link ChannelClient}.
     * </p>
     * <p>
     * The connection to the MQTT broker is established through {@link AccountConnector} first, which acts as an
     * interface to {@link AccountManager} and {@link MqttService}. Stored credentials of the account are used to
     * connect to the broker.
     * </p>
     * <p>
     * Subsequently, the corresponding {@link InputStream} is acquired from the opened {@code ChannelClient.Channel}. A
     * success listener is used to start processing the data as soon as the {@link InputStream} is ready to use.
     * </p>
     * <p>
     * {@link SharedPreferences} is used to retrieve the current session ID, which is used as an identifier for the MQTT
     * topics.
     * </p>
     * @param channel See {@link WearableListenerService#onChannelOpened(ChannelClient.Channel)}
     *
     * @see WearableListenerService
     * @see AccountManager
     * @see MqttService
     * @see SharedPreferences
     * @see ChannelClient
     *
     */
    @Override
    public void onChannelOpened(ChannelClient.Channel channel) {
        super.onChannelOpened(channel);
        Log.i(TAG, "channel opened!");
        this.channel = channel;
        this.readByteDataThread = new HandlerThread("Read byte data", Process.THREAD_PRIORITY_MORE_FAVORABLE);
        this.readByteDataThread.start();

        AccountConnector.connect(this, true, false, !loginScreenActive, mqttService);

        Wearable.getChannelClient(this)
                .getInputStream(channel)
                .addOnSuccessListener(this::startProcessing);

        SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences(getApplicationContext().getPackageName(), Context.MODE_PRIVATE);
        userId = sharedPreferences.getString("UUID", null);
        sessionId = UUID.randomUUID().toString();
        sharedPreferences.edit().putString("session_id", sessionId).apply();
    }

    public void startProcessing(InputStream command) {
        // without new thread, UI hangs. but why? this is supposed to run on a separate thread!
        new Handler(readByteDataThread.getLooper()).post(() -> {
            Log.i(TAG, "Input stream for channel " + channel.getPath() + " retrieved succesfully. Reading in new " +
                    "thread.");
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

    /**
     * <p>
     * Reads the stream of bytes by processing each message.As data is received as a continuous stream of bytes,
     * a simple custom protocol is used to extract separate messages from the stream. The processor reads the stream in
     * chunks of 40 bytes, the maximum size of a message defined by the protocol.
     * </p>
     * <p>
     * See the documentation of the publishing side in the wearable application for more details about the protocol.
     * </p>
     * @param inputStream Stream of sensor data to be processed
     *
     * @throws IOException if the {@link InputStream} was closed on the other side or could not be processed.
     */
    private void readStream(InputStream inputStream) throws IOException {
        // 24 byte max length of the values array between all sensor types (6 float numbers).
        // + 8 byte timestamp + 4 byte sensor type + 4 byte length value
        byte[] data = new byte[40];
        ByteArrayOutputStream tempBaos = new ByteArrayOutputStream(data.length);
        int c;
        Log.i(TAG, "Start reading channel input stream");
        while ((c = inputStream.read(data, 0, data.length)) != -1) {
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
        String sensorName = Globals.sensorTopicNames.get(sensorType);
        int payloadLength = buf.getInt();
        float[] floats = getFloats(buf, payloadLength);

        if (!mqttService.isConnected()) {
            return;
        }
        if (sensorName == null) {
            Log.e(TAG, String.format("Topic name not defined for sensor type %s", sensorType));
            return;
        }
        if (userId == null) {
            Log.e(TAG, "User ID was not found in shared preferences.");
            return;
        }
        if (sessionId == null) {
            Log.e(TAG, "Session ID was not found.");
            return;
        }

        JSONObject sample = new JSONObject();
        Instant now = Instant.ofEpochMilli(timestamp);
        try {
            sample.put("time", now.toEpochMilli());
            sample.put("values", new JSONArray(floats));
            sample.put("sessionId", sessionId);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        String topic = String.format("sensors/%s/%s", sensorName, userId);
        String message = sample.toString();

        mqttService.sendMessage(topic, message.getBytes());
    }

    private void updateCount(long now) {
        lastMessage = now;
        messageCount++;
        messageCountTotal++;
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
