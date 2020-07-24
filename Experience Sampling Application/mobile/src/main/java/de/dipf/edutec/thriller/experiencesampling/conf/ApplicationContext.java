package de.dipf.edutec.thriller.experiencesampling.conf;

import android.app.NotificationManager;
import android.content.Context;
import de.dipf.edutec.thriller.experiencesampling.activities.MainActivity;
import de.dipf.edutec.thriller.experiencesampling.foreground.ForegroundNotificationCreator;
import de.dipf.edutec.thriller.experiencesampling.sensorservice.transport.*;
import lombok.Getter;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.jetbrains.annotations.Nullable;

@Getter
public class ApplicationContext {
    // TODO: unsafe certificate validation for development, turn off later
    private static final boolean DEVELOPMENT = true;
    private static final String CHANNEL_ID = "ForegroundServiceChannel";
    private static final String NOTIFICATION_CONTENT_TITLE = "App is still running";
    private static final String NOTIFICATION_CONTENT_TEXT = "Tap to go back to the app.";
    private static final String MQTT_BROKER_URL = "ssl://flex-pc:8883";

    private final MqttService mqttService;
    private final ForegroundNotificationCreator foregroundNotificationCreator;

    public ApplicationContext(Context ctx, NotificationManager notificationManager) {

        MqttConnectOptions connOpts = new MqttConnectOptions();
        connOpts.setCleanSession(true);
        connOpts.setAutomaticReconnect(true);
        connOpts.setConnectionTimeout(10);
        SslSocketFactoryFactory sslSocketFactoryFactory = getSslSocketFactoryWrapper(ctx);

        if(sslSocketFactoryFactory != null) {
            connOpts.setSocketFactory(sslSocketFactoryFactory.create());
        }

        connOpts.setPassword("changeme".toCharArray());
        connOpts.setUserName("user");

        this.foregroundNotificationCreator = new ForegroundNotificationCreator(
                1,
                CHANNEL_ID,
                ctx,
                MainActivity.class,
                notificationManager,
                NOTIFICATION_CONTENT_TITLE,
                NOTIFICATION_CONTENT_TEXT,
                de.dipf.edutec.thriller.experiencesampling.shared.R.drawable.ic_notifications_black_24dp);

        mqttService = new MqttService(
                MqttClientBuilder.builder()
                        .broker(MQTT_BROKER_URL)
                        .clientId(MqttAsyncClient.generateClientId())
                        .build()
                        .build(),
                connOpts);
    }

    @Nullable
    private SslSocketFactoryFactory getSslSocketFactoryWrapper(Context ctx) {
        SslSocketFactoryFactory socketFactoryFactory = null;

        if(DEVELOPMENT) {
            socketFactoryFactory = new UnsafeSslSocketFactoryFactory();
        } else {
            int raw = ctx.getResources().getIdentifier("ca", "raw", ctx.getPackageName());
            if (raw != 0) {
                socketFactoryFactory = new CustomCaSslSocketFactoryFactory(ctx, raw);
            }
        }
        return socketFactoryFactory;
    }
}
