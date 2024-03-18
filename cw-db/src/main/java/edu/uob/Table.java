package edu.uob;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class Table {
    String name;

    DBSession currentSession;

    ArrayList<ArrayList<Attribute>> tableContents;

//    ArrayList<Integer> rowID;

    Database parent;

    File tableFile;

    public Table(String tableName, DBSession current, Database parentDatabase) throws IOException {
        this.name = tableName;
        this.currentSession = current;
        this.parent = parentDatabase;
        tableContents = new ArrayList<>();
        //rowID = new ArrayList<>();
        createAttribute("id", DataType.INTEGER); // Add the id column to table
    }

    public String getTableName(){
        return this.name;
    }

    public void createAttribute(String attributeName, DataType type) throws IOException {
        ArrayList<Attribute> attributeColumn = new ArrayList<>(); // Add a column for each attribute
        attributeColumn.add(new Attribute(attributeName, currentSession, type, this));
        tableContents.add(attributeColumn);
    }

    public void createValueFromString(String attributeName, String value, int idIndex) throws IOException {
        String[] valueAsArray = new String[]{value, " "}; // Extra buffer to avoid going out of bounds
        DBParser valueParser = new DBParser(valueAsArray);
        DataType valueDataType = valueParser.findDataTypeOfValue();
        new Value(value, idIndex, attributeName, valueDataType, currentSession, this);
    }


    public void setIDValue(String value, int row) throws IOException {
        tableContents.get(row).set(0, new Value(String.valueOf(currentSession.getIndexID()), currentSession.getIndexID(),
                "id", DataType.INTEGER, currentSession, this));
        currentSession.incrementIndexID(); // Increment the ID for next use
    }

    public int attributeIndexFromName(String attributeName) throws IOException {
        int attributeIndex = 0;
        for (ArrayList<Attribute> column : tableContents){
            if (column.get(0).name.equals(attributeName)){
                return attributeIndex;
            }
            attributeIndex++;
        }
        throw new IOException("No such attribute exists");
    }

    public String getAttributeNameFromIndex(int columnIndex){
        return tableContents.get(0).get(columnIndex).getDataAsString();
    }

    public void setTableFile(File file){
        this.tableFile = file;
    }

    public void writeAttributesToFile() throws IOException {
        FileWriter fileWriter = new FileWriter(this.tableFile);
        for (ArrayList<Attribute> tableContent : tableContents) {
            for (Attribute attribute : tableContent) {
                fileWriter.write(attribute.getDataAsString() + "\t");
            }
        }
        fileWriter.write("\n");
        fileWriter.close();
    }

    public int getIDFromRowIndex(int row){
        return Integer.parseInt(tableContents.get(row).get(0).getDataAsString());
    }

    public int getRowIndexFromID(int id) throws IOException {
        int rowIndex = 0;
        for (ArrayList<Attribute> row : tableContents){
            if (Integer.parseInt(row.get(0).getDataAsString()) == id){
                return rowIndex;
            }
            rowIndex++;
        }
        throw new IOException("No row with that ID exists in the table");
    }

    public void updateFile(){

    }
}