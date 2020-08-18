package de.dipf.edutec.thriller.experiencesampling.sensorservice.transport;

import javax.net.ssl.*;
import java.security.*;
import java.security.cert.X509Certificate;

public class UnsafeSslSocketFactoryWrapper implements SslSocketFactoryWrapper {

    public SSLSocketFactory create() {
        SSLContext sslContext;
        try {
            sslContext = SSLContext.getInstance("TLS");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        TrustManager[] trustAllCerts = new TrustManager[] {
                new X509TrustManager() {

                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }

                    public void checkClientTrusted(
                            X509Certificate[] certs, String authType) {
                        // trust all certificates with this TrustManagger
                    }

                    public void checkServerTrusted(
                            X509Certificate[] certs, String authType) {
                        // trust all certificates with this TrustManagger
                    }
                }

        };
        try {
            sslContext.init(null, trustAllCerts, new SecureRandom());
        } catch (KeyManagementException e) {
            throw new RuntimeException(e);
        }
        return sslContext.getSocketFactory();
    }

}
