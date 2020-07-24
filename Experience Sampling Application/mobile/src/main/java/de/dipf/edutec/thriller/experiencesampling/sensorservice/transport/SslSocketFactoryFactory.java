package de.dipf.edutec.thriller.experiencesampling.sensorservice.transport;

import javax.net.ssl.SSLSocketFactory;

public interface SslSocketFactoryFactory {
    SSLSocketFactory create();
}
