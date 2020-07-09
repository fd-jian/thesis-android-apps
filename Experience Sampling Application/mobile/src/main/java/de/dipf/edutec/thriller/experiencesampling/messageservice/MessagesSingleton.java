package de.dipf.edutec.thriller.experiencesampling.messageservice;

import de.dipf.edutec.thriller.messagestruct.MyMessage;
import de.dipf.edutec.thriller.messagestruct.OnSuccessSendPair;

import java.util.ArrayList;
import java.util.List;

public class MessagesSingleton {

    public static MessagesSingleton instance;
    public List<MyMessage> messagesSend;
    public List<MyMessage> messagesReceived;
    public List<OnSuccessSendPair> msgSendList;
    public int numOpenMessages = 0;

    public MessagesSingleton(){
        messagesSend = new ArrayList<MyMessage>();
        messagesReceived = new ArrayList<MyMessage>();
        msgSendList = new ArrayList<OnSuccessSendPair>();
    }

    public static MessagesSingleton getInstance(){
        if(MessagesSingleton.instance == null){
            MessagesSingleton.instance = new MessagesSingleton();
        }
        return MessagesSingleton.instance;
    }

    public void addMessageSend(MyMessage message){
        messagesSend.add(message);
        numOpenMessages += 1;
    }
    public void addMessageReceived(MyMessage message) {
        messagesReceived.add(message);
        if(mListener != null) mListener.onStateChange(message.getUuid());
        System.out.println("MESSAGERECEIVED IS ADDED");
        numOpenMessages -= 1;
    }
    public void addOnSuccessSendPair(OnSuccessSendPair onSuccessSendPair){
        msgSendList.add(onSuccessSendPair);
        if(onSuccListener != null) onSuccListener.onSuccessStateChange(onSuccessSendPair);
    }

    public boolean existUUIDReceived(String uuid){
        for(MyMessage msg: messagesReceived){
            if(msg.getUuid().equals(uuid)){
                return true;
            }
        }
        return false;
    }
    public String getMyMessageByUUID(String uuid){
        for(MyMessage msg : messagesReceived){
            if(msg.getUuid().equals(uuid)){
                return msg.encodeMessage();
            }
        }
        return "";
    }

    public int getNumOpenMessages(){return numOpenMessages;}

    // Variabel change Listener
    private MessagesSingleton.Listener mListener = null;
    public void registerListener(MessagesSingleton.Listener listener) {mListener = listener; }
    public void unregisterListener(){mListener = null;}
    public interface Listener{
        String onStateChange(String uuid);
    }

    private MessagesSingleton.OnSuccessListener onSuccListener = null;
    public void registerListener(MessagesSingleton.OnSuccessListener listener) {onSuccListener = listener;}
    public void unregisterListenerOnSucc(){onSuccListener = null;}
    public interface OnSuccessListener{
        OnSuccessSendPair onSuccessStateChange(OnSuccessSendPair onSuccessSendPair);

        ;
    }


}
