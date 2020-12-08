package de.dipf.edutec.thriller.experiencesampling.auth;

import android.accounts.AccountManager;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import androidx.annotation.Nullable;

/**
 * Activates {@link AccountAuthenticator} to be used by {@link AccountManager} by returning its {@link IBinder} on bind.
 */
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
