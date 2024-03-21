package edu.uob;

import java.util.ArrayList;

public class ResponseTableGenerator {
    public ResponseTableGenerator(){}

    public ArrayList<ArrayList<String>> createConditionedResponseTable(ArrayList<ArrayList<Attribute>> selectedAttributes){
        // Create a initialise the responseTable arraylist
        ArrayList<ArrayList<String>> responseTable = new ArrayList<>();
        for (int i = 0; i < selectedAttributes.size(); i++){
            ArrayList<String> row = new ArrayList<>();
            responseTable.add(row);
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

    public ArrayList<ArrayList<String>> createUnconditionedResponseTable(ArrayList<Attribute> selectedAttributes){
        // Find the number of row to add to response table + 1 to account for attribute header
        int columnSize = selectedAttributes.get(0).allValues.size() + 1;

        // Create and initialise the response table
        ArrayList<ArrayList<String>> responseTable = new ArrayList<>();
        for (int i = 0; i < columnSize; i++){
            ArrayList<String> row = new ArrayList<>();
            responseTable.add(row);
        }

        for (Attribute attribute : selectedAttributes){ // For each selected attribute add their corresponding values
            responseTable.get(0).add(attribute.getDataAsString());
            int rowIndex = 1; // Start at row 1 for value input to skip header row
            for (Value value : attribute.allValues){
                // Add all the corresponding values for that attribute to the column
                responseTable.get(rowIndex).add(value.getDataAsString());
                rowIndex++; // Add next value at same column index on next row
            }
        }
        return responseTable;
    }


    public ArrayList<ArrayList<String>> createJoinedResponseTable(ArrayList<Attribute> t1SelectedAttributes,
         ArrayList<Attribute> t2SelectedAttributes, Attribute t1Attribute, Attribute t2Attribute){

        ArrayList<ArrayList<String>> responseTable = createNewHeaderRow(t1SelectedAttributes, t2SelectedAttributes);
        int rowsToAdd = Math.max(t1Attribute.allValues.size(), t2Attribute.allValues.size()); // Find the number of rows to add
        responseTable = addNewIDRows(responseTable, rowsToAdd); // Generate the appropriate number of new id rows

        // Add the left side of table, all Values in selected attributes of table1
        for (Attribute t1A : t1SelectedAttributes){
            int rowIndex = 1;
            for (Value t1AValue : t1A.allValues){
                responseTable.get(rowIndex).add(t1AValue.getDataAsString());
                rowIndex++; // Add next value at same column index on next row
            }
        }

        // Determine the sequence to add the table 2 values in and the number of matching columns
        ArrayList<Integer> rowIndexSequence = findRowIndexSequenceForJoin(t1Attribute, t2Attribute);
        for (Attribute columnAttribute : t2SelectedAttributes) {
            for (int i = 0; i < rowIndexSequence.size(); i++) {
                // Add the appropriate value at the appropriate index
                int rowIndex = rowIndexSequence.get(i);
                Value valueToAdd = columnAttribute.allValues.get(i);
                responseTable.get(rowIndex).add(valueToAdd.getDataAsString());
            }
        }
        return responseTable;
    }

    public ArrayList<ArrayList<String>> createNewHeaderRow(ArrayList<Attribute> t1SelectedAttributes, ArrayList<Attribute> t2SelectedAttributes){
        ArrayList<String> headerRow = new ArrayList<>();

        headerRow.add("id"); // Add the new id column header
        for (Attribute t1A : t1SelectedAttributes){
            String headerName = t1A.parent.name + "." + t1A.name;
            headerRow.add(headerName);
        }
        for (Attribute t2A : t2SelectedAttributes){
            String headerName = t2A.parent.name + "." + t2A.name;
            headerRow.add(headerName);
        }
        ArrayList<ArrayList<String>> responseTable = new ArrayList<>();
        responseTable.add(headerRow);
        return responseTable;
    }

    public ArrayList<ArrayList<String>> addNewIDRows(ArrayList<ArrayList<String>> responseTable, int rowsToAdd){
        for (int i = 0; i < rowsToAdd; i++){
            ArrayList<String> row = new ArrayList<>();
            int rowID = i+1;
            row.add(String.valueOf(rowID)); // Add an ID value at the start of each row
            responseTable.add(row); // Add each row to the table
        }
        return responseTable;
    }

    public ArrayList<Integer> findRowIndexSequenceForJoin(Attribute t1Attribute, Attribute t2Attribute){
        ArrayList<Integer> rowIndexSequence = new ArrayList<>();

        for (Value t2AValue : t2Attribute.allValues){ // Iterate through all values in the joining column
            int rowIndex = 1;
            for (Value t1AValue : t1Attribute.allValues){ // Find the rowIndex it should be joined onto
                if (t2AValue.getDataAsString().equals(t1AValue.getDataAsString())){
                    rowIndexSequence.add(rowIndex); // Store the sequence of storing the values of table 2
                }
                rowIndex++;
            }
        }
        return rowIndexSequence;
    }
}
