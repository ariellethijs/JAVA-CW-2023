package edu.uob;

import java.util.ArrayList;

public class Attribute {
    String name;
    ArrayList<Value> allValues;

    public Attribute(String attributeName){
        this.name = attributeName;
        allValues = new ArrayList<>();
    }

    public String getDataAsString(){
        return this.name;
    }

    public void setDataAsString(String newValue){
        this.name = newValue;
    }
}
