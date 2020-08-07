package de.dipf.edutec.thriller.experiencesampling.auth;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import androidx.annotation.Nullable;

public class AuthenticatorService extends Service {
    private AccountAuthenticator authenticator;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return authenticator.getIBinder();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        this.authenticator = new AccountAuthenticator(this);
    }
}
