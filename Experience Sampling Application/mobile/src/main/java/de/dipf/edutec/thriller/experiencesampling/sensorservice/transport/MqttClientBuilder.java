package de.dipf.edutec.thriller.experiencesampling.sensorservice.transport;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

@Builder
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class MqttClientBuilder {

    @Builder.Default
    // TODO: find solution to get rid of dummy url
    private String broker = "ssl://example:8883";
    private String clientId;

    @Builder.Default
    private MemoryPersistence persistence = new MemoryPersistence();

    public MqttAsyncClient build() {
        try {
            return new MqttAsyncClient(broker, clientId, persistence);
        } catch (MqttException e) {
            throw new RuntimeException(e);
        }
    }

}
