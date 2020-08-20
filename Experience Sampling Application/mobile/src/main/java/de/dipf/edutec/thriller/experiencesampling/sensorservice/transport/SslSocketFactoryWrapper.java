package de.dipf.edutec.thriller.experiencesampling.sensorservice.transport;

import javax.net.ssl.SSLSocketFactory;

public interface SslSocketFactoryWrapper {
    SSLSocketFactory create();
}
