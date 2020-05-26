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

    Button bt_start_act;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_testing);

        this.PATH_SMARTWATCH_TEST = getResources().getString(R.string.PATH_TOSMARTWATCH_TEST);


        findGUIElements();

        messagesSingleton = MessagesSingleton.getInstance();
        Receiver receiver = new Receiver(this);

    }



    public void findGUIElements(){
        bt_start_act = findViewById(R.id.bt_testing_startAct);
        bt_start_act.setOnClickListener(this);



    }

    @Override
    public void onClick(View v) {

        switch(v.getId()){
            case R.id.bt_testing_startAct:
                callStartSmartwatchActivity();

                break;

            default:
                break;
        }

    }

    public void callStartSmartwatchActivity(){

        final MyMessage myMessage = new MyMessage();
        myMessage.setStartActivity(true);
        myMessage.setRestDefault();

        messagesSingleton.registerListener(new MessagesSingleton.OnSuccessListener() {
            @Override
            public OnSuccessSendPair onSuccessStateChange(OnSuccessSendPair onSuccessSendPair) {

                if(onSuccessSendPair.getSuccess() == true && myMessage.getUuid().equals(onSuccessSendPair.getUuid())){

                    GifImageView gif = findViewById(R.id.iv_testing_startAct_gif);
                    gif.setVisibility(View.VISIBLE);

                } else {

                    if(onSuccessSendPair.getSuccess() == false && myMessage.getUuid().equals(onSuccessSendPair.getUuid())){

                        ImageView iv = findViewById(R.id.iv_testing_startAct);
                        iv.setImageResource(R.drawable.icon_error);
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
                }

                return null;
            }
        });

                System.out.println(myMessage.encodeMessage());

        SendMessage messageService = new SendMessage(this);
        messageService.sendMessage(this.PATH_SMARTWATCH_TEST,myMessage);


    }
}
