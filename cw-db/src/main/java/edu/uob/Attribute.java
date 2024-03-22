package edu.uob;

import java.util.ArrayList;

public class Attribute {
    String name;
    ArrayList<Value> allValues; // All values stored under attribute in table
    Table parent; // Table it belongs to

    public Attribute(String attributeName, Table parentTable){
        this.name = attributeName;
        this.parent = parentTable;
        allValues = new ArrayList<>();
    }

    public String getDataAsString(){
        return this.name;
    }

    public void setDataAsString(String newValue){
        this.name = newValue;
    }
}
