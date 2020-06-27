package com.example.myapplication;

import android.content.Context;
import androidx.annotation.RawRes;
import lombok.Getter;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;

@Getter
public class ApplicationContext {

    private final MqttService mqttService;

    public ApplicationContext(Context ctx, @RawRes int caRes) {

        MqttConnectOptions connOpts = new MqttConnectOptions();
        connOpts.setCleanSession(true);
        connOpts.setAutomaticReconnect(true);
        connOpts.setConnectionTimeout(10);
        connOpts.setSocketFactory(new CustomSslSocketFactory(ctx, caRes).create());

        mqttService = new MqttService(
                MqttClientBuilder.builder()
                        .broker("ssl://flex-pc:8883")
                        .clientId(MqttAsyncClient.generateClientId())
                        .build()
                        .build(),
                connOpts);
    }
}
