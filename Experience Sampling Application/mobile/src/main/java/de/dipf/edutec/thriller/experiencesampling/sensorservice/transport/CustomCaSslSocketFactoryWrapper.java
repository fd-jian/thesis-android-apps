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

/**
 * Provides a custom {@link SSLSocketFactory} that accepts a self signed CA certificate.
 */
public class CustomCaSslSocketFactoryWrapper implements SslSocketFactoryWrapper {
    private final Context context;
    private final int caRes;

    /**
     * Instantiates the wrapper with the ID of the raw resource that holds the self-signed CA certificate as well as the
     * {@code Context} that is used to retrieve the resource.
     *
     * @param context context to retrieve the raw resource
     * @param caRes ID of the self-signed Ca certificate resource
     */
    public CustomCaSslSocketFactoryWrapper(Context context, @RawRes int caRes) {
        this.context = context;
        this.caRes = caRes;
    }

    /**
     * Loads the self-signed CA certificate provided through the constructor and adds it to the trust store of a new
     * {@code SSLSocketFactory}.
     *
     * @return socket factory with the custom CA certificate added to the trust store
     */
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
