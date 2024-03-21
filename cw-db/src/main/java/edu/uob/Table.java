package edu.uob;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class Table {
    String name;
    ArrayList<ArrayList<Attribute>> tableContents;
    File tableFile;
    int indexID;

    public Table(String tableName, boolean fromFile) {
        this.indexID = 1; // First index for the table
        this.name = tableName;
        tableContents = new ArrayList<>();
        if (!fromFile){ // Tables from files should already have id column
            createAttribute("id"); // Add the id column to table
        }
    }

    public String getTableName(){
        return this.name;
    }

    public int getNumberOfAttributes(){
        return this.tableContents.size();
    }

    public ArrayList<Attribute> getAllAttributes(){
        ArrayList<Attribute> allAttributes = new ArrayList<>();

        for (ArrayList<Attribute> column : tableContents) {
            allAttributes.add(column.get(0)); // Attributes are the first item in column arraylists
        }
        return allAttributes;
    }

    public void createAttribute(String attributeName) {
        ArrayList<Attribute> attributeColumn = new ArrayList<>(); // Add a column for each attribute
        attributeColumn.add(new Attribute(attributeName));
        tableContents.add(attributeColumn);
    }

    public void storeValueRow(ArrayList<String> values) throws IOException {
        int currentIndexID = this.indexID;
        this.indexID++;

        // Add the id value before processing other values:
        Attribute idAttribute = getAttributeFromName("id");
        Value idValue = new Value(currentIndexID, String.valueOf(currentIndexID), "id");
        idAttribute.allValues.add(idValue);
        tableContents.get(0).add(idValue);

        for (String value : values){
            int columnIndex = values.indexOf(value) + 1; // Add one to account for id column at start of each row
            String attributeName = getAttributeNameFromIndex(columnIndex); // Get corresponding attribute from value index
            Attribute parent = getAttributeFromName(attributeName); // Get parent attribute

            Value newValue = new Value(currentIndexID, value, attributeName);
            parent.allValues.add(newValue); // Add the value to its corresponding attributes ArrayList
            tableContents.get(columnIndex).add(newValue);
        }
        writeAttributesAndValuesToFile(); // Overwrite the file with the new values
    }

    public void createValueFromFile(int attributeIndex, String value, int idIndex) {
        String attributeName = getAttributeNameFromIndex(attributeIndex);
        Attribute parent = getAttributeFromName(attributeName);

        // Create a new value and store it in tableContents & parent attributes ArrayList
        Value newValue = new Value(idIndex, value, attributeName);
        parent.allValues.add(newValue);
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
        return tableContents.get(columnIndex).get(0).getDataAsString();
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

    public int getRowIndexFromID(int id) throws IOException {
        ArrayList<Attribute> idColumn = tableContents.get(0);
        int rowIndex = 1; // Skip past header row

        for (int i = 1; i < idColumn.size(); i++) {
            Attribute indexID = idColumn.get(i); // skip past the "id" at top of the string
            if (Integer.parseInt(indexID.getDataAsString()) == id){
                return rowIndex;
            }
            rowIndex++;
        }
        throw new IOException("No row with that ID exists in the table");
    }

    public void updateValue(String idIndex, String attributeName, String newValue) throws IOException {
        int rowIndex = getRowIndexFromID(Integer.parseInt(idIndex));
        int attributeIndex = getAttributeIndexFromName(attributeName);
        // Update the appropriate value in table contents
        tableContents.get(attributeIndex).get(rowIndex).setDataAsString(newValue);

        Attribute parent = getAttributeFromName(attributeName);
        // Update the appropriate value in parent attributes arraylist
        parent.allValues.get(rowIndex-1).setDataAsString(newValue);

        writeAttributesAndValuesToFile();
    }
}