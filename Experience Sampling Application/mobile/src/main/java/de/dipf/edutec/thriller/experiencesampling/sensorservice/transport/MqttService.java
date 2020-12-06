package de.dipf.edutec.thriller.experiencesampling.sensorservice.transport;

import android.os.Handler;
import android.util.Log;
import lombok.RequiredArgsConstructor;
import org.eclipse.paho.client.mqttv3.*;

import static de.dipf.edutec.thriller.experiencesampling.conf.Globals.useOffline;

/**
 * <p>
 * Provides a simplified interface to {@link MqttAsyncClient} for use by the android app.
 * </p>
 * <p>
 * Also features a "repeating connect" functionality, which causes the client to retry a connection in an interval
 * specified by {@link #RECONNECT_INTERVAL}.
 * <p>
 * Only necessary before the initial connection is established, at which point {@code MqttAsyncClient} will handle
 * the automatic reconnect.
 * </p>
 */
@RequiredArgsConstructor
public class MqttService {

    private final static String TAG = "wear:" + MqttService.class.getSimpleName();
    private static final int RECONNECT_INTERVAL = 30000;

    private final MqttAsyncClient sampleClient;
    private final MqttConnectOptions connOpts;

    private final static int QOS = 0;
    private Handler handler = new Handler();

    /**
     * Runnable that tries to connect to the MQTT broker. If the connection fails, the runnable passes itself to a
     * handler to be repeated every 30s. After this initial connection has been established, the connection will be
     * managed automatically by eclipse paho`s MqttConnectOptions#setAutomaticReconnect(boolean)}.
     */
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

    /**
     * <p>
     * Perform the actual connection operation to the MQTT broker.
     * </p>
     * <p>
     * The asynchronous nature of the client was mostly not desired in
     * the case of the connect operation; therefore, it is executed in a blocking manner through
     * {@link IMqttToken#waitForCompletion()}`.
     * </p>
     * The method will block until either
     * <ol type="a">
     * <li>The connection was established successfully</li>
     * <li>An exception occurred during the connect operation</li>
     * </ol>
     *
     * @return MQTT token of the successfully established connection.
     * @throws MqttException if the connection to the MQTT broker fails.
     */
    public IMqttToken doConnect() throws MqttException {
        Log.d(TAG, "Connecting to broker: " + sampleClient.getServerURI());
        IMqttToken connect = sampleClient.connect(connOpts);
        connect.waitForCompletion();
        Log.d(TAG, "Connected");
        useOffline = false;
        return connect;
    }

    /**
     * Publishes a message to the MQTT broker.
     *
     * @param topic Topic on which the message shall be published.
     * @param content Content of the message.
     */
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

    /**
     * Connects to the MQTT broker by initiating a repeating connect handler.
     */
    public void connect() {
        handler.removeCallbacks(repeatingConnect);
        repeatingConnect.run();
    }

    /**
     * Same as {@link #connect()}, but with explicitly specified username, password and url.
     *
     * @param username User name for authentication with the MQTT broker.
     * @param password For authentication with the MQTT broker.
     * @param url Of the MQTT broker.
     */
    public void connect(String username, String password, String url) {
        connOpts.setUserName(username);
        connOpts.setPassword(password.toCharArray());
        connOpts.setServerURIs(new String[] { url });
        connect();
    }

    /**
     * Same as {@link #loginCheck()}, but with explicitly specified credentials and URL.
     *
     * @param username User name for authentication with the MQTT broker.
     * @param password For authentication with the MQTT broker.
     * @param url Of the MQTT broker.
     * @throws MqttException if the broker is unavailable or the credentials are incorrect.
     */
    public void loginCheck(String username, String password, String url) throws MqttException {
        connOpts.setUserName(username);
        connOpts.setPassword(password.toCharArray());
        connOpts.setServerURIs(new String[] { url });
        loginCheck();
    }

    /**
     * <p>
     * Checks if the user is able to login with the specified credentials.
     * </p>
     * <p>
     * If a connection to the broker is established and the credentials are correct, the method terminates normally.
     * In all other cases, an exception is thrown. To find out what caused the exception, the return code of the
     * exception must be inspected.
     * </p>
     * @throws MqttException if the broker is unavailable or the credentials are incorrect.
     */
    public void loginCheck() throws MqttException {
        Log.d(TAG, "Connecting to broker: " + sampleClient.getServerURI());
        doConnect();
        try {
            IMqttToken disconnect = sampleClient.disconnect();
            disconnect.waitForCompletion();
            Log.d(TAG, "Disconnected");
        } catch (MqttException e) {
            handleException(e);
        }
    }

    /**
     * Checks if the client is currently connected to the MQTT broker.
     *
     * @return True if connected, false if not.
     */
    public boolean isConnected() {
        boolean connected = sampleClient.isConnected();
        if (connected) {
            useOffline = false;
        }
        return connected;
    }

    /**
     * Disconnects the client from the MQTT broker.
     * <p>
     * The asynchronous nature of the client was mostly not desired in the case of the disconnect operation; therefore,
     * it is executed in a blocking manner through {@link IMqttToken#waitForCompletion()}`.
     * </p>
     * The method will block until either
     * <ol type="a">
     * <li>The connection closed successfully</li>
     * <li>An exception occurred during the disconnect operation</li>
     * </ol>
     */
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
