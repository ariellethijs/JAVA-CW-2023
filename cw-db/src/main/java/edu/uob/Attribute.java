package edu.uob;

import java.util.ArrayList;

public class Attribute {
    String name;

    DBSession currentSession;
    DataType dataType;

    ArrayList<Value> allValues;

    public Attribute(String attributeName, DBSession current, DataType type){
        this.name = attributeName;
        this.currentSession = current;
        this.dataType = type;
        allValues = new ArrayList<>();
    }

    public void setDataType(DataType type){
        this.dataType = type;
    }

    public void addValue(Value v){
        allValues.add(v);
    }

    public void removeValue(Value v){
        allValues.remove(v);
    }

}
