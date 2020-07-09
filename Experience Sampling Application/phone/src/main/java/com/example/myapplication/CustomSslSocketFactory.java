package com.example.myapplication;

import android.content.Context;

import javax.net.ssl.*;
import java.io.IOException;
import java.io.InputStream;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

public class CustomSslSocketFactory {
    private final boolean allowAllCerts;
    private final int caRes;
    private final Context context;

    public CustomSslSocketFactory(Context ctx, int caRes, boolean allowAllCerts) {
        this.context = ctx;
        this.caRes = caRes;
        this.allowAllCerts = allowAllCerts;
    }

    public SSLSocketFactory create() {
        if(allowAllCerts) {
            return createAllCertsTrustingFactory();
        }

        InputStream caInput = context.getResources().openRawResource(caRes);
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            Certificate ca = CertificateFactory.getInstance("X.509").generateCertificate(caInput);

            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(null, null);
            keyStore.setCertificateEntry("ca", ca);

            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(keyStore);

            sslContext.init(null, tmf.getTrustManagers(), new SecureRandom());

            return sslContext.getSocketFactory();
        } catch (CertificateException |
                KeyStoreException |
                IOException |
                NoSuchAlgorithmException |
                KeyManagementException e) {
            throw new RuntimeException(e);
        } finally {
            this.closeStream(caInput);
        }

    }

    private SSLSocketFactory createAllCertsTrustingFactory() {
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

    private void closeStream(InputStream inputStream) {
        try {
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
