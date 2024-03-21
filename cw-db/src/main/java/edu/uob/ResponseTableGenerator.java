package edu.uob;

import java.util.ArrayList;

public class ResponseTableGenerator {
    public ResponseTableGenerator(){}

    public ArrayList<ArrayList<String>> createConditionedResponseTable(ArrayList<ArrayList<Attribute>> selectedAttributes){
        // Create and initialise the responseTable arraylist
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


    public ArrayList<ArrayList<String>> createJoinedTable(ArrayList<Attribute> selectedAttributes, Attribute t1Attribute, Attribute t2Attribute){

        ArrayList<ArrayList<String>> responseTable = createNewHeaderRow(selectedAttributes);

        // Determine the sequence to add the table 1 and table 2 values in
        ArrayList<Integer> t1RowsToAdd = findValueIndexesToStore(t1Attribute, t2Attribute);
        ArrayList<Integer> t2rowSequence = findRowIndexSequenceForJoin(t1RowsToAdd, t1Attribute, t2Attribute);

        int rowsToAdd = t1RowsToAdd.size();
        responseTable = addNewIDRows(responseTable, rowsToAdd); // Generate the appropriate number of new id rows

        for (Attribute columnAttribute : selectedAttributes) {

            if (columnAttribute.parent == t1Attribute.parent){
                for (int i = 0; i < t1RowsToAdd.size(); i++){
                    int valueIndex = t1RowsToAdd.get(i);
                    Value valueToAdd = columnAttribute.allValues.get(valueIndex);
                    responseTable.get(i+1).add(valueToAdd.getDataAsString());
                }
            } else if (columnAttribute.parent == t2Attribute.parent){
                responseTable = storeValuesAccordingToRowSequence(responseTable, t2rowSequence, columnAttribute);
            }
        }
        return responseTable;
    }

    public ArrayList<ArrayList<String>> createNewHeaderRow(ArrayList<Attribute> selectedAttributes){
        ArrayList<String> headerRow = new ArrayList<>();

        headerRow.add("id"); // Add the new id column header
        for (Attribute a : selectedAttributes){
            String headerName = a.parent.name + "." + a.name;
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

    public ArrayList<Integer> findRowIndexSequenceForJoin(ArrayList<Integer> t1RowsToAdd, Attribute t1Attribute, Attribute t2Attribute){

        ArrayList<Value> newT1AttributeValues = new ArrayList<>();
        for (Integer integer : t1RowsToAdd) { // Store a new sub arraylist of all the values in table 1 which have a matching t2 value
            newT1AttributeValues.add(t1Attribute.allValues.get(integer));
        }

        ArrayList<Integer> rowIndexSequence = new ArrayList<>();
        for (Value t2Value : t2Attribute.allValues){ // Iterate through all values in the joining column
            int rowIndex = 1;
            for (Value t1Value : newT1AttributeValues){ // Find the rowIndex it should be joined onto
                if (t2Value.getDataAsString().equals(t1Value.getDataAsString())){
                    rowIndexSequence.add(rowIndex); // Store the sequence of storing the values of table 2
                }
                rowIndex++;
            }
        }
        return rowIndexSequence;
    }

    public ArrayList<ArrayList<String>> storeValuesAccordingToRowSequence(ArrayList<ArrayList<String>> responseTable,
                            ArrayList<Integer> rowSequence, Attribute columnAttribute){
        for (int i = 0; i < rowSequence.size(); i++) {
            // Add the appropriate value at the appropriate index
            int rowIndex = rowSequence.get(i);
            Value valueToAdd = columnAttribute.allValues.get(i);
            responseTable.get(rowIndex).add(valueToAdd.getDataAsString());
        }
        return responseTable;
    }

    public ArrayList<Integer> findValueIndexesToStore(Attribute t1Attribute, Attribute t2Attribute){
        ArrayList<Integer> rowIndexesToStore = new ArrayList<>();
        for (Value t1Value : t1Attribute.allValues){
            for (Value t2Value : t2Attribute.allValues){
                if (t1Value.getDataAsString().equals(t2Value.getDataAsString())){
                    rowIndexesToStore.add(t1Attribute.allValues.indexOf(t1Value));
                }
            }
        }
        return rowIndexesToStore;
    }


}
