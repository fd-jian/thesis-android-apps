package de.dipf.edutec.thriller.experiencesampling.activities;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import android.transition.Slide;

import android.annotation.SuppressLint;
import android.app.ActivityOptions;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.transition.Explode;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import de.dipf.edutec.thriller.experiencesampling.R;
import de.dipf.edutec.thriller.experiencesampling.messageservice.Receiver;
import pl.droidsonroids.gif.GifDrawable;
import pl.droidsonroids.gif.GifImageButton;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    // GUI Elements
    GifImageButton dummy2;
    GifImageButton dummy1;
    Button bt_settings, bt_database, bt_smartwatch, bt_testing, bt_info, bt_wearos;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS);
        setContentView(R.layout.activity_main);
        findGUIElements();
        Receiver receiver = new Receiver(this);



    }

    /*
    @SuppressLint("ResourceType")
    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.layout.menu_settings, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        int id = item.getItemId();
        if (id == R.id.action_settings){
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }*/

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

        bt_info = (Button) findViewById(R.id.bt_main_smartwatch);
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
            default:
                break;

        }
    }
}

