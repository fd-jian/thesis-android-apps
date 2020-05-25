package de.dipf.edutec.thriller.experiencesampling.messageservice;

import android.content.Intent;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

public class MessageService extends WearableListenerService {

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {

        System.out.println("Registered Event");
        System.out.println("Path: " + messageEvent.getPath());

        if (messageEvent.getPath().equals("/toSmartwatch/Test")) {

            System.out.println("TO THIS POINT");
            final String message = new String(messageEvent.getData());
            Intent messageIntent = new Intent();
            messageIntent.setAction(Intent.ACTION_SEND);
            messageIntent.putExtra("message",message);
            LocalBroadcastManager.getInstance(this).sendBroadcast(messageIntent);
            System.out.println("I DID SEND");

            /**
            final String message = new String(messageEvent.getData());
            Intent messageIntent = new Intent();
            messageIntent.setAction(Intent.ACTION_SEND);
            messageIntent.putExtra("message", message);
            LocalBroadcastManager.getInstance(this).sendBroadcast(messageIntent);
             **/
        }
        else {
            super.onMessageReceived(messageEvent);
        }
    }

}