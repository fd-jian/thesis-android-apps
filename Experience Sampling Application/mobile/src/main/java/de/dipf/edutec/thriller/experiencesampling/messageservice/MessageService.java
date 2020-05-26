package de.dipf.edutec.thriller.experiencesampling.messageservice;

import android.content.Intent;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

public class MessageService extends WearableListenerService {



    public MessageService(){}

    @Override
    public void onMessageReceived(MessageEvent messageEvent){
        Log.e("---------","----------");
        Log.e("General Listenerservice"," Handheld received anything");
        Log.e("Path: ", messageEvent.getPath());
        Log.e("Message",new String(messageEvent.getData()));
        Log.e("My current context", this.toString());
        Log.e("---------","----------");

        if(messageEvent.getPath().equals("/toHandheld/Test")){

            final String path = messageEvent.getPath();
            final String msg = new String(messageEvent.getData());

            Intent messageIntent = new Intent();
            messageIntent.setAction(Intent.ACTION_SEND);
            messageIntent.putExtra("message",msg);

            LocalBroadcastManager.getInstance(this).sendBroadcast(messageIntent);

        }



    }

}
