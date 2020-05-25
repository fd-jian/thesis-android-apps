package de.dipf.edutec.thriller.experiencesampling.activities;

import android.content.Context;
import android.os.Bundle;
import android.view.Window;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.NodeClient;
import com.google.android.gms.wearable.Wearable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import de.dipf.edutec.thriller.experiencesampling.R;
import de.dipf.edutec.thriller.experiencesampling.dialogs.ProgressDialog;

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


        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);

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

            this.connectedBluetoothDevices = new ArrayList<String>();
            this.connectedBluetoothValues = new ArrayList<String>();

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