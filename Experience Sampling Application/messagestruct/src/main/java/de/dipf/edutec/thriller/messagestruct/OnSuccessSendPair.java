package de.dipf.edutec.thriller.messagestruct;

public class OnSuccessSendPair {

    boolean sendSuccessfull;
    String uuid;

    public OnSuccessSendPair(String uuid, boolean sendSuccessfull){
        this.uuid = uuid;
        this.sendSuccessfull = sendSuccessfull;
    }

    public boolean getSuccess(){return this.sendSuccessfull;}
    public String getUuid(){return this.uuid;}


}
