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
    public static final boolean DEVELOPMENT = true;
    private final int caRes;
    private final Context context;

    public CustomSslSocketFactory(Context ctx, int caRes) {
        this.context = ctx;
        this.caRes = caRes;
    }

    public SSLSocketFactory create() {
        InputStream caInput = context.getResources().openRawResource(caRes);
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            // TODO: unsafe certificate validation for development, turn off later
            if(DEVELOPMENT) {
                TrustManager[] trustAllCerts = new TrustManager[] {
                        new X509TrustManager() {

                            public X509Certificate[] getAcceptedIssuers() {
                                return new X509Certificate[0];
                            }

                            public void checkClientTrusted(
                                    X509Certificate[] certs, String authType) {
                            }

                            public void checkServerTrusted(
                                    X509Certificate[] certs, String authType) {
                            }
                        }

                };
                sslContext.init(null, trustAllCerts, new SecureRandom());
                return sslContext.getSocketFactory();
            }

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

    private void closeStream(InputStream inputStream) {
        try {
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
