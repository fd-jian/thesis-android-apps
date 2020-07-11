package de.dipf.edutec.thriller.experiencesampling.activities;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.annotation.SuppressLint;
import android.app.ActivityOptions;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;

import com.mikepenz.actionitembadge.library.ActionItemBadge;

import de.dipf.edutec.thriller.experiencesampling.sensorservice.DataLayerListenerService;

import de.dipf.edutec.thriller.experiencesampling.R;
import de.dipf.edutec.thriller.experiencesampling.messageservice.MessagesSingleton;
import de.dipf.edutec.thriller.experiencesampling.messageservice.Receiver;
import de.dipf.edutec.thriller.experiencesampling.messageservice.WebSocketService;
import pl.droidsonroids.gif.GifImageButton;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {



    // GUI Elements
    GifImageButton dummy2;
    Button bt_settings, bt_database, bt_smartwatch, bt_testing, bt_info, bt_wearos;

    Menu menu;
    int msgToHandle = 0;

    private BroadcastReceiver mConnectionReceiver = new BroadcastReceiver() {
        @SuppressLint("RestrictedApi")
        @Override
        public void onReceive(Context context, Intent intent) {
            String connected = intent.getStringExtra("message");
            Log.d("receiver", "Got message: " + connected);
            if(connected.equals("true")){
                //bt_database.setCompoundDrawableTintList(ColorStateList.valueOf(getResources().getColor(R.color.green_ok, null)));
                getSupportActionBar().setTitle(Html.fromHtml("Exp. Sampling: <font color='#4CAF50'> Online </font>", 1));
            } else if(connected.equals("false")) {
                //bt_database.setCompoundDrawableTintList(ColorStateList.valueOf(getResources().getColor(R.color.red_ok, null)));
                getSupportActionBar().setTitle(Html.fromHtml("Exp. Sampling: <font color='#F44336'> Offline </font>", 1));
            } else {
                getSupportActionBar().setTitle(Html.fromHtml("Exp. Sampling: <font color='#FF9800'> Connecting </font>", 1));
            }

        }
    };

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @SuppressLint("RestrictedApi")
        @Override
        public void onReceive(Context context, Intent intent) {
            String connected = intent.getStringExtra("message");
            Log.d("receiver", "Got message: " + connected);
            msgToHandle +=1;
            ActionItemBadge.update(menu.findItem(R.id.item_samplebadge),MessagesSingleton.getInstance().numOpenMessages);

        }
    };




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS);
        setContentView(R.layout.activity_main);
        findGUIElements();

        getSupportActionBar().setTitle(Html.fromHtml("Exp. Sampling: - ", 1));
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        //getSupportActionBar().setLogo(R.drawable.ic_notifications_black_24dp);
        //getSupportActionBar().setDisplayUseLogoEnabled(true);

        Receiver receiver = new Receiver(this);
        startForegroundService(new Intent(this, WebSocketService.class));

        LocalBroadcastManager.getInstance(this).registerReceiver(mConnectionReceiver,
                new IntentFilter("connection-state-changed"));

        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                new IntentFilter("message-received"));

        // start service to connect to mqtt and listen to data from wearable
        // this service is usually autostarted on boot. The following serves as a way to restart the service,
        // for instance if it was shutdown unintentionally.
//        Intent intent = new Intent(this, DataLayerListenerService.class);
//        startForegroundService(intent);
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

        @SuppressLint("ResourceType")
        @Override
        public boolean onCreateOptionsMenu(Menu menu) {
            // Inflate the menu; this adds items to the action bar if it is present.
            this.menu = menu;
            getMenuInflater().inflate(R.layout.menu_settings, menu);

            //you can add some logic (hide it if the count == 0)
            Drawable drawable = getDrawable(R.drawable.ic_message_black_24dp);
            ActionItemBadge.update(this, menu.findItem(R.id.item_samplebadge),drawable, ActionItemBadge.BadgeStyles.GREEN, MessagesSingleton.getInstance().numOpenMessages);

            return true;
        }

}

