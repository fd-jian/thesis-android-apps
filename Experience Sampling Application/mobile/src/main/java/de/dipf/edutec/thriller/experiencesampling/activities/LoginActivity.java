package de.dipf.edutec.thriller.experiencesampling.activities;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import androidx.preference.PreferenceManager;
import com.google.android.material.textfield.TextInputLayout;
import de.dipf.edutec.thriller.experiencesampling.R;
import de.dipf.edutec.thriller.experiencesampling.conf.CustomApplication;
import de.dipf.edutec.thriller.experiencesampling.sensorservice.AccountConnector;
import de.dipf.edutec.thriller.experiencesampling.sensorservice.transport.MqttService;
import org.eclipse.paho.client.mqttv3.MqttException;

import java.net.UnknownHostException;

import static de.dipf.edutec.thriller.experiencesampling.sensorservice.AccountConnector.checkPortAndHost;
import static org.eclipse.paho.client.mqttv3.MqttException.*;

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

    private final String TAG = this.getClass().getSimpleName();

    private MqttService mqttService;
    private String host;
    private String port;

    @Override
    protected void onResume() {
        super.onResume();
        if (mqttService.isConnected() && getIntent().getBooleanExtra(ARG_IS_ADDING_NEW_ACCOUNT, false)) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "creating login activity");
        super.onCreate(savedInstanceState);
        this.mqttService = ((CustomApplication) getApplication()).getContext().getMqttService();
        setContentView(R.layout.act_login);

        String accountName = getIntent().getStringExtra(ARG_ACCOUNT_NAME);
//        mAuthTokenType = getIntent().getStringExtra(ARG_AUTH_TYPE);
//        if (mAuthTokenType == null)
//            mAuthTokenType = AccountGeneral.AUTHTOKEN_TYPE_FULL_ACCESS;

        if (accountName != null) {
            ((TextView)findViewById(R.id.accountName)).setText(accountName);
        }

        findViewById(R.id.submit).setOnClickListener(v -> submit());
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

        new AsyncTask<String, Void, Intent>() {

            @Override
            protected Intent doInBackground(String... params) {
                Bundle data = new Bundle();

                try {
                    if (mqttService.isConnected()) {
                        mqttService.disconnect();
                    }
                    mqttService.loginCheck(userName, userPass, String.format("ssl://%s:%s", host, port));
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
                if (intent.hasExtra(KEY_ERROR_MESSAGE)) {
                    if(intent.hasExtra(KEY_ERROR_CODE)) {
                        int code = intent.getIntExtra(KEY_ERROR_CODE, 0);
                        if (code == REASON_CODE_CLIENT_CONNECTED) {
                            setAccountAuthenticatorResult(intent.getExtras());
                            setResult(RESULT_OK, intent);
                            finish();
                            return;
                        } else {
                            boolean wrongPort = code == REASON_CODE_SERVER_CONNECT_ERROR;
                            boolean wrongHost = code == REASON_CODE_CLIENT_EXCEPTION && intent.getBooleanExtra(ERROR_UNKNOWN_HOST, false);

                            if (!checkPortAndHost(LoginActivity.this, false, getApplicationContext(), wrongHost, wrongPort, false))
                                return;
                        }
                    }
                    Toast.makeText(getBaseContext(), intent.getStringExtra(KEY_ERROR_MESSAGE), Toast.LENGTH_SHORT).show();
                } else {
                    finishLogin(intent);
                }
            }
        }.execute();
    }

    private void finishLogin(Intent intent) {
        String accountName = intent.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
        String accountPassword = intent.getStringExtra(PARAM_USER_PASS);

        final Account account = new Account(accountName, intent.getStringExtra(AccountManager.KEY_ACCOUNT_TYPE));

        AccountManager accountManager = AccountManager.get(this);

        if (getIntent().getBooleanExtra(ARG_IS_ADDING_NEW_ACCOUNT, false)) {
//            String authtoken = intent.getStringExtra(AccountManager.KEY_AUTHTOKEN);
//            String authtokenType = mAuthTokenType;

            // Creating the account on the device and setting the auth token we got
            // (Not setting the auth token will cause another call to the server to authenticate the user)
            accountManager.addAccountExplicitly(account, accountPassword, null);
//            accountManager.setAuthToken(account, authtokenType, authtoken);
        } else {
            accountManager.setPassword(account, accountPassword);
        }

        setAccountAuthenticatorResult(intent.getExtras());
        setResult(RESULT_OK, intent);
        finish();
    }
}
