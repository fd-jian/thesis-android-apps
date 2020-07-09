package com.example.myapplication;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Builder
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class MqttClientBuilder {
    
    private String broker;
    private String clientId;

    @Builder.Default
    private MemoryPersistence persistence = new MemoryPersistence();

    public MqttClient build() {
        try {
            return new MqttClient(broker, clientId, persistence);
        } catch (MqttException e) {
            throw new RuntimeException(e);
        }
    }

}
