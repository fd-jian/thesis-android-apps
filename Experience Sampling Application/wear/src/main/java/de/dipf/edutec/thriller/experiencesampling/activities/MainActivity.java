package de.dipf.edutec.thriller.experiencesampling.activities;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.core.app.ShareCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.Optional;

import de.dipf.edutec.thriller.experiencesampling.R;
import de.dipf.edutec.thriller.experiencesampling.messageservice.Receiver;
import de.dipf.edutec.thriller.experiencesampling.messageservice.ReplyService;
import de.dipf.edutec.thriller.experiencesampling.messageservice.SendMessageWear;
import de.dipf.edutec.thriller.experiencesampling.sensors.SensorDataService;

public class MainActivity extends WearableActivity {

    private static final String TAG = "wear:" + MainActivity.class.getSimpleName().toLowerCase();

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

        Intent intent = new Intent(getApplicationContext(), SensorDataService.class);
        // Handling our GUI Elements
        addButtonOnClickListener(v -> {
            // Start Smartphone Activity + Start Websocket Backend.
            Log.i(TAG,"start sensor streaming");
            intent.putExtra(SensorDataService.SENSOR_ACTION_EXTRA, SensorDataService.START_ACTION);
            startService(intent);
        }, R.id.bt_main_startSession);

        addButtonOnClickListener((v) -> {
            // Start Smartphone Activity + Start Websocket Backend.
            Log.i(TAG,"stop sensor streaming");
            intent.putExtra(SensorDataService.SENSOR_ACTION_EXTRA, SensorDataService.STOP_ACTION);
            startService(intent);
        }, R.id.bt_main_stopSession);

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

    private void addButtonOnClickListener(View.OnClickListener c, int buttonId) {
        Optional.ofNullable((Button) findViewById(buttonId))
                .orElseThrow(() -> new RuntimeException(String.format("Button %s not found.", buttonId)))
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
