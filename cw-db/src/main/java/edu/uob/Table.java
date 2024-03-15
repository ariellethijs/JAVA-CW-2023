package edu.uob;

import java.io.IOException;
import java.util.ArrayList;

public class Table {
    String name;

    DBSession currentSession;

    ArrayList<ArrayList<Attribute>> tableContents;

    Database parent;

    int idIndex;

    public Table(String tableName, DBSession current, Database parentDatabase){
        this.name = tableName;
        this.currentSession = current;
        this.idIndex = 1; // Needs to be changes to a data storage wide index?
        this.parent = parentDatabase;
        tableContents = new ArrayList<>();
    }

    public String getTableName(){
        return this.name;
    }

    public void createAttribute(String attributeName, DataType type){
        ArrayList<Attribute> column = new ArrayList<>(); // Add a column for each attribute
        column.add(new Attribute(attributeName, currentSession, type, this));
        tableContents.add(column);
    }

    public void createValueFromString(String attributeName, String value){
        String[] valueAsArray = new String[]{value, " "}; // Extra buffer to avoid going out of bounds
        DBParser valueParser = new DBParser(valueAsArray);
        DataType valueDataType = valueParser.findDataTypeOfValue();
        Value newValue = new Value(this.idIndex, attributeName, valueDataType, currentSession, this);
        newValue.storeValue(value, valueDataType);
    }

    int attributeIndexFromName(String attributeName) throws IOException {
        int attributeIndex = 0;
        for (ArrayList<Attribute> column : tableContents){
            if (column.get(0).name.equals(attributeName)){
                return attributeIndex;
            }
            attributeIndex++;
        }
        throw new IOException("No such attribute exists");
    }

    String getAttributeNameFromIndex(int columnIndex){
        return tableContents.get(0).get(columnIndex).getName();
    }

//    public void addValueToKnownAttribute(String attributeName, DataType type) throws IOException {
//        int attributeIndex = attributeIndexFromName(attributeName);
//
//        // This only works if setting a new value for a new ID, need to find a way of getting IDindex for different values
//        // Need an overall better understanding of SQL syntax I think
//        tableContents.get(this.idIndex).add(attributeIndex, new Value(this.idIndex, attributeName, type, currentSession));
//        this.idIndex++;
//    }
}
