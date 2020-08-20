package de.dipf.edutec.thriller.experiencesampling.sensorservice.transport;

import android.content.Context;
import androidx.annotation.RawRes;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.IOException;
import java.io.InputStream;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;

public class CustomCaSslSocketFactoryWrapper implements SslSocketFactoryWrapper {
    private final Context context;
    private final int caRes;

    public CustomCaSslSocketFactoryWrapper(Context context, @RawRes int caRes) {
        this.context = context;
        this.caRes = caRes;
    }

    public SSLSocketFactory create() {
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

    private void closeStream(InputStream inputStream) {
        try {
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
