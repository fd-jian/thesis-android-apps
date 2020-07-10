package de.dipf.edutec.thriller.experiencesampling.messageservice;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;

import de.dipf.edutec.thriller.experiencesampling.conf.CustomApplication;
import de.dipf.edutec.thriller.experiencesampling.foreground.ForegroundNotificationCreator;
import de.dipf.edutec.thriller.experiencesampling.messagestruct.MyMessage;
import org.json.JSONException;
import org.json.JSONObject;

import de.dipf.edutec.thriller.experiencesampling.R;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

public class WebSocketService extends Service {
    // Static
    private static String TAG = "WebSockerService: ";
    String PATH_SMARTWATCH_TEST;

    // WebSockets
    OkHttpClient client;
    WebSocket ws;
    MessagesSingleton messagesSingleton;

    Intent intent;
    int flags;
    int startid;
    private ForegroundNotificationCreator fgNotificationManager;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startid) {

        this.intent = intent;
        this.flags = flags;
        this.startid = startid;

        PATH_SMARTWATCH_TEST = getResources().getString(R.string.PATH_TOSMARTWATCH_TEST);
        client = new OkHttpClient();
        messagesSingleton = MessagesSingleton.getInstance();

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        System.out.println(sp.getAll());
        String lobby = sp.getString("signature","defaultChannel");

        Request request = new Request.Builder().url("ws://192.168.99.100:8000/ws/chat/"+lobby+"/").build();
        EchoWebSocketListener listener = new EchoWebSocketListener();
        ws = client.newWebSocket(request, listener);
        //client.dispatcher().executorService().shutdown();

        startForeground(
                fgNotificationManager.getId(),
                fgNotificationManager.getNotification());

        return START_NOT_STICKY;
    }

    public void restart(){
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        System.out.println(sp.getAll());
        String lobby = sp.getString("signature","defaultChannel");

        Request request = new Request.Builder().url("ws://192.168.99.100:8000/ws/chat/"+lobby+"/").build();
        EchoWebSocketListener listener = new EchoWebSocketListener();
        ws = client.newWebSocket(request, listener);
        //client.dispatcher().executorService().shutdown();
    }

    void sendMsgTest(final String text){

        final String msg = text;

        if(MyMessage.isTypeMyMessage(msg)){
            try{
                JSONObject reader = new JSONObject(msg);
                System.out.println(reader);
                String txt = reader.getString("message");
                final MyMessage myMessage = MyMessage.decodeMessage(txt);
                SendMessage messageService = new SendMessage(this);
                messageService.sendMessage(PATH_SMARTWATCH_TEST,myMessage);

                messagesSingleton.registerListener(new MessagesSingleton.Listener() {
                    @Override
                    public String onStateChange(String uuid) {
                        System.out.println("onStateChange is called " + uuid + "  " + myMessage.getUuid());

                        if(myMessage.getUuid().equals(uuid)){
                            JSONObject obj = new JSONObject();
                            try {
                                obj.put("message" , messagesSingleton.getMyMessageByUUID(uuid));
                                }
                            catch (JSONException e) {
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

    void sendConnectionUpdate(String isConnected){
        Intent intent = new Intent("connection-state-changed");
        // You can also include some extra data.
        intent.putExtra("message", isConnected);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }





    private final class EchoWebSocketListener extends WebSocketListener {

        private static final int NORMAL_CLOSURE_STATUS = 1000;


        @Override
        public void onOpen(WebSocket webSocket, Response response) {
            Log.d(TAG , "onOpen() is called.");
            JSONObject obj = new JSONObject();
            try {
                obj.put("message" , "Smartphone opened Connection");
            } catch (JSONException e) {
                e.printStackTrace();
            }

            webSocket.send(obj.toString());
            sendConnectionUpdate("true");
        }

        @Override
        public void onMessage(WebSocket webSocket, String text) {

            if(MyMessage.isTypeMyMessage(text)){
                MessagesSingleton.getInstance().numOpenMessages += 1;
                Intent intent = new Intent("message-received");
                // You can also include some extra data.
                intent.putExtra("message", "isConnected");
                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
            }
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
            sendConnectionUpdate("false");
            Log.d("W TAG ", "onFailure() is called.");
            Log.d(TAG, "waiting some time..");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            sendConnectionUpdate("reconnecting");
            restart();
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        this.fgNotificationManager =
                ((CustomApplication) getApplication()).getContext().getForegroundNotificationCreator();

        if (fgNotificationManager == null) {
            throw new RuntimeException();
        }
    }
}
