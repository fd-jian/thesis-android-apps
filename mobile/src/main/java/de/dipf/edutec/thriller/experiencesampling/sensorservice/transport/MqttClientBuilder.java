package de.dipf.edutec.thriller.experiencesampling.sensorservice.transport;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

/**
 * <p>
 * Builds an instance of {@link MqttAsyncClient}.
 * </p>
 * <p>
 * Caveat: Through the @{@link Builder} annotation, an instance of this builder class
 * is built, rather than an actual instance of {@code MqttAsyncClient}. The build method must be called on the result
 * as well to finally obtain an instance of {@code MqttAsyncClient}. This may seem like an odd design, but the
 * {@code @Builder} annotation still saves quite some boilerplate code.
 * </p>
 */
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
