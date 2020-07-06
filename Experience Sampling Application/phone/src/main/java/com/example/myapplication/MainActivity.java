package com.example.myapplication;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private BroadcastReceiver updateUIReciever;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(view -> Snackbar
                        .make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show());

        // add basic layout with textview to update later
        LinearLayout linearLayout = new LinearLayout(this);
        setContentView(linearLayout);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        TextView accelerometerDataView = new TextView(this);
        accelerometerDataView.setText("Waiting for data to be received...");
        linearLayout.addView(accelerometerDataView);

        TextView messageCountView = new TextView(this);
        linearLayout.addView(messageCountView);

        TextView secondsElapsedView = new TextView(this);
        linearLayout.addView(secondsElapsedView);

        TextView messagesPerSecondView = new TextView(this);
        linearLayout.addView(messagesPerSecondView);

        // Listen to broadcast from data layer service
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.hello.action");
        updateUIReciever = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // update UI
                String accelerometerData = intent.getStringExtra(DataLayerListenerService.ACCELEROMETER);
                Log.v(TAG, accelerometerData);
                accelerometerDataView.setText(accelerometerData);

                String messageCount = String.format("%d messages received",
                        Integer.parseInt(intent.getStringExtra(DataLayerListenerService.MESSAGE_COUNT)));
                Log.v(TAG, messageCount);
                messageCountView.setText(messageCount);

                String secondsElapsed = String.format("%d seconds elapsed",
                        Long.parseLong(intent.getStringExtra(DataLayerListenerService.SECONDS_ELAPSED)));
                Log.v(TAG, secondsElapsed);
                secondsElapsedView.setText(secondsElapsed);

                String messagesPerSecond = String.format("%.2f messages/second",
                        Float.parseFloat(intent.getStringExtra(DataLayerListenerService.MESSAGES_PER_SECOND)));
                Log.v(TAG, messagesPerSecond);
                messagesPerSecondView.setText(messagesPerSecond);
            }
        };

        registerReceiver(updateUIReciever, filter);

        // start service to trigger periodical ui updates. error if app starts inactive/locked screen!
        Intent intent = new Intent(this, DataLayerListenerService.class);
        startService(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(updateUIReciever);
    }
}
