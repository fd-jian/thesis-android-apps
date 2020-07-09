package de.dipf.edutec.thriller.experiencesampling.activities;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceManager;

import android.app.ActivityOptions;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import de.dipf.edutec.thriller.experiencesampling.R;
import de.dipf.edutec.thriller.experiencesampling.messageservice.MessagesSingleton;
import de.dipf.edutec.thriller.experiencesampling.messageservice.Receiver;
import de.dipf.edutec.thriller.experiencesampling.messageservice.SendMessage;
import de.dipf.edutec.thriller.experiencesampling.messageservice.WebSocketService;
import de.dipf.edutec.thriller.messagestruct.MyMessage;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;
import pl.droidsonroids.gif.GifImageButton;
import pl.droidsonroids.gif.GifImageView;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {



    // GUI Elements
    GifImageButton dummy2;
    Button bt_settings, bt_database, bt_smartwatch, bt_testing, bt_info, bt_wearos;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS);
        setContentView(R.layout.activity_main);
        findGUIElements();

        Receiver receiver = new Receiver(this);
        startService(new Intent(this, WebSocketService.class));


    }





    // instantiate GUI Elements
    public void findGUIElements(){
        // Buttons
        bt_settings = (Button) findViewById(R.id.bt_main_setting);
        bt_settings.setOnClickListener(this);

        bt_database = (Button) findViewById(R.id.bt_main_database);
        bt_database.setOnClickListener(this);

        bt_testing = (Button) findViewById(R.id.bt_main_testing);
        bt_testing.setOnClickListener(this);

        bt_smartwatch = (Button) findViewById(R.id.bt_main_smartwatch);
        bt_smartwatch.setOnClickListener(this);

        bt_info = (Button) findViewById(R.id.bt_main_info);
        bt_info.setOnClickListener(this);

        bt_wearos = (Button) findViewById(R.id.bt_main_wearos);
        bt_wearos.setOnClickListener(this);


        dummy2 = findViewById(R.id.bt_main_dummy2);
        dummy2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LinearLayout ll = findViewById(R.id.ll_main_gif);
                ll.setVisibility(View.GONE);
                bt_wearos.setVisibility(View.VISIBLE);
            }
        });

    }

    @Override
    public void onClick(View v) {
        Intent intent;
        ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(this);
        switch (v.getId()){
            case R.id.bt_main_setting:
                intent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(intent,options.toBundle());
                break;
            case R.id.bt_main_testing:
                intent = new Intent(MainActivity.this, TestingActivity.class);
                startActivity(intent, options.toBundle());
                break;
            case R.id.bt_main_wearos:
                LinearLayout ll = findViewById(R.id.ll_main_gif);
                if(bt_wearos.getVisibility() == View.VISIBLE){
                    bt_wearos.setVisibility(View.GONE);
                    ll.setVisibility(View.VISIBLE);
                } else {
                    bt_wearos.setVisibility(View.VISIBLE);
                    ll.setVisibility(View.GONE);
                }
                break;
            case R.id.bt_main_database:
                intent = new Intent(MainActivity.this, ReplyCollectionActivity.class);
                startActivity(intent,options.toBundle());
                break;

            case R.id.bt_main_info:
                break;
            default:
                break;

        }
    }

        /*
        try{
            MyMessage myMessage = MyMessage.decodeMessage(text);
            //System.out.print(myMessage.toString());
            SendMessage messageService = new SendMessage(this);
            messageService.sendMessage(PATH_SMARTWATCH_TEST,myMessage);
        } catch (Exception e){
            System.out.println(e);
            System.out.println(R.string.TAG_WEBSOCKET_RECEIVED + text);

        }
        */




}

