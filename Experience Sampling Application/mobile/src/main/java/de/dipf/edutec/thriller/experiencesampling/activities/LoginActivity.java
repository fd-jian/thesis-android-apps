package de.dipf.edutec.thriller.experiencesampling.activities;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;
import androidx.preference.PreferenceManager;
import com.google.android.material.textfield.TextInputLayout;
import de.dipf.edutec.thriller.experiencesampling.R;
import de.dipf.edutec.thriller.experiencesampling.auth.AccountAuthenticator;
import de.dipf.edutec.thriller.experiencesampling.conf.CustomApplication;
import de.dipf.edutec.thriller.experiencesampling.sensorservice.transport.MqttService;
import org.eclipse.paho.client.mqttv3.MqttException;

import java.lang.ref.WeakReference;
import java.net.UnknownHostException;

import static de.dipf.edutec.thriller.experiencesampling.conf.Globals.loginScreenActive;
import static de.dipf.edutec.thriller.experiencesampling.conf.Globals.useOffline;
import static de.dipf.edutec.thriller.experiencesampling.sensorservice.AccountConnector.checkPortAndHost;
import static org.eclipse.paho.client.mqttv3.MqttException.*;

/**
 * Activity for both the login and the credential modification process. {@link LoginTask} is executed after the user
 * submitted their credentials. If the login task is successful, the credentials are stored with {@link AccountManager}.
 * This activity is ought to be initiated by
 * {@link AccountAuthenticator#addAccount(AccountAuthenticatorResponse, String, String, String[], Bundle)} or
 * {@link AccountAuthenticator#updateCredentials(AccountAuthenticatorResponse, Account, String, Bundle)}.
 *
 */
public class LoginActivity extends AccountAuthenticatorActivity {

    public final static String ARG_ACCOUNT_TYPE = "ACCOUNT_TYPE";
    public final static String ARG_AUTH_TYPE = "AUTH_TYPE";
    public final static String ARG_ACCOUNT_NAME = "ACCOUNT_NAME";
    public final static String ARG_IS_ADDING_NEW_ACCOUNT = "IS_ADDING_ACCOUNT";

    public static final String KEY_ERROR_MESSAGE = "ERR_MSG";
    public static final String KEY_ERROR_CODE = "ERR_CODE";
    public final static String PARAM_USER_PASS = "USER_PASS";
    private static final String ERROR_UNKNOWN_HOST = "UNKNOWN_HOST";

    private final int REQ_SIGNUP = 1;

    private static final String TAG = LoginActivity.class.getSimpleName();

    private MqttService mqttService;
    private String host;
    private String port;

    @Override
    protected void onResume() {
        super.onResume();
        loginScreenActive = true;
        if (mqttService.isConnected() && getIntent().getBooleanExtra(ARG_IS_ADDING_NEW_ACCOUNT, false)) {
            redirectToMain();
            finish();
        }
    }

    private void redirectToMain() {
        startActivity(new Intent(this, MainActivity.class));
        loginScreenActive = false;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "creating login activity");
        super.onCreate(savedInstanceState);
        this.mqttService = ((CustomApplication) getApplication()).getContext().getMqttService();
        setContentView(R.layout.act_login);

        Intent intent = getIntent();
        String accountName = intent.getStringExtra(ARG_ACCOUNT_NAME);

        if (accountName != null) {
            ((TextInputLayout)findViewById(R.id.accountName)).getEditText().setText(accountName);
        }

