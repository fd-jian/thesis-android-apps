package de.dipf.edutec.thriller.experiencesampling.messageservice;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.core.app.RemoteInput;

import de.dipf.edutec.thriller.experiencesampling.activities.MainActivity;
import de.dipf.edutec.thriller.experiencesampling.messagestruct.MyMessage;

public class ReplyService extends IntentService {

    public ReplyService(){
        super("ReplySerive");

    }


    @Override
    protected void onHandleIntent(@Nullable Intent intent) {

        if(intent.hasExtra("test")){
            if(intent.getStringExtra("test").equals("justStartAct")){
                System.out.println("RECIEVED JUST START ACT");

                Intent intent1 = new Intent();
                intent1.setClassName(this.getPackageName(), MainActivity.class.getName());
                intent1.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent1.putExtra("bool",true);
                intent1.putExtra("message",intent.getStringExtra("receivedMessageFromHandheld"));
                this.startActivity(intent1);
            }
            if(intent.getStringExtra("test").equals("textQuestion")){
                CharSequence directReply = getMessageText(intent);
                if (directReply != null) {
                    System.out.println("GOT MESSAGE: " + directReply);

                    MyMessage myMessage = MyMessage.decodeMessage(intent.getStringExtra("receivedMessageFromHandheld"));
                    myMessage.setUserAnswer(String.valueOf(directReply));

                    SendMessageWear sendMessageWear = new SendMessageWear(this);
                    sendMessageWear.sendAck("/toHandheld/Test", myMessage.encodeMessage());


                }
            }

            if(intent.getStringExtra("test").equals("choices")){
                Bundle remoteInput = RemoteInput.getResultsFromIntent(intent);
                if(remoteInput != null){
                    CharSequence answer = remoteInput.getCharSequence("key_choice_reply");
                    MyMessage myMessage = MyMessage.decodeMessage(intent.getStringExtra("receivedMessageFromHandheld"));
                    myMessage.setUserAnswer(String.valueOf(answer));
                    SendMessageWear sendMessageWear = new SendMessageWear(this);
                    sendMessageWear.sendAck("/toHandheld/Test", myMessage.encodeMessage());
                }








            }
        }





    }


    private CharSequence getMessageText(Intent intent) {
        // Decode the reply text
        Bundle remoteInput = RemoteInput.getResultsFromIntent(intent);
        if (remoteInput != null) {
            return remoteInput.getCharSequence("key_text_reply");
        }
        return null;
    }

    }

