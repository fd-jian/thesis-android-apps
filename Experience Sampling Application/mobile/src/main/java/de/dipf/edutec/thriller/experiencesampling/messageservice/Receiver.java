package de.dipf.edutec.thriller.experiencesampling.messageservice;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import de.dipf.edutec.thriller.messagestruct.MyMessage;

public class Receiver extends BroadcastReceiver {

    Context context;
    MessagesSingleton messagesSingleton;

    public Receiver(Context context){
        this.context = context;
        IntentFilter newFilter = new IntentFilter(Intent.ACTION_SEND);
        LocalBroadcastManager.getInstance(context).registerReceiver(this ,newFilter);
        messagesSingleton = MessagesSingleton.getInstance();


    }








    @Override
    public void onReceive(Context context, Intent intent) {

        System.out.println("MOBILE RECEIVER RECEIVED SOMETHING");
        MyMessage myMessage = MyMessage.decodeMessage(intent.getStringExtra("message"));


        if(messagesSingleton.existUUIDReceived(myMessage.getUuid()) == false){
            System.out.println(myMessage.toString());
            messagesSingleton.addMessageReceived(myMessage);
            System.out.println("ADDED MESSAGE TO MESSAGESINGLETON");
        }


    }

    public void unregisterReceiver(){
        LocalBroadcastManager.getInstance(context).unregisterReceiver(this);

    }

    // Variabel change Listener
    private Receiver.Listener mListener = null;
    public void registerListener(Receiver.Listener listener) {mListener = listener; }
    public interface Listener{
        void onStateChange();
    }
}
