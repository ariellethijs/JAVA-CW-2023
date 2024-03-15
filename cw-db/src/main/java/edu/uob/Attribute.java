package edu.uob;

import java.util.ArrayList;

public class Attribute {
    String name;

    DBSession currentSession;
    DataType dataType;

    ArrayList<Value> allValues;

    Table parent;

    public Attribute(String attributeName, DBSession current, DataType type, Table parentTable){
        this.name = attributeName;
        this.currentSession = current;
        this.dataType = type;
        this.parent = parentTable;
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

    public String getName(){
        return this.name;
    }

}
