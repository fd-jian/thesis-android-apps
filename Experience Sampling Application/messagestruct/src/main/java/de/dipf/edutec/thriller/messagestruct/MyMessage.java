package de.dipf.edutec.thriller.messagestruct;

import java.lang.reflect.Field;
import java.util.UUID;

public class MyMessage {

    private String msgQuestion;
    private String[] msgAnswers;
    private String userAnswer;
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
    public void setMsgQuestion(String question){this.msgQuestion = question;}
    public void setMsgOrigin(int origin){this.msgOrigin = 1;}
    public void setMsgAnswers(String[] answers){
        this.msgAnswers = answers;
        this.numAnsw = this.msgAnswers.length;
    }
    public void setUserAnswer(String userAnswer){this.userAnswer = userAnswer;}

    public Boolean getStartActivity(){return this.startActivity;}
    public String getQuestion(){return this.msgQuestion;}
    public String[] getAnswers(){return this.msgAnswers;}

    public int getOrigin(){return this.msgOrigin;}
    public String getUuid(){return this.uuid;}
    public int getNumAnsw(){return this.numAnsw;}
    public String getUserAnswer(){return this.userAnswer;}

    public String encodeMessage(){

        String toreturn = "";
        toreturn += String.valueOf(startActivity) + ";";

        toreturn += this.uuid + ";";

        toreturn += String.valueOf(this.msgOrigin) + ";" + String.valueOf(this.numAnsw) + ";";

        toreturn += this.msgQuestion + ";";

        toreturn += this.userAnswer + ";";

        try{

            for(String answer: msgAnswers){
                toreturn += answer + ",";
            }
            toreturn = toreturn.substring(0, toreturn.length() - 1) +  ";";
        } catch (Exception e){


        }

        return toreturn;
    }


    public static MyMessage decodeMessage(String encoded){

        String[] cipher = encoded.split(";");
        MyMessage myMessage = new MyMessage(cipher[1]);
        myMessage.setStartActivity(Boolean.valueOf(cipher[0]));
        myMessage.msgOrigin = Integer.valueOf(cipher[2]);
        myMessage.numAnsw = Integer.valueOf(cipher[3]);
        myMessage.msgQuestion = cipher[4];
        myMessage.userAnswer = cipher[5];
        try{
            myMessage.msgAnswers = cipher[6].split(",");
        } catch (Exception e){
            // TODO
        }
        return myMessage;


    }

    public static boolean isTypeMyMessage(String text){
        try{
            String[] cipher = text.split(";");
            MyMessage myMessage = new MyMessage(cipher[1]);
            myMessage.setStartActivity(Boolean.valueOf(cipher[0]));
            myMessage.msgOrigin = Integer.valueOf(cipher[2]);
            myMessage.numAnsw = Integer.valueOf(cipher[3]);
            myMessage.msgQuestion = cipher[4];
            myMessage.userAnswer = cipher[5];
            try{
                myMessage.msgAnswers = cipher[6].split(",");
            } catch (Exception e){
                // TODO
            }

            return true;
        }catch (Exception e){

            return false;

        }

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
            } catch (Exception e) {
                // TODO
            }
            result.append(newLine);
        }
        result.append("}");

        return result.toString();
    }

}


// Example false;55e1f97c-62c9-4c08-aefe-2a075abc793e;1;0;How are you?;null;
// Example false;d8bd78d0-3584-42b0-9fc7-cfc40ca52c91;1;3;One, two or three?;null;1,2,3;
// Example false;55e1f97c-62c9-4c08-aefe-2a075abc792e;1;0;Was machst du gerade?;null;