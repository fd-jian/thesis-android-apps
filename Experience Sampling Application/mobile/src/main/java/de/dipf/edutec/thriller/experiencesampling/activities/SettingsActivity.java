package de.dipf.edutec.thriller.experiencesampling.activities;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import de.dipf.edutec.thriller.experiencesampling.R;
import de.dipf.edutec.thriller.experiencesampling.conf.CustomApplication;

import static de.dipf.edutec.thriller.experiencesampling.activities.MainActivity.ACCOUNT_TYPE;

public class SettingsActivity extends AppCompatActivity {

    public static final String TAG = String.valueOf(R.string.TAG_SETTINGS_ACTIVITY);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS);
        setContentView(R.layout.activity_settings);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings, new SettingsFragment())
                .commit();

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    public static class SettingsFragment extends PreferenceFragmentCompat implements Preference.OnPreferenceClickListener {

        public List<String> connectedBluetoothDevices;
        public List<String> connectedBluetoothValues;

        // To Change Preferences
        public ListPreference connectedDevicesList;
        private AccountManager accountManager;
        private Preference removeCredentials;

        private Account getAccount() {
            Account[] accountsByType = accountManager.getAccountsByType(ACCOUNT_TYPE);
            if (accountsByType.length == 0) {
                Log.e(TAG, "Account not found.");
                return null;
            }
            return accountsByType[0];
        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);

            accountManager = AccountManager.get(getContext());

            removeCredentials = findPreference("remove_credentials");
            Account account = getAccount();
            if(account == null) {
                removeCredentials.setEnabled(false);
            }

            // TODO: update credentials does not work (does not check apply new credentials)
            findPreference("edit_credentials").setOnPreferenceClickListener(preference -> {
                Account ac = getAccount();
                if (ac == null) {
                    Log.e(TAG,"Account not found.");
                    accountManager.addAccount(ACCOUNT_TYPE, null, null, null, getActivity(), null, null);
                    return true;
                }

                accountManager.updateCredentials(ac, null, null, getActivity(), null, null);
                removeCredentials.setEnabled(true);
                return true;
            });

            removeCredentials.setOnPreferenceClickListener(preference -> {
                accountManager.removeAccount(getAccount(), getActivity(), null, null);
                preference.setEnabled(false);
                disconnect();
                return true;
            });

            findPreference("host").setOnPreferenceClickListener(this::disconnect);
            findPreference("port").setOnPreferenceClickListener(this::disconnect);

            this.connectedDevicesList = findPreference(getResources().getString(R.string.key_connected_devices));
            this.connectedDevicesList.setOnPreferenceClickListener(this);
            setListConnectedDevices();
            this.connectedDevicesList.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    preference.setSummary(newValue.toString());
                    return false;
                }
            });

            ListPreference updateRateList = findPreference(getResources().getString(R.string.key_update_rate));
            updateRateList.setOnPreferenceClickListener(this);

            this.connectedBluetoothDevices = new ArrayList<>();
            this.connectedBluetoothValues = new ArrayList<>();
        }

        @Override
        public void onResume() {
            super.onResume();
            removeCredentials.setEnabled(getAccount() != null);
        }

        private boolean disconnect(Preference preference) {
            return disconnect();
        }

        private boolean disconnect() {
            ((CustomApplication) getActivity().getApplication()).getContext().getMqttService().disconnect();
            return true;
        }

        @Override
        public boolean onPreferenceClick(Preference preference){
            if(preference.getKey().equals(getResources().getString(R.string.key_connected_devices))){

                connectedDevicesList.setEntries(connectedBluetoothDevices.toArray(new CharSequence[connectedBluetoothDevices.size()]));
                connectedDevicesList.setEntryValues(connectedBluetoothValues.toArray(new CharSequence[connectedBluetoothValues.size()]));

            } else if(preference.getKey().equals(getResources().getString(R.string.key_update_rate))){

            }
            return true;

        }



        public void setListConnectedDevices() {

            Thread t = new Thread(){
                public void run(){



                    Task<List<Node>> connectedNodes = Wearable.getNodeClient(getContext()).getConnectedNodes();
                    try {
                        List<Node> nodes = Tasks.await(connectedNodes);
                        int i = 0;
                        for(Node node : nodes){
                            i += 1;
                            connectedBluetoothDevices.add(node.getDisplayName());
                            connectedBluetoothValues.add(String.valueOf(i-1));
                        }
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }


            };

            t.start();
        }
    }
}