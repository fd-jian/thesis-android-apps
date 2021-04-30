
package de.dipf.edutec.thriller.experiencesampling.activities;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.*;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import de.dipf.edutec.thriller.experiencesampling.R;
import de.dipf.edutec.thriller.experiencesampling.messageservice.Receiver;
import de.dipf.edutec.thriller.experiencesampling.messageservice.SendMessageWear;
import de.dipf.edutec.thriller.experiencesampling.sensorservice.SensorDataService;

import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;

public class MainActivity extends WearableActivity {

    private static final String TAG = "wear:" + MainActivity.class.getSimpleName();
    private Button startButton;
    private Button stopButton;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.OnSharedPreferenceChangeListener onSharedPreferenceChangeListener;

    @Override
    public void onRestart() {
        super.onRestart();
        Log.d(TAG, "onResume called");
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        setRunning(sharedPreferences.getBoolean("running", false));
        Log.d(TAG, "onResume called");
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate called");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Enables Always-on
        setAmbientEnabled();

        startButton = Optional.ofNullable((Button) findViewById(R.id.bt_main_startSession))
                .orElseThrow(() -> new RuntimeException(String.format("Button %s not found.", R.id.bt_main_startSession)));

        stopButton = Optional.ofNullable((Button) findViewById(R.id.bt_main_stopSession))
                .orElseThrow(() -> new RuntimeException(String.format("Button %s not found.", R.id.bt_main_stopSession)));

        Intent intent = new Intent(getApplicationContext(), SensorDataService.class);
        intent.setClassName(getPackageName(), SensorDataService.class.getName());
        // Handling our GUI Elements

        startButton.setOnClickListener(v -> {
            setRunning(true);
            Log.i(TAG, "start sensor streaming");
            startService(intent);
        });

        stopButton.setOnClickListener(v -> {
            // Start Smartphone Activity + Start Websocket Backend.
            setRunning(false);
            Log.i(TAG, "stop sensor streaming");
            stopService(intent);
        });

        LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(this);
        broadcastManager.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String last_second = String.valueOf(intent.getIntExtra(SensorDataService.LAST_SECOND_INTENT_EXTRA, 0));
                String per_second = String.valueOf(Math.round(intent.getFloatExtra(SensorDataService.RECORDS_PER_SECOND_INTENT_EXTRA, 0) * 100)/(float) 100);
//                Log.d(TAG, "last second: " + last_second);
//                Log.d(TAG, "per second: " + per_second);
                Optional.ofNullable((TextView) findViewById(R.id.last_second_text))
                        .ifPresent(textView -> textView.setText(last_second));
                Optional.ofNullable((TextView) findViewById(R.id.average_text))
                        .ifPresent(textView -> textView.setText(per_second));
            }
        }, new IntentFilter(SensorDataService.UPDATED_ANALYTICS_INTENT_ACTION));

        broadcastManager.registerReceiver(
                new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        setRunning(sharedPreferences.getBoolean("running", false));
                    }
                }, new IntentFilter("sensor-service-destroyed"));

        // Ability to Receive Messages from the MessageService ( WearableListener )
        IntentFilter newFilter = new IntentFilter(Intent.ACTION_SEND);
        Receiver messageReceiver = new Receiver();
        broadcastManager.registerReceiver(messageReceiver, newFilter);

        System.out.println("Receiver registered");

        // If we restarted our Application via handheld we have to send an acknowledment
        Boolean isIntent = getIntent().getBooleanExtra("bool", false);
        if (isIntent) {
            SendMessageWear sendMessageWear = new SendMessageWear(this);
            sendMessageWear.sendAck("/toHandheld/Test", getIntent().getStringExtra("message"));
        }

        createNotificationChannel();

        sharedPreferences = getSharedPreferences(getPackageName(), MODE_PRIVATE);
        boolean running = sharedPreferences.getBoolean("running", false);
        boolean connected = sharedPreferences.getBoolean("mobile_connected", false);

        setRunning(running);
        updateStatus(running, connected);

        onSharedPreferenceChangeListener = (sprefs, key) -> {
            Log.d(TAG, String.format("on shared preferences change: %s", key));
            boolean run;
            boolean con;
            switch (key) {
                case "mobile_connected":
                    con = sprefs.getBoolean(key, false);
                    run = sprefs.getBoolean("running", false);
                    Log.d(TAG, "connected changed to " + con);
                    updateStatus(run, con);
                case "running":
                    con = sprefs.getBoolean("mobile_connected", true);
                    run = sprefs.getBoolean(key, false);
                    Log.d(TAG, "running changed to " + run);
                    setRunning(run);
                    updateStatus(run, con);
            }
        };
        sharedPreferences.registerOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener);
    }

    private void updateStatus(boolean running, boolean connected) {
        Log.d(TAG, String.format("updating status ->  running: %s, connected: %s", running, connected));
        ImageView icon = Objects.requireNonNull(findViewById(R.id.connection_icon));
        TextView status = Objects.requireNonNull(findViewById(R.id.connection_status));
        if (running) {
            String phone = "Phone %s";
            Resources resources = getResources();
            if (connected) {
                icon.setImageResource(R.drawable.icon_connected);
                status.setText(String.format(phone, "connected"));
                status.setTextColor(resources.getColor(R.color.colorAccent, null));
            } else {
                icon.setImageResource(R.drawable.icon_disconnected);
                status.setText(String.format(phone, "disconnected"));
                status.setTextColor(resources.getColor(R.color.red_ok, null));
            }
            icon.setVisibility(View.VISIBLE);
            status.setVisibility(View.VISIBLE);
        } else {
            icon.setVisibility(View.INVISIBLE);
            status.setVisibility(View.INVISIBLE);
        }
    }

    private void setRunning(boolean running) {
        Log.d(TAG, "setting running to " + running);
        stopButton.setEnabled(running);
        startButton.setEnabled(!running);
    }

    public void createNotificationChannel() {
        NotificationManager notificationManager = (NotificationManager)
                getSystemService(Context.NOTIFICATION_SERVICE);

        String channelID = getResources().getString(R.string.notiChannelID);
        String channelName = getResources().getString(R.string.notiChannelName);
        int importance = NotificationManager.IMPORTANCE_HIGH;

        @SuppressLint("WrongConstant") NotificationChannel notificationChannel = new NotificationChannel(channelID, channelName, importance);
        notificationChannel.enableLights(true);
        notificationChannel.setLightColor(Color.RED);

        notificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        notificationChannel.setShowBadge(true);
        notificationChannel.setVibrationPattern(new long[]{0, 1000, 500, 50,
                0, 1000, 500, 50,
                0, 1000, 500, 50});
        notificationChannel.enableVibration(true);
        notificationManager.createNotificationChannel(notificationChannel);

    }

}
