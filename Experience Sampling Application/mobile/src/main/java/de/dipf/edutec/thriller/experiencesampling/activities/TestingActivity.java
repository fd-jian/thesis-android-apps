package de.dipf.edutec.thriller.experiencesampling.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import de.dipf.edutec.thriller.experiencesampling.R;
import de.dipf.edutec.thriller.experiencesampling.messageservice.SendMessage;

import de.dipf.edutec.thriller.messagestruct.MyMessage;

public class TestingActivity extends AppCompatActivity implements View.OnClickListener {


    private String PATH_SMARTWATCH_TEST;

    Button bt_start_act;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_testing);
        this.PATH_SMARTWATCH_TEST = getResources().getString(R.string.PATH_TOSMARTWATCH_TEST);
        findGUIElements();
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

        MyMessage myMessage = new MyMessage();
        myMessage.setStartActivity(true);
        myMessage.setRestDefault();

        System.out.println(myMessage.encodeMessage());


        SendMessage messageService = new SendMessage(this);
        messageService.sendMessage(this.PATH_SMARTWATCH_TEST,myMessage);


        // Debug Spielerein
        /**
        GifImageView preloader = findViewById(R.id.iv_testing_startAct_gif);
        ImageView iv = findViewById(R.id.iv_testing_startAct);
        if(preloader.getVisibility() == View.VISIBLE){
            preloader.setVisibility(View.GONE);
            Random rand = new Random();
            int randomNum = rand.nextInt((100-0) + 1) + 0;
            if(randomNum < 50) iv.setImageResource(R.drawable.icon_correct);
            else iv.setImageResource(R.drawable.icon_error);
            iv.setVisibility(View.VISIBLE);
        }else{
            if(iv.getVisibility() == View.VISIBLE) iv.setVisibility(View.GONE);
            preloader.setVisibility(View.VISIBLE);
        }
         **/
    }
}
