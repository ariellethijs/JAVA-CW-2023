package edu.uob;

import java.util.AbstractList;
import java.util.ArrayList;

public class ResponseTableGenerator {

    DBSession currentSession;

    public ResponseTableGenerator(DBSession current){
        this.currentSession = current;
    }

    public ArrayList<ArrayList<String>> createConditionedResponseTable(ArrayList<ArrayList<Attribute>> selectedAttributes, boolean isJoin){
        ArrayList<ArrayList<String>> responseTable = new ArrayList<>();
        if (isJoin){ // If executing a join command you need to replace the old ID values with new ones
            responseTable = addNewIDRows(responseTable, (selectedAttributes.size() - 1));
        } else {
            for (int i = 0; i < selectedAttributes.size(); i++){
                ArrayList<String> row = new ArrayList<>();
                responseTable.add(row);
            }
        }

        // Replace Attribute object with their corresponding attributeName/valuesAsString
        for (ArrayList<Attribute> row : selectedAttributes){
            int rowIndex = selectedAttributes.indexOf(row);
            for (Attribute attribute : row){
                responseTable.get(rowIndex).add(attribute.getDataAsString());
            }
        }
        return responseTable;
    }

    public ArrayList<ArrayList<String>> createUnconditionedResponseTable(ArrayList<Attribute> selectedAttributes, boolean isJoin){
        int columnSize = selectedAttributes.get(0).allValues.size() + 1;
        // Find the number of row to add to response table + 1 to account for attribute header
        ArrayList<ArrayList<String>> responseTable = new ArrayList<>();
        if (isJoin){ // If executing a join command you need to replace the old ID values with new ones
            responseTable = addNewIDRows(responseTable, columnSize);
        } else {
            for (int i = 0; i < columnSize; i++){
                ArrayList<String> row = new ArrayList<>();
                responseTable.add(row);
            }
        }

        for (Attribute attribute : selectedAttributes){ // For each selected attribute add their corresponding values
            responseTable.get(0).add(attribute.getDataAsString());

            int rowIndex = 1; // Start at row 1 for value input to skip header row
            for (Value value : attribute.allValues){
                // Add all the corresponding values for that attribute to the column
                responseTable.get(rowIndex).add(value.getDataAsString()); // REMOVED COLUMN INDEX FOR NOW
                rowIndex++; // Add next value at same column index on next row
            }
        }
        return responseTable;
    }

    public ArrayList<ArrayList<String>> addNewIDRows(ArrayList<ArrayList<String>> responseTable, int rowsToAdd){
        if (responseTable.isEmpty()){ // If no ID rows have been added yet, you need to add the header
            ArrayList<String> row = new ArrayList<>();
            row.add("id");
            responseTable.add(row);
        }

        for (int i = 0; i < rowsToAdd; i++){
            ArrayList<String> row = new ArrayList<>();
            int rowID = i+1;
            row.add(String.valueOf(rowID)); // Add an ID value at the start of each row
            responseTable.add(row); // Add each row to the table
        }
        return responseTable;
    }
}
