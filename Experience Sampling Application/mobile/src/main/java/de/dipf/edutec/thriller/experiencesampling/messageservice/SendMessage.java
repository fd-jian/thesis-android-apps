package de.dipf.edutec.thriller.experiencesampling.messageservice;

import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;
import java.util.List;
import java.util.concurrent.ExecutionException;
import de.dipf.edutec.thriller.messagestruct.MyMessage;

public class SendMessage extends Application {

    public static de.dipf.edutec.thriller.experiencesampling.messageservice.SendMessage instance;
    public MessagesSingleton messagesSingleton;
    public Context context;
    protected Handler myHandler;

    public SendMessage(Context context){
        this.context = context;
        messagesSingleton = MessagesSingleton.getInstance();
        myHandler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(@NonNull Message msg) {
                Bundle stuff = msg.getData();
                System.out.println(stuff.getString("messageText"));
                return true;
            }
        });
    }

    /**
    public static MessageService getInstance(){
        if(MessageService.instance == null){
            MessageService.instance = new MessageService();

        }
        return MessageService.instance;
    }**/

    public void sendMessage(String path, MyMessage myMessage){

        NewThread sendMessage = new NewThread(path,myMessage.encodeMessage());
        sendMessage.start();

        messagesSingleton.addMessage(myMessage);

    }

    public void sendmessage(String messageText) {
        Bundle bundle = new Bundle();
        bundle.putString("messageText", messageText);
        Message msg = myHandler.obtainMessage();
        msg.setData(bundle);
        myHandler.sendMessage(msg);

    }




    public class NewThread extends Thread{
        String path;
        String message;

        public NewThread(String path, String message){
            this.path = path;
            this.message = message;
        }

        public void run(){
            Task<List<Node>> nodeListTask =
                    Wearable.getNodeClient(context).getConnectedNodes();

            try {
                List<Node> nodes = Tasks.await(nodeListTask);
                for(Node node :nodes){
                    Task<Integer> sendMessageTask =
                            Wearable.getMessageClient(context).sendMessage(node.getId(), path, message.getBytes());

                    System.out.println("MESSAGE SEND TO " + node.getDisplayName() + " due path: " + path);

                    sendMessageTask.addOnSuccessListener(new OnSuccessListener<Integer>() {
                        @Override
                        public void onSuccess(Integer integer) {
                            //Toast.makeText(context,String.valueOf(integer),Toast.LENGTH_LONG).show();
                        }
                    });

                    sendMessageTask.addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            //Toast.makeText(context,e.toString(),Toast.LENGTH_LONG).show();
                        }
                    });

                    try{

                        Integer result = Tasks.await(sendMessageTask);
                        System.out.println("RESULT: " + result);
                        sendmessage("I just sent the wearable a message ");

                    } catch (ExecutionException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }

        }

    }



}
