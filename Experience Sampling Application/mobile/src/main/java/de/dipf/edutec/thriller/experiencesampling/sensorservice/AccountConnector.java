package de.dipf.edutec.thriller.experiencesampling.sensorservice;

import android.accounts.*;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;
import androidx.preference.PreferenceManager;
import de.dipf.edutec.thriller.experiencesampling.activities.SettingsActivity;
import de.dipf.edutec.thriller.experiencesampling.auth.AccountAuthenticator;
import de.dipf.edutec.thriller.experiencesampling.sensorservice.transport.MqttService;
import org.eclipse.paho.client.mqttv3.MqttException;

import java.io.IOException;

import static de.dipf.edutec.thriller.experiencesampling.activities.MainActivity.ACCOUNT_TYPE;
import static org.eclipse.paho.client.mqttv3.MqttException.*;

/**
 * Provides methods to connect to the MQTT broker through {@link MqttService} with automatic redirect to login or settings
 * page. {@link AccountManager} is used to manage the authentication process and store login credentials.
 *
 * @see MqttService
 * @see AccountManager
 * @see AbstractAccountAuthenticator
 * @see AccountAuthenticatorActivity
 */
public class AccountConnector {
    private static final String TAG = AccountConnector.class.getSimpleName();

    /**
     *
     * <p>
     * Connects the client to the MQTT broker using credentials from {@link AccountManager}. Redirects automatically to
     * account creation or modification if credentials do not exist or are invalid. Moreover redirects to settings page
     * if necessary settings to connect are not found in {@link SharedPreferences}.
     * </p>
     * <p>
     * Account and credential management is outsourced exclusively to {@code AccountManager}. If no accounts are found
     * for the appropriate account type, the user is redirected through
     * {@link AccountManager#addAccount(String, String, String[], Bundle, Activity, AccountManagerCallback, Handler)}.
     * The corresponding logic to add an account is implemented in
     * {@link AccountAuthenticator#addAccount(AccountAuthenticatorResponse, String, String, String[], Bundle)}. If
     * login credentials are invalid, the process is continued by {@link AccountAuthenticator#updateCredentials(AccountAuthenticatorResponse, Account, String, Bundle)}.
     * </p>
     *
     * @param ctx Context in which the connect logic should take place.
     * @param isFlagNew Start the account management activity with {@link Intent#FLAG_ACTIVITY_NEW_TASK}.
     * @param checkLoginOnly Established a connection to the MQTT broker after login.
     * @param redirect Enable automatic redirect to the account management activity if login fails.
     * @param mqttService Service to login and connect to the MQTT broker.
     * @return True if connection and login was successful, false if not.
     */
    public static boolean connect(Context ctx, boolean isFlagNew, boolean checkLoginOnly, boolean redirect,
                                  MqttService mqttService) {
        Context applicationContext = ctx.getApplicationContext();
        SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(applicationContext);
        String host = sharedPreferences.getString("host", "");
        String port = sharedPreferences.getString("port", "");

        if (!checkPortAndHost(ctx, isFlagNew, applicationContext, host.isEmpty(), port.isEmpty(), true))
            return false;

        AccountManager accountManager = AccountManager.get(applicationContext);
        Account[] accountsByType = accountManager.getAccountsByType(ACCOUNT_TYPE);

        if (accountsByType.length == 0) {
            if (!redirect) {
                if (!isFlagNew) {
                    Toast.makeText(applicationContext, "No login credentials found but offline mode selected. " +
                            "You can sign in through Settings -> 'Change Login Credentials'.", Toast.LENGTH_LONG).show();
                }
                return false;
            }
            accountManager.addAccount(ACCOUNT_TYPE, null, null, null, null,
                    future1 -> {
                        try {
                            Bundle bnd = future1.getResult();
                            Log.d(TAG, "AddNewAccount Bundle is " + bnd);
                            Intent intent = bnd.getParcelable(AccountManager.KEY_INTENT);
                            if (isFlagNew) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            ctx.startActivity(intent);
                            Toast.makeText(applicationContext, "Please create an Account first.", Toast.LENGTH_LONG).show();
                        } catch (Exception e) {
                            e.printStackTrace();
                            Toast.makeText(applicationContext, e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }, null);
            return false;
        }

        Account account = accountsByType[0];

        if (!mqttService.isConnected()) {
            String url = String.format("ssl://%s:%s", host, port);
            try {
                String password = accountManager.getPassword(account);
                String name = account.name;
                mqttService.loginCheck(name, password, url);
                if (!checkLoginOnly) {
                    mqttService.connect(name, password, url);
                }
            } catch (MqttException e) {
                Toast.makeText(applicationContext, e.getReasonCode() + " ;" + e.getCause().toString(),
                        Toast.LENGTH_LONG).show();
                Log.e(TAG, String.valueOf(e.getReasonCode()));
                Log.e(TAG, e.getCause().toString());

                if (e.getReasonCode() == REASON_CODE_NOT_AUTHORIZED ||
                        e.getReasonCode() == REASON_CODE_FAILED_AUTHENTICATION) {

                    if (!redirect) {
                        if (!isFlagNew) {
                            Toast.makeText(applicationContext, "Login failed but offline mode selected. " +
                                    "You can sign in through Settings -> 'Change Login Credentials'.",
                                    Toast.LENGTH_LONG).show();
                        }
                        return false;
                    }

                    Log.e("TAG", "Authentication to MQTT failed");
                    accountManager.updateCredentials(account, null, null, null, future -> {
                        try {
                            Bundle result = future.getResult();
                            Intent intent = result.getParcelable(AccountManager.KEY_INTENT);
                            if (isFlagNew) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            ctx.startActivity(intent);
                        } catch (AuthenticatorException | IOException | OperationCanceledException authenticatorException) {
                            authenticatorException.printStackTrace();
                            Toast.makeText(applicationContext, e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }, null);
                }
                return false;
            }

        }
        return true;
    }

    /**
     * Redirects to {@link SettingsActivity} if host and port configuration is incomplete.
     *
     * @param ctx Context in which the {@code SettingsActivity} shall be launched.
     * @param isFlagNew Start the {@code SettingsActivity} with {@link Intent#FLAG_ACTIVITY_NEW_TASK}.
     * @param applicationContext Application context in which the {@code SettingsActivity} shall be launched.
     * @param badHost Host parameter is invalid.
     * @param badPort Port parameter is invalid.
     * @param isEmpty Parameters are invalid because they are empty.
     *
     * @return True if badHost and badPort are true, false if not.
     */
    public static boolean checkPortAndHost(Context ctx, boolean isFlagNew, Context applicationContext,
                                           boolean badHost, boolean badPort, boolean isEmpty) {
        if (badHost || badPort) {
            String out = isEmpty ? "Please provide MQTT %s." : "Invalid MQTT %s specified";

            if (badHost && badPort) { // in this case, isEmpty will always be true
                out = String.format(out, "host and port");
            } else if (badHost) {
                out = String.format(out, "host");
            } else {
                out = String.format(out, "port");
            }

            Log.e(TAG, "Invalid MQTT configuration. Redirecting to settings.");

            Intent intent = new Intent(applicationContext, SettingsActivity.class);
            if (isFlagNew) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(intent);
            Toast.makeText(applicationContext, out, Toast.LENGTH_LONG).show();
            return false;
        }
        return true;
    }
}
