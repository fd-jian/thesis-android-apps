package com.example.myapplication;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class MqttService {

    private final MqttClient sampleClient;
    private final MqttConnectOptions connOpts;

    private final static int QOS = 0;
    
    public void sendMessage(String topic, byte[] content) {

        try {
            System.out.println("Publishing message to topic '" + topic + "': " + content);
            MqttMessage message = new MqttMessage(content);
            message.setQos(QOS);
            sampleClient.publish(topic, message);
            System.out.println("Message published");
        } catch(MqttException me) {
            handleException(me);
        }

    }

    public void connect() {
        System.out.println("Connecting to broker: "+ sampleClient.getServerURI());
        
        try {
            sampleClient.connect(connOpts);
        } catch(MqttException me) {
            handleException(me);
            throw new RuntimeException();
        }

        System.out.println("Connected");
    }

    public boolean isConnected() {
        return sampleClient.isConnected();
    }

    public void disconnect() {
        try {
            sampleClient.disconnect();
        } catch (MqttException me) {
            handleException(me);
        }
        System.out.println("Disconnected");
    }

    private void handleException(MqttException me) {
            System.out.println("reason "+me.getReasonCode());
            System.out.println("msg "+me.getMessage());
            System.out.println("loc "+me.getLocalizedMessage());
            System.out.println("cause "+me.getCause());
            System.out.println("excep "+me);
            me.printStackTrace();
    }
}
