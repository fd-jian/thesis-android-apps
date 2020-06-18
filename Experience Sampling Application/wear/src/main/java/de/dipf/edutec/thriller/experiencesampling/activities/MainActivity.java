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
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.security.acl.NotOwnerException;

import de.dipf.edutec.thriller.experiencesampling.R;
import de.dipf.edutec.thriller.experiencesampling.messageservice.Receiver;
import de.dipf.edutec.thriller.experiencesampling.messageservice.SendMessageWear;
import de.dipf.edutec.thriller.messagestruct.MyMessage;

public class MainActivity extends WearableActivity {

    private Button bt_main_startSession;

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

        // Handling our GUI Elements
        findGUIElements();

        // Ability to Receive Messages from the MessageService ( WearableListener )
        IntentFilter newFilter = new IntentFilter(Intent.ACTION_SEND);
        Receiver messageReceiver = new Receiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, newFilter);

        System.out.println("Receiver registered");

        // If we restarted our Application via handheld we have to send an acknowledment
        Boolean isIntent = getIntent().getBooleanExtra("bool",false);
        if(isIntent){
            SendMessageWear sendMessageWear = new SendMessageWear(this);
            sendMessageWear.sendAck("/toHandheld/Test", getIntent().getStringExtra("message"));
        }

        createNotificationChannel();

    }

    public void createNotificationChannel(){
        NotificationManager notificationManager = (NotificationManager)
                getSystemService(Context.NOTIFICATION_SERVICE);

        String channelID = getResources().getString(R.string.notiChannelID);
        String channelName = getResources().getString(R.string.notiChannelName);
        int importance = NotificationManager.IMPORTANCE_HIGH;

        @SuppressLint("WrongConstant") NotificationChannel notificationChannel = new NotificationChannel(channelID,channelName,importance);
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

    public void findGUIElements(){
        bt_main_startSession = findViewById(R.id.bt_main_startSession);
        bt_main_startSession.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO
                // Start Smartphone Activity + Start Websocket Backend.
            }
        });

    }
}
