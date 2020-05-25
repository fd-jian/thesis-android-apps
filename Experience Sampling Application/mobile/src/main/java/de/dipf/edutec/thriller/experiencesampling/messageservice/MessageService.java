package de.dipf.edutec.thriller.experiencesampling.messageservice;

import android.util.Log;

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
    }

}
