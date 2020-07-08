package de.dipf.edutec.thriller.experiencesampling.activities;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import android.app.ActivityOptions;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;

import de.dipf.edutec.thriller.experiencesampling.sensorservice.DataLayerListenerService;
import org.json.JSONException;
import org.json.JSONObject;

import de.dipf.edutec.thriller.experiencesampling.R;
import de.dipf.edutec.thriller.experiencesampling.messageservice.MessagesSingleton;
import de.dipf.edutec.thriller.experiencesampling.messageservice.Receiver;
import de.dipf.edutec.thriller.experiencesampling.messageservice.SendMessage;
import de.dipf.edutec.thriller.messagestruct.MyMessage;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;
import pl.droidsonroids.gif.GifImageButton;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    // Static
    String PATH_SMARTWATCH_TEST;


    // GUI Elements
    GifImageButton dummy2;
    Button bt_settings, bt_database, bt_smartwatch, bt_testing, bt_info, bt_wearos;

    // WebSockets
    OkHttpClient client;
    WebSocket ws;
    MessagesSingleton messagesSingleton;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS);
        setContentView(R.layout.activity_main);
        findGUIElements();

        Receiver receiver = new Receiver(this);

        PATH_SMARTWATCH_TEST = getResources().getString(R.string.PATH_TOSMARTWATCH_TEST);

        client = new OkHttpClient();
        messagesSingleton = MessagesSingleton.getInstance();
//        start();

        // start service to listen to sensor data
        // automatic start via manifest file did not work with stable mqtt connection. Needs to be started as a
        // foreground service.
        Intent intent = new Intent(this, DataLayerListenerService.class);
        startService(intent);
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

    void start() {

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        System.out.println(sp.getAll());
        String lobby = sp.getString("signature","defaultChannel");

        Request request = new Request.Builder().url("ws://192.168.2.107:8000/ws/chat/"+lobby+"/").build();
        EchoWebSocketListener listener = new EchoWebSocketListener();
        ws = client.newWebSocket(request, listener);
        //client.dispatcher().executorService().shutdown();
    }

    void sendMsgTest(final String text){

        final String msg = text;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(MyMessage.isTypeMyMessage(msg)){
                    try{

                        JSONObject reader = new JSONObject(msg);
                        System.out.println(reader);
                        String msg = reader.getString("message");
                        final MyMessage myMessage = MyMessage.decodeMessage(msg);
                        SendMessage messageService = new SendMessage(MainActivity.this);
                        messageService.sendMessage(PATH_SMARTWATCH_TEST,myMessage);

                        messagesSingleton.registerListener(new MessagesSingleton.Listener() {
                            @Override
                            public String onStateChange(String uuid) {

                                System.out.println("onStateChange is called " + uuid + "  " + myMessage.getUuid());

                                if(myMessage.getUuid().equals(uuid)){
                                    JSONObject obj = new JSONObject();
                                    try {
                                        obj.put("message" , messagesSingleton.getMyMessageByUUID(uuid));
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                    ws.send(obj.toString());

                                    messagesSingleton.unregisterListener();
                                }

                                return null;
                            }
                        });

                    }catch (Exception e){
                        e.printStackTrace();
                    }

                } else {

                    System.out.println("Websocket Message: " + text);

                }
            }
        });



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

    private final class EchoWebSocketListener extends WebSocketListener {

        private static final int NORMAL_CLOSURE_STATUS = 1000;

        @Override
        public void onOpen(WebSocket webSocket, Response response) {
            Log.d("MainActivity TAG ", "onOpen() is called.");
            JSONObject obj = new JSONObject();
            try {
                obj.put("message" , "Smartphone opened Connection");
            } catch (JSONException e) {
                e.printStackTrace();
            }

            webSocket.send(obj.toString());

            getSupportActionBar().setIcon(R.drawable.icon_correct);

        }

        @Override
        public void onMessage(WebSocket webSocket, String text) {
            sendMsgTest(text);

        }

        @Override
        public void onMessage(WebSocket webSocket, ByteString bytes) {
            Log.d("MainActivity TAG ", "onMessage() for ByteString is called.");

        }

        @Override
        public void onClosing(WebSocket webSocket, int code, String reason) {
            webSocket.send("Smartphone closed Connection");
            Log.d("MainActivity TAG ", "onClosing() is called.");
            System.out.println("Reason " + reason);
            webSocket.close(NORMAL_CLOSURE_STATUS, null);

        }

        @Override
        public void onFailure(WebSocket webSocket, Throwable t, Response response) {
            Log.d("MainActivity TAG ", "onFailure() is called.");
            System.out.println(t);
            System.out.println(response);

        }
    }
}

