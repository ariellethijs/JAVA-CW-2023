package edu.uob;

import java.io.IOException;
import java.util.ArrayList;

public class Table {
    String name;

    DBSession currentSession;

    ArrayList<ArrayList<Attribute>> tableContents;

    int idIndex;

    public Table(String tableName, DBSession current){
        this.name = tableName;
        this.currentSession = current;
        this.idIndex = 1;
        tableContents = new ArrayList<>();
    }

    public String getTableName(){
        return this.name;
    }

    public void addAttribute(String attributeName, DataType type){
        ArrayList<Attribute> column = new ArrayList<>(); // Add a column for each attribute
        column.add(new Attribute(attributeName, currentSession, DataType.UNDEFINED));
        tableContents.add(column);
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

    public void addValueToKnownAttribute(String attributeName, DataType type) throws IOException {
        int attributeIndex = attributeIndexFromName(attributeName);

        // This only works if setting a new value for a new ID, need to find a way of getting IDindex for different values
        // Need an overall better understanding of SQL syntax I think
        tableContents.get(this.idIndex).add(attributeIndex, new Value(this.idIndex, attributeName, type, currentSession));
        this.idIndex++;
    }
}
