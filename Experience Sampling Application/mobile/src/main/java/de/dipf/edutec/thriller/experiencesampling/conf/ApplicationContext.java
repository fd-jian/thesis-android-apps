package de.dipf.edutec.thriller.experiencesampling.conf;

import android.app.NotificationManager;
import android.content.Context;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;
import com.google.android.gms.auth.api.credentials.*;
import com.google.android.gms.common.api.ResolvableApiException;
import de.dipf.edutec.thriller.experiencesampling.activities.MainActivity;
import de.dipf.edutec.thriller.experiencesampling.foreground.ForegroundNotificationCreator;
import de.dipf.edutec.thriller.experiencesampling.sensorservice.transport.*;
import lombok.Getter;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

@Getter
public class ApplicationContext {
    // TODO: unsafe certificate validation for development, turn off later
    private static final boolean DEVELOPMENT = true;
    private static final String CHANNEL_ID = "ForegroundServiceChannel";
    private static final String NOTIFICATION_CONTENT_TITLE = "App is still running";
    private static final String NOTIFICATION_CONTENT_TEXT = "Tap to go back to the app.";

    // TODO: make user configurable
    private static final String MQTT_BROKER_URL = "ssl://flex-pc:8883";

    private final MqttService mqttService;
    private final ForegroundNotificationCreator foregroundNotificationCreator;

    public ApplicationContext(Context ctx, NotificationManager notificationManager) {

        MqttConnectOptions connOpts = new MqttConnectOptions();
        connOpts.setCleanSession(true);
        connOpts.setAutomaticReconnect(true);
        connOpts.setConnectionTimeout(10);
        SslSocketFactoryFactory sslSocketFactoryFactory = getSslSocketFactoryWrapper(ctx);

        // if null, there was no certificate provided in PROD so the trusted android certificates will be allowed
        if(sslSocketFactoryFactory != null) {
            connOpts.setSocketFactory(sslSocketFactoryFactory.create());
        }

        // TODO: put this right after the successful login to mqtt client with provided credentials
//        CredentialsClient client = Credentials.getClient(ctx);
//        Credential cred = new Credential.Builder("username")
//                .setPassword("password")
//                .setAccountType("thriller")
//                .build();
//
//        client.save(cred).addOnCompleteListener(task -> {
//            if (task.isSuccessful()) {
//                Log.d("tag", "SAVE: OK");
//                Toast.makeText(ctx, "Credentials saved", Toast.LENGTH_SHORT).show();
//                return;
//            }
//
//            Exception e = task.getException();
//            if (e instanceof ResolvableApiException) {
//                // Try to resolve the save request. This will prompt the user if
//                // the credential is new.
//                ResolvableApiException rae = (ResolvableApiException) e;
//                try {
//                    rae.startResolutionForResult(activity, 1);
//                } catch (IntentSender.SendIntentException exception) {
//                    // Could not resolve the request
//                    Log.e("tag", "Failed to send resolution.", exception);
//                    Toast.makeText(ctx, "Save failed", Toast.LENGTH_SHORT).show();
//                }
//            } else {
//                Toast.makeText(ctx, "Save failed", Toast.LENGTH_SHORT).show();
//            }
//        });

        // TODO: put this right before mqtt connection is established to retrieve stored username and password AND
        //  also before initial login. in the first case, it is a background service so it should fail silently.
        //  in the second case, after app start, the user has to be redirected to login on error
//        CredentialsClient client = Credentials.getClient(ctx);
//        CredentialRequest build = new CredentialRequest.Builder()
//                .setPasswordLoginSupported(true)
//                .setAccountTypes("thriller")
//                .build();
//
//        client.request(build).addOnCompleteListener(command -> {
//            if(command.isSuccessful()) {
//                Credential credential = command.getResult().getCredential();
//                String accountType = credential.getAccountType();
//
//                if (accountType == null) {
//                    //signInWithPassword(credential.getId(), credential.getPassword());
//                } else if (accountType.equals("thriller")) {
//                    // user is already signed in with thriller
//                }
//                return;
//            }
//
//            Exception e = command.getException();
//            Log.e("appcontext", "Could not retrieve user credentials from store: ");
//            Log.e("appcontext", e.getMessage());
//            e.printStackTrace();
//            // redirect to login
//        });

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
            pref.edit().putString("UUID", clientId.toString());
        } else {
            clientId = UUID.fromString(string);
        }
        return clientId;
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
