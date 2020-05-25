package de.dipf.edutec.thriller.experiencesampling.activities;

import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.widget.TextView;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import de.dipf.edutec.thriller.experiencesampling.R;
import de.dipf.edutec.thriller.experiencesampling.messageservice.Receiver;

public class MainActivity extends WearableActivity {

    private TextView mTextView;

    @Override
    public void onRestart() {
        super.onRestart();
        System.out.println("On Restart is called");
    }

    @Override
    public void onResume() {
        super.onResume();
        System.out.println("onResume is called");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Enables Always-on
        setAmbientEnabled();

        mTextView = (TextView) findViewById(R.id.text);
        mTextView.setText("Hello Dear");

        IntentFilter newFilter = new IntentFilter(Intent.ACTION_SEND);
        Receiver messageReceiver = new Receiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, newFilter);

        System.out.println("Receiver registered");
    }
}
