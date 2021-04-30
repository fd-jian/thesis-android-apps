package de.dipf.edutec.thriller.experiencesampling.conf;

import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import de.dipf.edutec.thriller.experiencesampling.activities.MainActivity;
import de.dipf.edutec.thriller.experiencesampling.foreground.ForegroundNotificationCreator;
import de.dipf.edutec.thriller.experiencesampling.sensorservice.transport.*;
import lombok.Getter;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

@Getter
public class ApplicationContext {

    private static final boolean DEVELOPMENT = true;

    private static final String CHANNEL_ID = "ForegroundServiceChannel";
    private static final String NOTIFICATION_CONTENT_TITLE = "App is still running";
    private static final String NOTIFICATION_CONTENT_TEXT = "Tap to go back to the app.";

    private final MqttService mqttService;
    private final ForegroundNotificationCreator foregroundNotificationCreator;

    public ApplicationContext(Context ctx, NotificationManager notificationManager) {

        MqttConnectOptions connOpts = new MqttConnectOptions();
        connOpts.setCleanSession(true);
        connOpts.setAutomaticReconnect(true);
        connOpts.setConnectionTimeout(10);

        SslSocketFactoryWrapper sslSocketFactoryWrapper = createSslSocketFactoryWrapper(ctx);
        // if null, there was no certificate provided in PROD, so only the trusted android certificates will be allowed
        if(sslSocketFactoryWrapper != null) {
            connOpts.setSocketFactory(sslSocketFactoryWrapper.create());
            connOpts.setSSLHostnameVerifier((hostname, session) -> true);
        }

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
                        .clientId(getUuid(ctx).toString())
                        .build()
                        .build(),
                connOpts);
    }

    private UUID getUuid(Context ctx) {
        UUID clientId;
        SharedPreferences pref = ctx.getSharedPreferences(ctx.getPackageName(), Context.MODE_PRIVATE);
        String string = pref.getString("UUID", null);
        if(string == null){
            clientId = UUID.randomUUID();
            pref.edit().putString("UUID", clientId.toString()).apply();
        } else {
            clientId = UUID.fromString(string);
        }
        return clientId;
    }

    @Nullable
    private SslSocketFactoryWrapper createSslSocketFactoryWrapper(Context ctx) {
        SslSocketFactoryWrapper socketFactoryFactory = null;

        if(DEVELOPMENT) {
            socketFactoryFactory = new UnsafeSslSocketFactoryWrapper();
        } else {
            int raw = ctx.getResources().getIdentifier("ca", "raw", ctx.getPackageName());
            if (raw != 0) {
                socketFactoryFactory = new CustomCaSslSocketFactoryWrapper(ctx, raw);
            }
        }
        return socketFactoryFactory;
    }
}
