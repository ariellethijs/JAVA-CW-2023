package edu.uob;

public class Value extends Attribute {

    int correspondingID;

    DBSession currentSession;
    DataType dataType;
    private String dataString;
    private boolean dataBoolean;

    private int dataInteger;

    private float dataFloat;

    public Value(int iD, String attributeName, DataType type, DBSession current){
        super(attributeName, current, type);
        super.setDataType(type);
        this.currentSession = current;
        this.correspondingID = iD;
        this.dataType = type;
    }

    public void setDataString(String data){
        this.dataString = data;
    }

    public String getDataString(){
        return this.dataString;
    }

    public void setDataBoolean(boolean data){
        this.dataBoolean = data;
    }

    public boolean getDataBoolean(){
        return this.dataBoolean;
    }

    public void setDataInteger(int data){
        this.dataInteger = data;
    }

    public int getDataInteger(){
        return this.dataInteger;
    }

    public void setDataFloat(float data){
        this.dataFloat = data;
    }

    public float getDataFloat(){
        return this.dataFloat;
    }







}
