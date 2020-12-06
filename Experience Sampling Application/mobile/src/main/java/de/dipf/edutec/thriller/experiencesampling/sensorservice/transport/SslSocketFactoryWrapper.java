package de.dipf.edutec.thriller.experiencesampling.sensorservice.transport;

import javax.net.ssl.SSLSocketFactory;

/**
 * Wrapper to create a custom {@link SSLSocketFactory}.
 */
public interface SslSocketFactoryWrapper {
    SSLSocketFactory create();
}
