package edu.uob;

import java.util.ArrayList;

public class Attribute {
    String name;
    ArrayList<Value> allValues;

    Table parent;

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