        Button cancel = findViewById(R.id.cancel);
        Button submit = findViewById(R.id.submit);
        if(intent.getBooleanExtra(LoginActivity.ARG_IS_ADDING_NEW_ACCOUNT, false)) {
            cancel.setOnClickListener(v -> { redirectToMain(); useOffline = true; });
        } else {
            cancel.setOnClickListener(v -> finish());
            cancel.setText("Cancel");
            submit.setText("Update");
        }
        submit.setOnClickListener(v -> submit());
    }

    @Override
    protected void onStop() {
        super.onStop();
        loginScreenActive = false;
    }

    public void submit() {

        final String userName = ((TextInputLayout) findViewById(R.id.accountName)).getEditText().getText().toString();
        final String userPass = ((TextInputLayout) findViewById(R.id.accountPassword)).getEditText().getText().toString();

        final String accountType = getIntent().getStringExtra(ARG_ACCOUNT_TYPE);

        SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        host = sharedPreferences.getString("host", "");
        port = sharedPreferences.getString("port", "");

        if(!checkPortAndHost(this, false, getApplicationContext(), host.isEmpty(), port.isEmpty(), true)) {
            finish();
            return;
        }
        new LoginTask(this, userName, userPass, host, port, accountType).execute();
    }

    /**
     * <p>
     * Connects to the MQTT broker to verify user credentials.
     * </p>
     * <p>
     * If the connection was established successfully, it is disconnected subsequently and the account is stored through
     * {@link AccountManager#addAccountExplicitly(Account, String, Bundle)}. If the client is already connected, the
     * account is not added again. In both cases,
     * {@link AccountAuthenticatorActivity#setAccountAuthenticatorResult(Bundle)} is called with the successful result
     * of the authentication and the activity finishes. Any other type of error will not result in the activity to
     * finish but rather keep it active.
     * </p>
     */
    private static class LoginTask extends AsyncTask<String, Void, Intent> {
        private final WeakReference<LoginActivity> activityWeakReference;
        private final WeakReference<String> userNameWeak;
        private final WeakReference<String> passwordWeak;
        private final WeakReference<String> hostWeak;
        private final WeakReference<String> portWeak;
        private final WeakReference<String> accountTypeWeak;

        public LoginTask(LoginActivity loginActivity, String userName, String password, String host, String port, String accountType) {
            this.activityWeakReference = new WeakReference<>(loginActivity);
            this.userNameWeak = new WeakReference<>(userName);
            this.passwordWeak = new WeakReference<>(password);
            this.hostWeak = new WeakReference<>(host);
            this.portWeak = new WeakReference<>(port);
            this.accountTypeWeak = new WeakReference<>(accountType);
        }

        @Override
        protected Intent doInBackground(String... params) {
            LoginActivity loginActivity = activityWeakReference.get();
            String userName = userNameWeak.get();
            String userPass = passwordWeak.get();
            String host = hostWeak.get();
            String port = portWeak.get();
            String accountType = accountTypeWeak.get();

            if(loginActivity == null || loginActivity.isFinishing() ||
                    userName == null ||
                    userPass == null ||
                    host == null ||
                    port == null ||
                    accountType == null) {
                return new Intent();
            }
            Bundle data = new Bundle();

            try {
                if (loginActivity.mqttService.isConnected()) {
                    loginActivity.mqttService.disconnect();
                }
                loginActivity.mqttService.loginCheck(userName, userPass, String.format("ssl://%s:%s", host, port));
                loginActivity.mqttService.connect();
                data.putString(AccountManager.KEY_ACCOUNT_NAME, userName);
                data.putString(AccountManager.KEY_ACCOUNT_TYPE, accountType);
//                    data.putString(AccountManager.KEY_AUTHTOKEN, authtoken);
                data.putString(PARAM_USER_PASS, userPass);
                data.putString(AccountManager.KEY_USERDATA, "");
            } catch (MqttException e) {
                data.putInt(KEY_ERROR_CODE, e.getReasonCode());
                data.putString(KEY_ERROR_MESSAGE, e.getMessage());
                data.putBoolean(ERROR_UNKNOWN_HOST, e.getCause() instanceof UnknownHostException);
                Log.d(TAG, "mqtt error: " + e);
            }

            final Intent res = new Intent();
            res.putExtras(data);
            return res;
        }

        @Override
        protected void onPostExecute(Intent intent) {
            LoginActivity loginActivity = activityWeakReference.get();
            if(loginActivity == null || loginActivity.isFinishing()) {
                return;
            }
            if (intent.hasExtra(KEY_ERROR_MESSAGE)) {
                if(intent.hasExtra(KEY_ERROR_CODE)) {
                    int code = intent.getIntExtra(KEY_ERROR_CODE, 0);
                    if (code == REASON_CODE_CLIENT_CONNECTED) {
                        loginActivity.setAccountAuthenticatorResult(intent.getExtras());
                        loginActivity.setResult(RESULT_OK, intent);
                        loginActivity.finish();
                        return;
                    }
                }
                Toast.makeText(loginActivity.getBaseContext(), intent.getStringExtra(KEY_ERROR_MESSAGE), Toast.LENGTH_SHORT).show();
            } else {
                loginActivity.finishLogin(intent);
            }
        }

    }

    private void finishLogin(Intent intent) {
        String accountName = intent.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
        String accountPassword = intent.getStringExtra(PARAM_USER_PASS);

        final Account account = new Account(accountName, intent.getStringExtra(AccountManager.KEY_ACCOUNT_TYPE));

        AccountManager accountManager = AccountManager.get(this);

        if (getIntent().getBooleanExtra(ARG_IS_ADDING_NEW_ACCOUNT, false)) {
            accountManager.addAccountExplicitly(account, accountPassword, null);
        } else {
            accountManager.setPassword(account, accountPassword);
        }

        setAccountAuthenticatorResult(intent.getExtras());
        setResult(RESULT_OK, intent);
        finish();
    }
}
