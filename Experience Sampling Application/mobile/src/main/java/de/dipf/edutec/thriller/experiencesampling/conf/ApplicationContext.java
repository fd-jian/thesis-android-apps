package de.dipf.edutec.thriller.experiencesampling.conf;

import android.content.Context;
import androidx.annotation.RawRes;
import de.dipf.edutec.thriller.experiencesampling.sensorservice.transport.CustomSslSocketFactory;
import de.dipf.edutec.thriller.experiencesampling.sensorservice.transport.MqttClientBuilder;
import de.dipf.edutec.thriller.experiencesampling.sensorservice.transport.MqttService;
import lombok.Getter;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;

@Getter
public class ApplicationContext {
    // TODO: unsafe certificate validation for development, turn off later
    public static final boolean DEVELOPMENT = true;

    private final MqttService mqttService;

    public ApplicationContext(Context ctx, @RawRes int caRes) {

        MqttConnectOptions connOpts = new MqttConnectOptions();
        connOpts.setCleanSession(true);
        connOpts.setAutomaticReconnect(true);
        connOpts.setConnectionTimeout(10);
        connOpts.setSocketFactory(new CustomSslSocketFactory(ctx, caRes, DEVELOPMENT).create());

        mqttService = new MqttService(
                MqttClientBuilder.builder()
                        .broker("ssl://flex-pc:8883")
                        .clientId(MqttAsyncClient.generateClientId())
                        .build()
                        .build(),
                connOpts);
    }
}
