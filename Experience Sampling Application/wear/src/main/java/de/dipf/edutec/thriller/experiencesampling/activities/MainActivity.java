
package de.dipf.edutec.thriller.experiencesampling.activities;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import de.dipf.edutec.thriller.experiencesampling.R;
import de.dipf.edutec.thriller.experiencesampling.messageservice.Receiver;
import de.dipf.edutec.thriller.experiencesampling.messageservice.SendMessageWear;
import de.dipf.edutec.thriller.experiencesampling.sensors.Helper;
import de.dipf.edutec.thriller.experiencesampling.sensors.SensorDataService;

import java.util.Optional;

import static de.dipf.edutec.thriller.experiencesampling.sensors.SensorDataService.WEAR_WAKELOCKTAG;

public class MainActivity extends WearableActivity {

    private boolean running = false;

    private static final String TAG = "wear:" + MainActivity.class.getSimpleName();
    private Button startButton;
    private Button stopButton;

    @Override
    public void onRestart() {
        super.onRestart();
        setRunning(Helper.accelerometerListener != null);
        Log.d(TAG, "onResume called");
    }

    @Override
    public void onResume() {
        super.onResume();
        setRunning(Helper.accelerometerListener != null);
        Log.d(TAG, "onResume called");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate called");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Enables Always-on
        setAmbientEnabled();

        Optional.ofNullable((PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE))
                .map(pm -> pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WEAR_WAKELOCKTAG))
                .ifPresent(PowerManager.WakeLock::acquire);

        startButton = Optional.ofNullable((Button) findViewById(R.id.bt_main_startSession))
                .orElseThrow(() -> new RuntimeException(String.format("Button %s not found.", R.id.bt_main_startSession)));

        stopButton = Optional.ofNullable((Button) findViewById(R.id.bt_main_stopSession))
                .orElseThrow(() -> new RuntimeException(String.format("Button %s not found.", R.id.bt_main_stopSession)));

        setRunning(Helper.accelerometerListener != null);

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

        LocalBroadcastManager.getInstance(this).registerReceiver(
                new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        setRunning(false);
                    }
                }, new IntentFilter("sensor-service-destroyed"));

        // Ability to Receive Messages from the MessageService ( WearableListener )
        IntentFilter newFilter = new IntentFilter(Intent.ACTION_SEND);
        Receiver messageReceiver = new Receiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, newFilter);

        System.out.println("Receiver registered");

        // If we restarted our Application via handheld we have to send an acknowledment
        Boolean isIntent = getIntent().getBooleanExtra("bool", false);
        if (isIntent) {
            SendMessageWear sendMessageWear = new SendMessageWear(this);
            sendMessageWear.sendAck("/toHandheld/Test", getIntent().getStringExtra("message"));
        }

        createNotificationChannel();

    }

    private void setRunning(boolean running) {
        Log.d(TAG, "setting running to " + running);
        this.running = running;
        stopButton.setEnabled(running);
        startButton.setEnabled(!running);
    }

    private void addButtonOnClickListener(View.OnClickListener c, Button button) {
        button
                .setOnClickListener(c);
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

    public void findGUIElements() {

    }
}
