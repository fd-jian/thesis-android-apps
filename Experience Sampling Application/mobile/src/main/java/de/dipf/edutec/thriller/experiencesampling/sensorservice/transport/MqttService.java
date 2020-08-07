package de.dipf.edutec.thriller.experiencesampling.sensorservice.transport;

import android.os.Handler;
import android.util.Log;
import lombok.RequiredArgsConstructor;
import org.eclipse.paho.client.mqttv3.*;

@RequiredArgsConstructor
public class MqttService {

    private final static String TAG = "wear:" + MqttService.class.getSimpleName();
    private static final int RECONNECT_INTERVAL = 30000;

    private final MqttAsyncClient sampleClient;
    private final MqttConnectOptions connOpts;

    private final static int QOS = 0;
    private Handler handler = new Handler();

    // Runnable that tries to connect to mqtt broker. If the connection fails, the runnable passes itself
    // to a handler to be repeated every 30s. After this initial connection has been established, the connection
    // will be managed automatically by eclipse paho`s MqttConnectOptions#setAutomaticReconnect(boolean)}.
    private Runnable repeatingConnect = new Runnable() {
        public void run() {
            try {
                doConnect();
                handler.removeCallbacks(this);
            } catch (MqttException e) {
                Log.e(TAG, e.getMessage());
                Log.e(TAG, "connecting to mqtt broker failed. Retrying every minute.");
                handler.postDelayed(this, RECONNECT_INTERVAL);
            }
        }
    };

    public IMqttToken doConnect() throws MqttException {
        IMqttToken connect = sampleClient.connect(connOpts);
        connect.waitForCompletion();
        Log.d(TAG, "Connected");
        return connect;
    }

    public void sendMessage(String topic, byte[] content) {
        try {
//            Log.d(TAG, "Publishing message to topic '" + topic + "': " + content);
            MqttMessage message = new MqttMessage(content);
            message.setQos(QOS);
            sampleClient.publish(topic, message);
//            Log.d(TAG, "Message published");
        } catch (MqttException me) {
            handleException(me);
        }

    }

    public void connect() {
        Log.d(TAG, "Connecting to broker: " + sampleClient.getServerURI());
        repeatingConnect.run();
    }

    public void loginCheck() throws MqttException {
        Log.d(TAG, "Connecting to broker: " + sampleClient.getServerURI());
        IMqttToken connect = sampleClient.connect(connOpts);
        connect.waitForCompletion();
        Log.d(TAG, "Connected");
        try {
            IMqttToken disconnect = sampleClient.disconnect();
            disconnect.waitForCompletion();
            Log.d(TAG, "Disconnected");
        } catch (MqttException e) {
            handleException(e);
        }
    }

    public boolean isConnected() {
        return sampleClient.isConnected();
    }

    public void disconnect() {
        try {
            IMqttToken disconnect = sampleClient.disconnect();
            disconnect.waitForCompletion();
        } catch (MqttException me) {
            handleException(me);
        }
        handler.removeCallbacks(repeatingConnect);
        Log.d(TAG, "Disconnected from broker: " + sampleClient.getServerURI());
    }

    private void handleException(MqttException me) {
        Log.d(TAG, "reason " + me.getReasonCode());
        Log.d(TAG, "msg " + me.getMessage());
        Log.d(TAG, "loc " + me.getLocalizedMessage());
        Log.d(TAG, "cause " + me.getCause());
        Log.d(TAG, "excep " + me);
    }
}
