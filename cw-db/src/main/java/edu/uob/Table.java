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

    Database parent;

    File tableFile;

    public Table(String tableName, DBSession current, Database parentDatabase) throws IOException {
        this.name = tableName;
        this.currentSession = current;
        this.parent = parentDatabase;
        tableContents = new ArrayList<>();
        createAttribute("id", DataType.INTEGER); // Add the id column to table
    }

    public String getTableName(){
        return this.name;
    }

    public int getNumberOfAttributes(){
        return this.tableContents.size();
    }

    public void createAttribute(String attributeName, DataType type) throws IOException {
        ArrayList<Attribute> attributeColumn = new ArrayList<>(); // Add a column for each attribute
        attributeColumn.add(new Attribute(attributeName, currentSession, type, this));
        tableContents.add(attributeColumn);
    }

    public void storeValueRow(ArrayList<String> values) throws IOException {
        int currentIndexID = currentSession.getIndexID();
        currentSession.incrementIndexID(); // Increment so next use will be next index

        // Add the id value before processing other values:
        Value idValue = new Value(currentIndexID, String.valueOf(currentIndexID), "id", DataType.INTEGER, currentSession, this);
        tableContents.get(0).add(idValue);

        for (String value : values){
            int columnIndex = values.indexOf(value) + 1; // Add one to account for id column at start of each row
            String attributeName = getAttributeNameFromIndex(columnIndex); // Get corresponding attribute from value index
            Value newValue = new Value(currentIndexID, value, attributeName, DataType.UNDEFINED, currentSession, this);
            tableContents.get(columnIndex).add(newValue);
        }
        writeAttributesAndValuesToFile(); // Overwrite the file with the new values
    }

    // Only necessary for conditional queries i think?
//    public boolean valuesInLineWithAttributesDataType(String attributeName, DataType valueDataType){
//        if (attributeExists(attributeName)){
//            Attribute attribute = getAttributeFromName(attributeName);
//            return ((attribute.dataType == DataType.UNDEFINED) || (attribute.dataType == valueDataType));
//        } else {
//            return false;
//        }
//    }

    public void createValueFromFile(int attributeIndex, String value, int idIndex) throws IOException {
        // Parse the value to try and determine its DataType
        String[] valueAsArray = new String[]{value, " "}; // Extra buffer to avoid going out of bounds
        DBParser valueParser = new DBParser(valueAsArray);
        DataType valueDataType = valueParser.findDataTypeOfValue(); // Will return UNDEFINED if still uncertain

        String attributeName = getAttributeNameFromIndex(attributeIndex);

        // Create a new value and
        Value newValue = new Value(idIndex, value, attributeName, valueDataType, currentSession, this);
        tableContents.get(attributeIndex).add(newValue);  // Add as the next row in file
    }

    public boolean attributeExists(String attributeName) {
        for (ArrayList<Attribute> column : tableContents){
            if (column.get(0).name.equals(attributeName)){
                return true;
            }
        }
        return false;
    }

    public Attribute getAttributeFromName(String attributeName){
        for (ArrayList<Attribute> column : tableContents){
            if (column.get(0).name.equals(attributeName)){
                return column.get(0);
            }
        }
        return null; // should always check for existence before calling this so usually won;t reach
    }

    public int getAttributeIndexFromName(String attributeName) throws IOException {
        if (!attributeExists(attributeName)){
            throw new IOException("No such attribute exists");
        } else {
            for (ArrayList<Attribute> column : tableContents){
                if (!column.isEmpty() && column.get(0).name.equals(attributeName)){
                    return tableContents.indexOf(column);
                }
            }
        }
        return -1;
    }

    public void deleteAttribute(String attributeName) throws IOException {
        int attributeIndex = getAttributeIndexFromName(attributeName);

        if (attributeIndex < 0 || attributeIndex > tableContents.size()){
            throw new IOException("Attempting to remove an attribute which exists outside of the table bounds");
            // For debugging remove/ change later as useless for users
        } else {
            tableContents.remove(attributeIndex);
            // Rewrite the file to reflect the changes:
            writeAttributesToFile();
        }
    }

    public String getAttributeNameFromIndex(int columnIndex){
        return tableContents.get(columnIndex).get(0).getAttributeName();
    }

    public void setTableFile(File file){
        this.tableFile = file;
    }

    public void writeAttributesToFile() throws IOException {
        FileWriter fileWriter = new FileWriter(this.tableFile);
        for (ArrayList<Attribute> tableContent : tableContents) {
            for (Attribute attribute : tableContent) {
                fileWriter.write(attribute.getAttributeName() + "\t");
            }
        }
        fileWriter.write("\n");
        fileWriter.close();
    }

    public void writeAttributesAndValuesToFile() throws IOException {
        FileWriter fileWriter = new FileWriter(this.tableFile);

        for (int row = 0; row < tableContents.get(0).size(); row++) {
            for (ArrayList<Attribute> column : tableContents) {
                Attribute attribute = column.get(row);
                fileWriter.write(attribute.getDataAsString() + "\t");
            }
            fileWriter.write("\n");
        }
        fileWriter.close();
    }

//    public int getRowIndexFromID(int id) throws IOException {
//        int rowIndex = 0;
//        for (ArrayList<Attribute> row : tableContents){
//            if (Integer.parseInt(row.get(0).getDataAsString()) == id){
//                return rowIndex;
//            }
//            rowIndex++;
//        }
//        throw new IOException("No row with that ID exists in the table");
//    }
}