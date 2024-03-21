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

    public String getAttributeName(){
        return this.name;
    }

    public void addValue(Value v){
        allValues.add(v);
    }

    public void removeValue(Value v){
        allValues.remove(v);
    }

    public String getDataAsString(){
        return this.name;
    }

}
