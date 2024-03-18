package edu.uob;

import java.io.IOException;

public class Value extends Attribute {

    String correspondingAttribute;
    int correspondingID;

    String dataAsString;

    DBSession currentSession;
    DataType dataType;
    private String dataString;
    private boolean dataBoolean;

    private int dataInteger;

    private float dataFloat;

    public Value(String value, int iD, String attributeName, DataType type, DBSession current, Table parentTable) throws IOException{
        super(attributeName, current, type, parentTable);
        super.setDataType(type);
        this.currentSession = current;
        this.correspondingAttribute = attributeName;
        this.correspondingID = iD;
        this.dataType = type;
        this.dataAsString = value;
        storeValue(value, type);
    }

    public void storeValue(String value, DataType type) throws IOException {
        switch (type){
            case STRING -> {
                setDataString(value);
            }
            case BOOLEAN -> {
                if (value.equals("TRUE")) { setDataBoolean(true); }
                if (value.equals("FALSE")) { setDataBoolean(false); }
            }

            case INTEGER -> {
                setDataInteger(Integer.parseInt(value));
            }

            case FLOAT -> {
                setDataFloat(Float.parseFloat(value));
            }

            case UNDEFINED -> {
                this.dataAsString = value;
            }

            default -> {
                throw new IOException("Invalid DataType for value");
            }
        }
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
