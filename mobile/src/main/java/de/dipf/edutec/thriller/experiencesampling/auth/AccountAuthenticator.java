package de.dipf.edutec.thriller.experiencesampling.auth;

import android.accounts.*;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import de.dipf.edutec.thriller.experiencesampling.activities.LoginActivity;

/**
 * <p>
 * Implements the authentication management logic for MQTT accounts, specifically adding and updating account
 * credentials. Token based authentication is not yet implemented, mainly due to limitations on the MQTT broker's end.
 * </p>
 * <p>
 * The overridden callback methods are called through {@link AccountManager}, when the corresponding account management
 * methods are called.
 * </p>
 * @see AccountManager
 * @see AbstractAccountAuthenticator
 * @see AccountAuthenticatorActivity
 */
public class AccountAuthenticator extends AbstractAccountAuthenticator {
    private final Context mContext;
    public AccountAuthenticator(Context context) {
        super(context);
        mContext = context;
    }

    @Override
    public Bundle editProperties(AccountAuthenticatorResponse response, String accountType) {
        throw new UnsupportedOperationException();
    }

    /**
     * Initiates the process of creating an MQTT account on the device. Triggered by
     * {@link AccountManager#addAccount(String, String, String[], Bundle, Activity, AccountManagerCallback, Handler)}.
     * The appropriate login activity of type {@link AccountAuthenticatorActivity} as well as additional extra
     * parameters are configured through the returned intent bundle.
     *
     * @param response See {@link AbstractAccountAuthenticator#addAccount(AccountAuthenticatorResponse, String, String, String[], Bundle)}
     * @param accountType See {@link AbstractAccountAuthenticator#addAccount(AccountAuthenticatorResponse, String, String, String[], Bundle)}
     * @param authTokenType See {@link AbstractAccountAuthenticator#addAccount(AccountAuthenticatorResponse, String, String, String[], Bundle)}
     * @param requiredFeatures See {@link AbstractAccountAuthenticator#addAccount(AccountAuthenticatorResponse, String, String, String[], Bundle)}
     * @param options See {@link AbstractAccountAuthenticator#addAccount(AccountAuthenticatorResponse, String, String, String[], Bundle)}
     * @return Bundle containing the intent that specifies the appropriate login activity and extra parameters.
     *
     * @see AccountAuthenticatorActivity
     * @see AccountManager
     *
     */
    @Override
    public Bundle addAccount(AccountAuthenticatorResponse response, String accountType, String authTokenType, String[] requiredFeatures, Bundle options) {
        final Intent intent = new Intent(mContext, LoginActivity.class);
        intent.putExtra(LoginActivity.ARG_ACCOUNT_TYPE, accountType);
//        intent.putExtra(LoginActivity.ARG_AUTH_TYPE, authTokenType);
        intent.putExtra(LoginActivity.ARG_IS_ADDING_NEW_ACCOUNT, true);
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);

        final Bundle bundle = new Bundle();
        bundle.putParcelable(AccountManager.KEY_INTENT, intent);
        return bundle;
    }

    @Override
    public Bundle confirmCredentials(AccountAuthenticatorResponse response, Account account, Bundle options) {

        return null;
    }

    @Override
    public Bundle getAuthToken(AccountAuthenticatorResponse response, Account account, String authTokenType, Bundle options) {
        return null;
    }

    @Override
    public String getAuthTokenLabel(String authTokenType) {
        return null;
    }

    /**
     * Initiates the process of updating the credentials of an MQTT account on the device.
     * The appropriate credential modification activity of type {@link AccountAuthenticatorActivity} as well as
     * additional extra parameters are configured through the returned intent bundle.
     *
     * @param response See {@link AbstractAccountAuthenticator#updateCredentials(AccountAuthenticatorResponse, Account, String, Bundle)}
     * @param authTokenType See {@link AbstractAccountAuthenticator#updateCredentials(AccountAuthenticatorResponse, Account, String, Bundle)}
     * @param options See {@link AbstractAccountAuthenticator#updateCredentials(AccountAuthenticatorResponse, Account, String, Bundle)}
     * @return Bundle containing the intent that specifies the appropriate credential modification activity and extra parameters.
     *
     * @see AccountAuthenticatorActivity
     * @see AccountManager
     */
    @Override public Bundle updateCredentials(AccountAuthenticatorResponse response, Account account, String authTokenType, Bundle options) {
        final Intent intent = new Intent(mContext, LoginActivity.class);
        intent.putExtra(LoginActivity.ARG_ACCOUNT_TYPE, account.type);
//        intent.putExtra(LoginActivity.ARG_AUTH_TYPE, authTokenType);
        intent.putExtra(LoginActivity.ARG_ACCOUNT_NAME, account.name);
        intent.putExtra(LoginActivity.ARG_IS_ADDING_NEW_ACCOUNT, false);
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);

        final Bundle bundle = new Bundle();
        bundle.putParcelable(AccountManager.KEY_INTENT, intent);
        return bundle;
    }

    @Override
    public Bundle hasFeatures(AccountAuthenticatorResponse response, Account account, String[] features) throws NetworkErrorException {
        final Bundle result = new Bundle();
        result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, false);
        return result;
    }
}
