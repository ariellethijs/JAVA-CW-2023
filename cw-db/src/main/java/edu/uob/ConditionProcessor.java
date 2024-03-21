package edu.uob;

import java.io.IOException;
import java.util.ArrayList;

public class ConditionProcessor {
    Table currentTable;

    public ConditionProcessor(){}

    public boolean checkRowMeetsConditions(ArrayList<String> allConditions, Value value, Table current) throws IOException {
        currentTable = current; // Store the current table for evaluating conditions when broken down
        int rowIndex = currentTable.getRowIndexFromID(value.correspondingID);

        ArrayList<Attribute> valueRow = new ArrayList<>();

        for (int colIndex = 0; colIndex < currentTable.tableContents.size(); colIndex++){
            if (rowIndex < currentTable.tableContents.get(colIndex).size()){
                valueRow.add(currentTable.tableContents.get(colIndex).get(rowIndex)); // Add all the values in the same row
            }
        }
        return evaluateConditions(allConditions, valueRow);
    }

    public boolean evaluateConditions(ArrayList<String> allConditions, ArrayList<Attribute> valueRow) throws IOException {
        if (allConditions.get(0).equals("(") && allConditions.get(allConditions.size() - 1).equals(")")){
            if (canRemoveExtraneousBrackets(allConditions)){
                allConditions = new ArrayList<>(allConditions.subList(1, allConditions.size() - 1));
                return evaluateConditions(allConditions, valueRow);
            }
        }

        int opIndex = findNextBooleanOperator(allConditions); // Finds the nested

        if (opIndex == -1) {
            return evaluateSimpleCondition(allConditions, valueRow); // No more booleanOperators left, determine the result of condition
        }

        String dividingOperator = allConditions.get(opIndex); // Store the operator

        // Store the separate conditions (could still be nested conditions within)
        ArrayList<String> condition1 = new ArrayList<>(allConditions.subList(0, opIndex));
        ArrayList<String> condition2 = new ArrayList<>(allConditions.subList((opIndex + 1), allConditions.size()));

        // Recursively find the result of each condition
        boolean result1 = evaluateConditions(condition1, valueRow);
        boolean result2 = evaluateConditions(condition2, valueRow);

        if (dividingOperator.equalsIgnoreCase("AND")){
            return (result1 && result2);
        } else if (dividingOperator.equalsIgnoreCase("OR")) {
            return (result1 || result2);
        } else {
            throw new IOException("Invalid Boolean Operator in condition: " +dividingOperator);
        }
    }

    public boolean canRemoveExtraneousBrackets(ArrayList<String> allConditions){
        ArrayList<String> allConditionsWithoutOuterBrackets = new ArrayList<>(allConditions.subList(1, allConditions.size() - 1));
        ArrayList<String> brackets = new ArrayList<>();

        int openBracketCount = 0;

        // Determine whether brackets can be safely removed
        for (String condition : allConditionsWithoutOuterBrackets){
            if (condition.equals("(")){
                openBracketCount++;
                brackets.add(condition);
            }
            if (condition.equals(")")){
                openBracketCount--;
                brackets.add(condition);
            }
        }

        boolean bracketsOrderCorrect = true;

        if (!brackets.isEmpty()){
            // If there's brackets left inside condition, check they're not left hanging
            bracketsOrderCorrect = !brackets.get(0).equals(")") && !brackets.get(brackets.size() - 1).equals("(");
        }

        return (openBracketCount == 0 && bracketsOrderCorrect);
    }

    public int findNextBooleanOperator(ArrayList<String> allConditions){
        int bracketCount = 0; // To ensure that the boolean you process first is not one w/in a nested condition
        // E.g., for (condition AND condition) OR (condition AND condition), OR would be selected first

        for (int index = 0; index < allConditions.size(); index++){
            if (allConditions.get(index).equals("(")){
                bracketCount++;
            } else if (allConditions.get(index).equals(")")){
                bracketCount--;
            } else if ((allConditions.get(index).equalsIgnoreCase("AND") ||
                    allConditions.get(index).equalsIgnoreCase("OR")) && bracketCount == 0) {
                return index;
            }
        }
        return -1;
    }

    public boolean evaluateSimpleCondition(ArrayList<String> condition, ArrayList<Attribute> valueRow) throws IOException {
        // Find the columnIndex the attribute resides in the table
        int attributeIndex = currentTable.getAttributeIndexFromName(condition.get(0));
        // This index is the index of the valueRow which needs to be tested
        String attributeValue = valueRow.get(attributeIndex).getDataAsString();
        String comparator = condition.get(1);
        String value = condition.get(2);

        //noinspection EnhancedSwitchMigration
        switch (comparator.toUpperCase()) {
            case "==":
                return attributeValue.equals(value);
            case ">":
                return evaluateGreaterThan(attributeValue, value);
            case "<":
                return evaluateLessThan(attributeValue, value);
            case ">=":
                return (evaluateGreaterThan(attributeValue, value) || attributeValue.equals(value));
            case "<=":
                return (evaluateLessThan(attributeValue, value) || attributeValue.equals(value));
            case "!=":
                return !attributeValue.equals(value);
            case "LIKE":
                return attributeValue.contains(value);
            default:
                throw new IOException("Invalid comparator: " + comparator);
        }
    }

    public boolean evaluateGreaterThan(String attributeValue, String value){
        try {
            return Float.parseFloat(attributeValue) > Float.parseFloat(value);
        } catch (NumberFormatException e) {
            // Not a valid numeric comparison, return false
            return false;
        }
    }

    public boolean evaluateLessThan(String attributeValue, String value){
        try {
            return Float.parseFloat(attributeValue) < Float.parseFloat(value);
        } catch (NumberFormatException e) {
            // Not a valid numeric comparison, return false
            return false;
        }
    }
    
}
