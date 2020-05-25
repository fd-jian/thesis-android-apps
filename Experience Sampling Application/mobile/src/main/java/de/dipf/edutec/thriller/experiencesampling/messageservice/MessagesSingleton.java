package de.dipf.edutec.thriller.experiencesampling.messageservice;

import de.dipf.edutec.thriller.messagestruct.MyMessage;

import java.util.ArrayList;
import java.util.List;

public class MessagesSingleton {

    public static MessagesSingleton instance;
    public List<MyMessage> messagesCollection;

    public MessagesSingleton(){
        messagesCollection = new ArrayList<MyMessage>();
    }

    public static MessagesSingleton getInstance(){
        if(MessagesSingleton.instance == null){
            MessagesSingleton.instance = new MessagesSingleton();
        }
        return MessagesSingleton.instance;
    }

    public void addMessage(MyMessage message){
        messagesCollection.add(message);
    }

}
