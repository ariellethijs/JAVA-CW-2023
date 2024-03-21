package edu.uob;

public class Value extends Attribute {
    int correspondingID;
    String dataAsString;

    public Value(int iD, String value, String attributeName, Table parentTable) {
        super(attributeName, parentTable);
        this.correspondingID = iD; // i.e. which row is it on
        this.dataAsString = value;
    }

    @Override
    public String getDataAsString(){
        return this.dataAsString;
    }

    @Override
    public void setDataAsString(String newValue){
        this.dataAsString = newValue;
    }
}
