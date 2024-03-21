package edu.uob;

public class Value extends Attribute {

    String correspondingAttribute;
    int correspondingID;
    String dataAsString;


    public Value(int iD, String value, String attributeName, Table parentTable) {
        super(attributeName, parentTable);
        this.correspondingID = iD; // i.e. which row is it on
        this.correspondingAttribute = attributeName; // i.e. which column is it in
        this.dataAsString = value;
    }

    @Override
    public String getDataAsString(){
        return this.dataAsString;
    }
}
