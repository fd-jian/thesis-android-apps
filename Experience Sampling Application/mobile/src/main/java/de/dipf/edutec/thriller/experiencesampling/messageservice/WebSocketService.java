package de.dipf.edutec.thriller.experiencesampling.messageservice;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

import org.json.JSONException;
import org.json.JSONObject;

import de.dipf.edutec.thriller.experiencesampling.R;
import de.dipf.edutec.thriller.experiencesampling.activities.MainActivity;
import de.dipf.edutec.thriller.messagestruct.MyMessage;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;
import tech.gusavila92.websocketclient.WebSocketClient;

public class WebSocketService extends Service {
    // Static
    String PATH_SMARTWATCH_TEST;
    // WebSockets
    OkHttpClient client;
    WebSocket ws;
    MessagesSingleton messagesSingleton;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        PATH_SMARTWATCH_TEST = getResources().getString(R.string.PATH_TOSMARTWATCH_TEST);
        client = new OkHttpClient();
        messagesSingleton = MessagesSingleton.getInstance();
        start();
        return null;
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
