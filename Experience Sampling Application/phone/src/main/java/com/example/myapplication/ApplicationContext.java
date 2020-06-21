package com.example.myapplication;

import lombok.Getter;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;

@Getter
public class ApplicationContext {

    private final MqttService mqttService;

    public ApplicationContext() {

        MqttConnectOptions connOpts = new MqttConnectOptions();
        connOpts.setCleanSession(true);
        connOpts.setAutomaticReconnect(true);
        connOpts.setConnectionTimeout(10);

        mqttService = new MqttService(
                MqttClientBuilder.builder()
                        .broker("tcp://flex-pc:1883")
                        .clientId(MqttAsyncClient.generateClientId())
                        .build()
                        .build(),
                connOpts);
    }
}
