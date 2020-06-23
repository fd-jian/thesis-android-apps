package com.example.myapplication;

import android.content.Context;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.IOException;
import java.io.InputStream;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;

public class CustomSslSocketFactory {
    private final int caRes;
    private final Context context;

    public CustomSslSocketFactory(Context ctx, int caRes) {
        this.context = ctx;
        this.caRes = caRes;
    }

    public SSLSocketFactory create() {
        InputStream caInput = context.getResources().openRawResource(caRes);
        try {
            Certificate ca = CertificateFactory.getInstance("X.509").generateCertificate(caInput);

            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(null, null);
            keyStore.setCertificateEntry("ca", ca);

            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(keyStore);

            SSLContext sslContext = SSLContext.getInstance("TLS");
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
