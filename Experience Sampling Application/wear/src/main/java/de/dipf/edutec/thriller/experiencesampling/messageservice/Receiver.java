package de.dipf.edutec.thriller.experiencesampling.messageservice;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import de.dipf.edutec.thriller.experiencesampling.activities.MainActivity;
import de.dipf.edutec.thriller.messagestruct.MyMessage;

public class Receiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String cipher = intent.getStringExtra("message");
        MyMessage myMessage = MyMessage.decodeMessage(cipher);

        // Interpretierung der MyMessage
        if(myMessage.getStartActivity() == true){
            Intent intent1 = new Intent();
            intent1.setClassName(context.getPackageName(), MainActivity.class.getName());
            intent1.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent1);

            SendMessageWear sendMessageWear = new SendMessageWear(context);
            sendMessageWear.sendAck("/toHandheld","test");

        }

    }
}
