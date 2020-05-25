package de.dipf.edutec.thriller.messagestruct;

import java.lang.reflect.Field;
import java.util.UUID;

public class MyMessage {

    private String msgQuestion;
    private String[] msgAnswers;
    private int msgOrigin;
    private int numAnsw;
    private boolean startActivity;
    private String uuid;

    public MyMessage(){
        this.uuid = UUID.randomUUID().toString();
    }

    public MyMessage(String uuid) {
        this.uuid = uuid;
    }


    public void setStartActivity(Boolean bool){
        this.startActivity = bool;
    }
    public void setMsgAnswersNums(int lowerBound, int upperBound){
        this.msgAnswers = new String[upperBound-lowerBound];
        int counter = 0;
        for(int i = lowerBound; i <= upperBound; i++){
            this.msgAnswers[counter] = String.valueOf(i);
            counter += 1;
        }
    }
    public void setRestDefault(){
        if(this.msgAnswers == null){            this.msgAnswers = new String[1];
            this.msgAnswers[0] = "0";

        }
        if(this.msgQuestion == null) this.msgQuestion = "0";
    }

    public Boolean getStartActivity(){return this.startActivity;}

    public String encodeMessage(){

        String toreturn = "";
        toreturn += String.valueOf(startActivity) + ";";

        toreturn += this.uuid + ";";

        toreturn += String.valueOf(this.msgOrigin) + ";" + String.valueOf(this.numAnsw) + ";";

        toreturn += this.msgQuestion + ";";

        for(String answer: msgAnswers){
            toreturn += answer + ",";
        }

        toreturn = toreturn.substring(0, toreturn.length() - 1) +  ";";

        return toreturn;
    }
    public static MyMessage decodeMessage(String encoded){
        String[] cipher = encoded.split(";");
        MyMessage myMessage = new MyMessage(cipher[1]);
        myMessage.setStartActivity(Boolean.valueOf(cipher[0]));
        myMessage.msgOrigin = Integer.valueOf(cipher[2]);
        myMessage.numAnsw = Integer.valueOf(cipher[3]);
        myMessage.msgQuestion = cipher[4];
        myMessage.msgAnswers = cipher[5].split(",");
        return myMessage;


    }
    public String toString() {
        StringBuilder result = new StringBuilder();
        String newLine = System.getProperty("line.separator");

        result.append( this.getClass().getName() );
        result.append(newLine);
        result.append( " Object {" );
        result.append(newLine);

        //determine fields declared in this class only (no fields of superclass)
        Field[] fields = this.getClass().getDeclaredFields();

        //print field names paired with their values
        for ( Field field : fields  ) {
            result.append("  ");
            try {
                result.append( field.getName() );
                result.append(": ");
                //requires access to private field:
                if(field.getName() == "msgAnswers"){
                    for(int i = 0; i < this.msgAnswers.length; i++){
                        result.append("  " + msgAnswers[i]);
                    }
                } else {

                    result.append( field.get(this) );
                }
            } catch ( IllegalAccessException ex ) {
                System.out.println(ex);
            }
            result.append(newLine);
        }
        result.append("}");

        return result.toString();
    }

}
