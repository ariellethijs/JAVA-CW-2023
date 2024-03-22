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

    public ArrayList<Attribute> getAllAttributes(){
        // Return the header row of tableContents
        ArrayList<Attribute> allAttributes = new ArrayList<>();
        for (ArrayList<Attribute> column : tableContents) {
            allAttributes.add(column.get(0)); // Attributes are the first item in column arraylists
        }
        return allAttributes;
    }

    public void createAttribute(String attributeName) {
        ArrayList<Attribute> attributeColumn = new ArrayList<>(); // Add a column for each attribute
        attributeColumn.add(new Attribute(attributeName, this)); // Add the attribute to the header row
        tableContents.add(attributeColumn); // add column to tableContents
    }

    public void storeValueRow(ArrayList<String> values) throws IOException {
        // For inserting a new row of values
        int currentIndexID = this.indexID;
        this.indexID++; // Increment the tableID for next use

        // Add the id attribute to the id column before processing other values:
        Attribute idAttribute = getAttributeFromName("id");
        Value idValue = new Value(currentIndexID, String.valueOf(currentIndexID), "id", this);
        idAttribute.allValues.add(idValue);
        tableContents.get(0).add(idValue);

        for (String value : values){
            int columnIndex = values.indexOf(value) + 1; // Add one to account for id column at start of each row
            String attributeName = getAttributeNameFromIndex(columnIndex); // Get corresponding attributeIndex from attributeName
            Attribute parent = getAttributeFromName(attributeName); // Get parent attribute

            Value newValue = new Value(currentIndexID, value, attributeName, this);
            parent.allValues.add(newValue); // Add the value to its corresponding attributes ArrayList
            tableContents.get(columnIndex).add(newValue); // Add value to the tableContents
        }
        writeAttributesAndValuesToFile(); // Overwrite the file with the new values
    }

    public void createValueFromFile(int attributeIndex, String value, int idIndex) {
        String attributeName = getAttributeNameFromIndex(attributeIndex);
        Attribute parent = getAttributeFromName(attributeName);

        // Create a new value and store it in tableContents & parent attributes ArrayList
        Value newValue = new Value(idIndex, value, attributeName, this);
        parent.allValues.add(newValue);
        tableContents.get(attributeIndex).add(newValue);  // Add as the next row in file
    }

    public boolean attributeExists(String attributeName){
        // Determine attributes existence in table
        for (ArrayList<Attribute> column : tableContents){
            if (column.get(0).name.equalsIgnoreCase(attributeName)){
                return true;
            }
        }
        return false;
    }

    public Attribute getAttributeFromName(String attributeName){
        // Return an attribute object from its name
        for (ArrayList<Attribute> column : tableContents){
            if (column.get(0).name.equalsIgnoreCase(attributeName)){
                return column.get(0);
            }
        }
        return null; // should always check for existence before calling this so usually won;t reach
    }

    public int getAttributeIndexFromName(String attributeName) throws IOException {
        // Return an attribute's index (column index) from it's name
        if (!attributeExists(attributeName)){
            throw new IOException("No such attribute exists");
        } else {
            for (ArrayList<Attribute> column : tableContents){
                if (!column.isEmpty() && column.get(0).name.equalsIgnoreCase(attributeName)){
                    return tableContents.indexOf(column);
                }
            }
        }
        return -1;
    }

    public void deleteAttribute(String attributeName) throws IOException {
        int attributeIndex = getAttributeIndexFromName(attributeName);
        tableContents.remove(attributeIndex); // Remove the entire attribute Column
        writeAttributesAndValuesToFile(); // Rewrite the file to reflect the changes
    }

    public String getAttributeNameFromIndex(int columnIndex){
        return tableContents.get(columnIndex).get(0).getDataAsString();
    }

    public void setTableFile(File file){
        this.tableFile = file;
    }

    public void writeAttributesToFile() throws IOException {
        // Write the header row to a file
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
        // Write tableContents to corresponding tableFile
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
        // Find a row's index by its idIndex
        ArrayList<Attribute> idColumn = tableContents.get(0);

        int rowIndex = 1; // Skip past header row
        for (int i = 1; i < idColumn.size(); i++){
            // Start at 1 to skip past the "id" at top of the string & avoid numberException
            Attribute indexID = idColumn.get(i);
            if (Integer.parseInt(indexID.getDataAsString()) == id){
                return rowIndex;
            }
            rowIndex++;
        }
        throw new IOException("Attempt to access an invalid row");
    }

    public void updateValue(String idIndex, String attributeName, String newValue) throws IOException {
        // Find the index of the value in table contents
        int rowIndex = getRowIndexFromID(Integer.parseInt(idIndex));
        int attributeIndex = getAttributeIndexFromName(attributeName);

        // If 'updating' a previously unset value, add a pseudo value there as placeholder
        if (rowIndex >= tableContents.get(attributeIndex).size()){
            Attribute parent = getAttributeFromName(attributeName);
            Value placeholder = new Value(Integer.parseInt(idIndex), "", "", this);
            tableContents.get(attributeIndex).add(rowIndex, placeholder);
            parent.allValues.add(placeholder);
        }

        // Override the current value with new dataValue
        tableContents.get(attributeIndex).get(rowIndex).setDataAsString(newValue);

        // Update the appropriate value in parent attributes arraylist
        Attribute parent = getAttributeFromName(attributeName);
        parent.allValues.get(rowIndex-1).setDataAsString(newValue);

        // Update the file accordingly
        writeAttributesAndValuesToFile();
    }

    public void deleteRow(String idIndex) throws IOException {
        if (idIndex.equalsIgnoreCase("id")){
            throw new IOException("Cannot delete the attribute header row of a table");
        } else {
            // Parse the value to find the rowIndex its resides on
            int rowIndex = getRowIndexFromID(Integer.parseInt(idIndex));
            // Remove the appropriate cell in each column arraylist
            for (ArrayList<Attribute> column : tableContents) {
                column.remove(rowIndex); // Remove the row from tableContents
                Attribute parent = column.get(0);
                parent.allValues.remove(rowIndex - 1); // Remove the row's value from its corresponding attributes data
            }
        }
    }

}