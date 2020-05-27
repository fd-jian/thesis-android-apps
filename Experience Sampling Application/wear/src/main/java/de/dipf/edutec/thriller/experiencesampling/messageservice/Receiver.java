package de.dipf.edutec.thriller.experiencesampling.messageservice;



import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;


import androidx.core.app.NotificationCompat;
import androidx.core.app.RemoteInput;

import de.dipf.edutec.thriller.experiencesampling.R;
import de.dipf.edutec.thriller.experiencesampling.activities.MainActivity;
import de.dipf.edutec.thriller.messagestruct.MyMessage;

import static android.app.PendingIntent.FLAG_CANCEL_CURRENT;

public class Receiver extends BroadcastReceiver {

    public static int flagCancelCurrent = FLAG_CANCEL_CURRENT;

    @Override
    public void onReceive(Context context, Intent intent) {

        String CHANNEL_ID = context.getResources().getString(R.string.notiChannelID);

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        String cipher = intent.getStringExtra("message");
        MyMessage myMessage = MyMessage.decodeMessage(cipher);

        // Interpretierung der MyMessage
        if(myMessage.getStartActivity() == true && myMessage.getQuestion().equals("0")){

            System.out.println("SYSTEM RECEIVER RECEIVED RESTART");

            Intent directReplyIntent = new Intent(context,ReplyService.class);
            directReplyIntent.putExtra("receivedMessageFromHandheld",cipher);
            directReplyIntent.putExtra("test","justStartAct");

            PendingIntent directReplyPendingIntent = PendingIntent.getService(context, 0, directReplyIntent, flagCancelCurrent);

            Notification notification = new NotificationCompat.Builder(context,CHANNEL_ID)
                    .setAutoCancel(true)
                    .setContentTitle("Activity Restart")
                    .setContentText("Activity Restarter")
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .addAction(R.mipmap.ic_launcher,"Start Activity",directReplyPendingIntent)
                    .build();

            notificationManager.notify(0,notification);

        }

        if(myMessage.getStartActivity() == false &&  myMessage.getNumAnsw() == 0){


            Intent directReplyIntent = new Intent(context, ReplyService.class);
            directReplyIntent.putExtra("receivedMessageFromHandheld",cipher);
            directReplyIntent.putExtra("test","textQuestion");

            PendingIntent directReplyPendingIntent = PendingIntent.getService(context, 0, directReplyIntent, flagCancelCurrent);

            String KEY_TEXT_REPLY = "key_text_reply";

            RemoteInput remoteInput = new RemoteInput.Builder(KEY_TEXT_REPLY)
                    .setLabel("Type Message").build();



            NotificationCompat.Action action = new NotificationCompat.Action.Builder(R.mipmap.ic_launcher,"Reply",directReplyPendingIntent)
                    .addRemoteInput(remoteInput).build();


            System.out.println("NOTIFICATION GERNERATION IS CALLED");



            Notification notifcation = new NotificationCompat.Builder(context,CHANNEL_ID)
                    .setAutoCancel(true)
                    .setContentTitle("Text Question")
                    .setContentText(myMessage.getQuestion())
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .addAction(action)
                    .build();

            notificationManager.notify(0,notifcation);

        }

    }
}
