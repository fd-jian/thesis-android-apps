package de.dipf.edutec.thriller.experiencesampling.messageservice;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class Receiver extends BroadcastReceiver {

    Context context;

    public Receiver(Context context){
        this.context = context;
        IntentFilter newFilter = new IntentFilter(Intent.ACTION_SEND);
        LocalBroadcastManager.getInstance(context).registerReceiver(this ,newFilter);
    }





    public void unregisterReceiver(){
        LocalBroadcastManager.getInstance(context).unregisterReceiver(this);

    }


    @Override
    public void onReceive(Context context, Intent intent) {

    }
}
