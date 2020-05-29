package de.dipf.edutec.thriller.experiencesampling.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import de.dipf.edutec.thriller.experiencesampling.R;
import de.dipf.edutec.thriller.experiencesampling.messageservice.MessagesSingleton;
import de.dipf.edutec.thriller.experiencesampling.messageservice.Receiver;
import de.dipf.edutec.thriller.experiencesampling.messageservice.SendMessage;

import de.dipf.edutec.thriller.messagestruct.MyMessage;
import de.dipf.edutec.thriller.messagestruct.OnSuccessSendPair;
import pl.droidsonroids.gif.GifDrawable;
import pl.droidsonroids.gif.GifImageView;

public class TestingActivity extends AppCompatActivity implements View.OnClickListener {

    // MessageSingleton
    MessagesSingleton messagesSingleton;

    // PATHS
    private String PATH_SMARTWATCH_TEST;
    Button bt_start_act, bt_notify_1, bt_notify_2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_testing);

        findGUIElements();

        messagesSingleton = MessagesSingleton.getInstance();


    }



    public void findGUIElements(){
        bt_start_act = findViewById(R.id.bt_testing_startAct);
        bt_start_act.setOnClickListener(this);

        bt_notify_1 = findViewById(R.id.bt_testing_sendNotification);
        bt_notify_1.setOnClickListener(this);

        bt_notify_2 = findViewById(R.id.bt_testing_sendNotification1);
        bt_notify_2.setOnClickListener(this);

        PATH_SMARTWATCH_TEST = getResources().getString(R.string.PATH_TOSMARTWATCH_TEST);


    }

    @Override
    public void onClick(View v) {

        switch(v.getId()){
            case R.id.bt_testing_startAct:
                callStartSmartwatchActivity();
                break;

            case R.id.bt_testing_sendNotification:
                callSendNotification();
                break;


            case R.id.bt_testing_sendNotification1:
                callSendNotificationChoices();
                break;
            default:
                break;
        }

    }

    public void callStartSmartwatchActivity(){

        GifImageView gif = findViewById(R.id.iv_testing_startAct_gif);
        gif.setVisibility(View.GONE);

        ImageView iv = findViewById(R.id.iv_testing_startAct);
        iv.setVisibility(View.GONE);


        final MyMessage myMessage = new MyMessage();
        myMessage.setStartActivity(true);
        myMessage.setRestDefault();

        messagesSingleton.registerListener(new MessagesSingleton.OnSuccessListener() {
            @Override
            public OnSuccessSendPair onSuccessStateChange(OnSuccessSendPair onSuccessSendPair) {

                if(onSuccessSendPair.getSuccess() == true && myMessage.getUuid().equals(onSuccessSendPair.getUuid())){

                    GifImageView gif = findViewById(R.id.iv_testing_startAct_gif);
                    gif.setVisibility(View.VISIBLE);
                    messagesSingleton.unregisterListenerOnSucc();

                } else {

                    if(onSuccessSendPair.getSuccess() == false && myMessage.getUuid().equals(onSuccessSendPair.getUuid())){

                        ImageView iv = findViewById(R.id.iv_testing_startAct);
                        iv.setImageResource(R.drawable.icon_error);
                        messagesSingleton.unregisterListenerOnSucc();
                    }

                }
                return null;
            }

        });

        messagesSingleton.registerListener(new MessagesSingleton.Listener() {
            @Override
            public String onStateChange(String uuid) {

                System.out.println("onStateChange is called " + uuid + "  " + myMessage.getUuid());

                if(myMessage.getUuid().equals(uuid)){
                    GifImageView gif = findViewById(R.id.iv_testing_startAct_gif);
                    gif.setVisibility(View.GONE);

                    ImageView iv = findViewById(R.id.iv_testing_startAct);
                    iv.setImageResource(R.drawable.icon_correct);
                    iv.setVisibility(View.VISIBLE);
                    messagesSingleton.unregisterListener();
                }

                return null;
            }
        });

                System.out.println(myMessage.encodeMessage());

        SendMessage messageService = new SendMessage(this);
        messageService.sendMessage(this.PATH_SMARTWATCH_TEST,myMessage);


    }
    public void callSendNotification(){
        ImageView iv = findViewById(R.id.iv_testing_sendNotification);
        GifImageView giv = findViewById(R.id.iv_testing_sendNotification_gif);

        iv.setVisibility(View.GONE);
        giv.setVisibility(View.GONE);

        final MyMessage myMessage = new MyMessage();
        myMessage.setStartActivity(false);
        myMessage.setMsgQuestion("How are you?");
        myMessage.setMsgAnswers(new String[]{});
        myMessage.setMsgOrigin(1);

        messagesSingleton.registerListener(new MessagesSingleton.OnSuccessListener() {
            @Override
            public OnSuccessSendPair onSuccessStateChange(OnSuccessSendPair onSuccessSendPair) {

                if(onSuccessSendPair.getSuccess() == true && myMessage.getUuid().equals(onSuccessSendPair.getUuid())){

                    GifImageView gif = findViewById(R.id.iv_testing_sendNotification_gif);
                    gif.setVisibility(View.VISIBLE);
                    messagesSingleton.unregisterListenerOnSucc();

                } else {

                    if(onSuccessSendPair.getSuccess() == false && myMessage.getUuid().equals(onSuccessSendPair.getUuid())){

                        ImageView iv = findViewById(R.id.iv_testing_sendNotification);
                        iv.setImageResource(R.drawable.icon_error);
                        messagesSingleton.unregisterListenerOnSucc();
                    }

                }
                return null;
            }

        });

        messagesSingleton.registerListener(new MessagesSingleton.Listener() {
            @Override
            public String onStateChange(String uuid) {

                if(myMessage.getUuid().equals(uuid)){
                    GifImageView gif = findViewById(R.id.iv_testing_sendNotification_gif);
                    gif.setVisibility(View.GONE);

                    ImageView iv = findViewById(R.id.iv_testing_sendNotification);
                    iv.setImageResource(R.drawable.icon_correct);
                    iv.setVisibility(View.VISIBLE);
                    messagesSingleton.unregisterListener();

                }
                return null;
            }
        });

        SendMessage messageService = new SendMessage(this);
        messageService.sendMessage(this.PATH_SMARTWATCH_TEST,myMessage);
        System.out.println("SEND ONE");


    }
    public void callSendNotificationChoices(){
        ImageView iv = findViewById(R.id.iv_testing_sendNotification1);
        GifImageView giv = findViewById(R.id.iv_testing_sendNotification1_gif);

        iv.setVisibility(View.GONE);
        giv.setVisibility(View.GONE);

        final MyMessage myMessage = new MyMessage();
        myMessage.setStartActivity(false);
        myMessage.setMsgQuestion("One, two or three?");
        myMessage.setMsgAnswers(new String[]{"1","2","3"});
        myMessage.setMsgOrigin(1);

        messagesSingleton.registerListener(new MessagesSingleton.OnSuccessListener() {
            @Override
            public OnSuccessSendPair onSuccessStateChange(OnSuccessSendPair onSuccessSendPair) {

                if(onSuccessSendPair.getSuccess() == true && myMessage.getUuid().equals(onSuccessSendPair.getUuid())){

                    GifImageView gif = findViewById(R.id.iv_testing_sendNotification1_gif);
                    gif.setVisibility(View.VISIBLE);

                    messagesSingleton.unregisterListenerOnSucc();

                } else {

                    if(onSuccessSendPair.getSuccess() == false && myMessage.getUuid().equals(onSuccessSendPair.getUuid())){

                        ImageView iv = findViewById(R.id.iv_testing_sendNotification1);
                        iv.setImageResource(R.drawable.icon_error);

                        messagesSingleton.unregisterListenerOnSucc();
                    }

                }
                return null;
            }

        });

        messagesSingleton.registerListener(new MessagesSingleton.Listener() {
            @Override
            public String onStateChange(String uuid) {

                if(myMessage.getUuid().equals(uuid)){
                    GifImageView gif = findViewById(R.id.iv_testing_sendNotification1_gif);
                    gif.setVisibility(View.GONE);

                    ImageView iv = findViewById(R.id.iv_testing_sendNotification1);
                    iv.setImageResource(R.drawable.icon_correct);
                    iv.setVisibility(View.VISIBLE);
                    messagesSingleton.unregisterListener();
                }
                return null;
            }
        });

        SendMessage messageService = new SendMessage(this);
        messageService.sendMessage(this.PATH_SMARTWATCH_TEST,myMessage);

    }
}
