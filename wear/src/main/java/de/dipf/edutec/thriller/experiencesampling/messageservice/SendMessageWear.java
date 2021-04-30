package de.dipf.edutec.thriller.experiencesampling.messageservice;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;

import java.util.List;
import java.util.concurrent.ExecutionException;


import javax.security.auth.callback.Callback;

public class SendMessageWear {

    Context context;
    protected Handler myHandler;

    public SendMessageWear(Context context){
        this.context = context;

        myHandler = new android.os.Handler(new android.os.Handler.Callback() {
            @Override
            public boolean handleMessage(@NonNull Message msg) {
                Bundle stuff = msg.getData();

                return true;
            }
        });
    }


    public void sendAck(String path, String message){
        new NewThread(path,message).start();
    }


    class NewThread extends Thread {

        String path;
        String message;

        NewThread(String path, String message){
            this.path = path;
            this.message = message;

            Log.e("-----------","----------------");
            Log.e("Added new Thread to response " + path," " +  message);
            Log.e("-----------","----------------");
        }


        public void run(){
            Task<List<Node>> nodeListTask =
                    Wearable.getNodeClient(context).getConnectedNodes();
            try {
                /* Block on a task and get the result synchronously */
                List<Node> nodes = Tasks.await(nodeListTask);
                for (Node node : nodes) {
                    /* Send the message */
                    Task<Integer> sendMessageTask =
                            Wearable.getMessageClient(context).sendMessage(node.getId(), path, message.getBytes());

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
                    try {
                        Integer result = Tasks.await(sendMessageTask);
                    } catch (ExecutionException exception) {
                        exception.printStackTrace();
                    } catch (InterruptedException exception) {
                        exception.printStackTrace();
                    }
                }
            } catch (ExecutionException exception) {
                /*//TO DO//*/
            } catch (InterruptedException exception) {
                /* //TO DO//*/
            }
        }

    }

}
