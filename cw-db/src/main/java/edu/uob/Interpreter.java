package edu.uob;

import java.io.IOException;
import java.util.ArrayList;

public class Interpreter {
    private final String[] commands;
    private int index;
    private final DBSession currentSession;
    public boolean responseRequired;
    public ArrayList<ArrayList<String>> responseTable;

    public Interpreter(String[] commandTokens, DBSession current) {
        this.currentSession = current;
        this.commands = commandTokens;
        this.index = 0;
        this.responseRequired = false; // Indicates whether a response table should be generated
        this.responseTable  = new ArrayList<>();
    }

    public void interpretCommand(int commandStartIndex) throws IOException {
        this.index = commandStartIndex; // Start at the next valid command to process
        String uppercaseCommand = commands[this.index].toUpperCase();

        switch (uppercaseCommand) {
            case "USE" -> executeUse();
            case "CREATE" -> executeCreate();
            case "DROP" -> executeDrop();
            case "ALTER" -> executeAlter();
            case "INSERT" -> executeInsert();
            case "SELECT" -> executeSelect();
            case "UPDATE" -> executeUpdate();
            case "DELETE" -> executeDelete();
            case "JOIN" -> executeJoin();
            default -> throw new IOException("Invalid command type");
        }
    }

    private void executeUse() throws IOException {
        this.index++;
        if (currentSession.dbExists(commands[this.index])){
            Database currentDatabase = currentSession.getDatabaseByName(commands[this.index]);
            currentSession.setDatabaseInUse(currentDatabase);
        } else {
            throw new IOException("Cannot <USE> a database that does not exist");
        }
    }

    private void executeCreate() throws IOException {
        this.index++; // Skip past "CREATE"
        if (commands[this.index].equalsIgnoreCase("DATABASE")){
            executeCreateDatabase();
        } else if (commands[this.index].equalsIgnoreCase("TABLE")){
            executeCreateTable();
        } else {
            throw new IOException("<CREATE> command is only applicable to tables and databases");
        }
    }

    private void executeCreateDatabase() throws IOException {
        this.index++; // Skip past "DATABASE"
        if (!currentSession.dbExists(commands[this.index])){ // Check if database already exists
            currentSession.createDatabase(commands[this.index]); // Create a database and directory
            currentSession.createDatabaseDirectory(commands[this.index]);
        } else {
            throw new IOException("Cannot <CREATE> a database with a databaseName that already exists");
        }
    }

    private void executeCreateTable() throws IOException {
        this.index++;

        Database currentDatabase = currentSession.getDatabaseInUse();
        if (currentDatabase == null){
            throw new IOException("Cannot <CreateTable> as no database is currently selected");
        }

        if (currentDatabase.tableExists(commands[this.index])) { // Check if table already exists
            throw new IOException("Cannot <CREATE> a table with a name that already exists within your current database");
        }

        String tableName = commands[this.index];
        Table currentTable = currentDatabase.createTable(tableName, false);
        currentDatabase.createTableFile(commands[this.index], currentTable);
        this.index++;

        if (commands[this.index].equals("(")){
            this.index++;
            storeAttributeList(currentDatabase, currentTable);
        }
        currentTable.writeAttributesToFile();
    }

    private void storeAttributeList(Database currentDatabase, Table currentTable) throws IOException {
        ArrayList<String> addedValues = new ArrayList<>();
        while (!commands[this.index].equals(")")){
            if (commands[this.index].equals(",")){ this.index++; } // Skip over the commas in attribute list

            if (attributeHasNotAppearedYet(addedValues, commands[this.index], currentTable)){
                // Check the name is not a repeat
                currentTable.createAttribute(commands[this.index]);
                addedValues.add(commands[this.index]);
            } else {
                currentDatabase.deleteTable(currentTable.getTableName()); // Delete everything you created if you encounter an error
                throw new IOException("An attribute with that name already exists in table");
            }
            this.index++;
        }
    }

    private boolean attributeHasNotAppearedYet(ArrayList<String> addedValues, String attribute, Table currentTable) {
        // Ensure no repeat attribute names
        if (currentTable.attributeExists(attribute)){
            return false;
        }

        if (!addedValues.isEmpty()){
            for (String s : addedValues){
                if (attribute.equalsIgnoreCase(s)){
                    return false;
                }
            }
        }
        return true;
    }

    private void executeDrop() throws IOException {
        // "DROP " "DATABASE " [DatabaseName] | "DROP " "TABLE " [TableName]
        this.index++; // Skip past "DROP"
        if (commands[this.index].equalsIgnoreCase("DATABASE")){
            executeDropDatabase();
        } else if (commands[this.index].equalsIgnoreCase("TABLE")){
            executeDropTable();
        } else {
            throw new IOException("<DROP> command is only applicable to tables and databases");
        }
    }

    private void executeDropDatabase() throws IOException {
        this.index++; // Skip past "DATABASE"
        if (currentSession.dbExists(commands[this.index])){ // Check for database existence
            currentSession.deleteDatabase(commands[this.index]);
        } else {
            throw new IOException("Cannot <DROP> a database which does not exist");
        }
    }

    private void executeDropTable() throws IOException {
        this.index++; // Skip past "TABLE"

        Database currentDatabase = currentSession.getDatabaseInUse();
        if (currentDatabase == null) { // Check a database in use
            throw new IOException("Cannot <DROP> table as no database is currently selected");
        }

        if (currentDatabase.tableExists(commands[this.index])){ // Check table's existence
            currentDatabase.deleteTable(commands[this.index]); // Delete the table
        } else {
            throw new IOException("Cannot <DROP> a table which does not exist");
        }
    }

    private void executeAlter() throws IOException {
        //  "ALTER " "TABLE " [TableName] " " <AlterationType> " " [AttributeName]
        this.index = this.index+2; // Skip "ALTER " "TABLE " safely as already parsed

        Table currentTable = findCurrentTable("<ALTER> a table");

        // Find out which alteration type is desired
        int nextIndex = this.index+1;
        String alterationType = commands[nextIndex];

        if (alterationType.equalsIgnoreCase("ADD")){
            this.index = nextIndex+1; // Skip past alteration type and execute
            executeAlterAdd(currentTable);
        } else if (alterationType.equalsIgnoreCase("DROP")){
            this.index = nextIndex+1;
            executeAlterDrop(currentTable);
        } else {
            throw new IOException("Invalid alteration type attempted as part of an <ALTER> command");
        }
    }

    private void executeAlterAdd(Table currentTable) throws IOException {
        String attributeName = commands[this.index];
        checkForMultipleAlterationAttempts(this.index + 1); // Ensure no attempt at multiple alteration
        if (!currentTable.attributeExists(attributeName)){ // Check such an attribute does not already exist
            currentTable.createAttribute(attributeName); // Create new attribute
        } else {
            throw new IOException("Cannot <ALTER> a table by adding an attribute which already exist");
        }
    }

    private void executeAlterDrop(Table currentTable) throws IOException {
        String attributeName = commands[this.index];
        checkForMultipleAlterationAttempts(this.index + 1); // Ensure no attempt at multiple alteration
        if (currentTable.attributeExists(attributeName)){ // Ensure such an attribute does exist
            if (attributeName.equalsIgnoreCase("id")){
                throw new IOException("Cannot <ALTER> the id column of a table"); // Ensure no attempt to alter id column
            } else {
                currentTable.deleteAttribute(attributeName); // Delete the attribute
            }
        } else {
            throw new IOException("Cannot <ALTER> a table by dropping an attribute which does not exist");
        }
    }

    private void checkForMultipleAlterationAttempts(int nextIndex) throws IOException {
        if (!commands[nextIndex].equals(";")){
            throw new IOException("Cannot perform multiple <ALTER> commands at once");
        }
    }

    private void executeInsert() throws IOException {
        // "INSERT " "INTO " [TableName] " VALUES" "(" <ValueList> ")"
        this.index = this.index+2; // Safely skip "INSERT " "INTO " as parsed successfully
        Table currentTable = findCurrentTable("<INSERT> into a table");
        this.index = this.index+3; // Navigate to first value in list by skipping " VALUES" & opening bracket

        ArrayList<String> valuesInValueList = storeValuesInValueList(currentTable); // Store all values in commands

        if (!valuesInValueList.isEmpty()){
            currentTable.storeValueRow(valuesInValueList);
        } else {
            throw new IOException("Couldn't input values into table contents"); // For debugging replace later
        }
    }

    private ArrayList<String> storeValuesInValueList(Table currentTable) throws IOException {
        ArrayList<String> valuesInValueList = new ArrayList<>();
        while (!commands[this.index].equals(")")){
            if (commands[this.index].equals(",")){ this.index++; } // Safely skip commas as already parsed
            if (isStringLiteral(commands[this.index])){ // If the value is a string literal, remove the quotes before storing
                commands[this.index] = removeQuotesFromStringLiteral(commands[this.index]);
            }
            valuesInValueList.add(commands[this.index]);
            this.index++;
        }

        if ((valuesInValueList.size() + 1) > currentTable.tableContents.size()){
            throw new IOException("Cannot input more <VALUES> than there are attributes in the table");
        }
        if ((valuesInValueList.size() + 1) < currentTable.tableContents.size()){
            throw new IOException("Cannot input less <VALUES> than there are attributes in the table");
        }
        return valuesInValueList;
    }

    private boolean isStringLiteral(String token){
        return (token.charAt(0) == '\'' && token.charAt(token.length() - 1) == '\'');
    }

    private String removeQuotesFromStringLiteral(String token){
        return token.substring(1, token.length() - 1);
    }

    private void executeSelect() throws IOException {
        this.index++; // Skip past "SELECT"

        Database currentDatabase = currentSession.getDatabaseInUse();
        if (currentDatabase == null) {
            throw new IOException("Cannot <SELECT> as no database is currently selected");
        }

        ArrayList<Attribute> selectedAttributes = selectAttributes(currentDatabase);
        Table currentTable = currentDatabase.getTableByName(commands[this.index]);

        ResponseTableGenerator responseGenerator = new ResponseTableGenerator();
        int nextIndex = this.index+1; // Check whether a conditioned select
        if (commands[nextIndex].equalsIgnoreCase("WHERE")){
            this.index = nextIndex+1; // Skip past "WHERE"
            // Generate a response table based on the conditionedValues
            ArrayList<ArrayList<Attribute>> conditionedValues = conditionSelectedAttributes(selectedAttributes, currentTable);
            this.responseRequired = true;
            this.responseTable = responseGenerator.createConditionedResponseTable(conditionedValues);
        } else {
            // Generate a response table of all values
            this.responseRequired = true;
            this.responseTable = responseGenerator.createUnconditionedResponseTable(selectedAttributes);
        }
    }

    private ArrayList<Attribute> selectAttributes(Database currentDatabase) throws IOException {
        // Store wild attribute list
        boolean selectAll = false;
        ArrayList<String> wildAttributeList = new ArrayList<>();
        while (!commands[this.index].equalsIgnoreCase("FROM")){
            if (commands[this.index].equals("*")){
                selectAll = true; // Mark a select all command for simplicity
            } else {
                if (commands[this.index].equals(",")){ this.index++; } // Skip commas
                wildAttributeList.add(commands[this.index]); // Store all attributes listed
            }
            this.index++;
        }
        this.index++; // skip past "FROM"

        if (!currentDatabase.tableExists(commands[this.index])) {
            throw new IOException("Cannot <SELECT> from a table that does not exist");
        }

        Table currentTable = currentDatabase.getTableByName(commands[this.index]);
        if (selectAll){
            return currentTable.getAllAttributes();
        } else {
            ArrayList<Attribute> selectedAttributes = new ArrayList<>();
            for (String attributeName : wildAttributeList){ // Ensure all attributes in list exist within table
                if (currentTable.attributeExists(attributeName)){
                    selectedAttributes.add(currentTable.getAttributeFromName(attributeName)); // Add them to selectedAttributes
                } else {
                    throw new IOException("Cannot <SELECT> an attribute that does not exist");
                }
            }
            return selectedAttributes;
        }
    }

    private ArrayList<ArrayList<Attribute>> conditionSelectedAttributes(ArrayList<Attribute> selectedAttributes, Table currentTable) throws IOException {
        ArrayList<String> allConditions = storeConditions();
        ArrayList<ArrayList<Attribute>> conditionedValues = new ArrayList<>();
        // Add the attributes to the top row of the response table, so header row is always returned regardless of conditions
        conditionedValues.add(selectedAttributes);

        for (Attribute attribute : selectedAttributes){
            int rowIndex = 1; // Reset to 1 (first row of values) for each column
            for (Value value : attribute.allValues){ // Add values to the response if they meet the conditions
                ConditionProcessor conditionProcessor = new ConditionProcessor();
                if (conditionProcessor.checkRowMeetsConditions(allConditions, value, currentTable)){
                    if (rowIndex >= conditionedValues.size()){ // Add a new row to arraylist if necessary
                        ArrayList<Attribute> row = new ArrayList<>();
                        conditionedValues.add(row);
                    }
                    conditionedValues.get(rowIndex).add(value); // Store the conditioned values in their appropriate arrangement
                    rowIndex++; // increment for each value in attribute that is selected and stored
                }
            }
        }
        return conditionedValues;
    }

    private ArrayList<String> storeConditions(){
        ArrayList<String> allConditions = new ArrayList<>();

        while (!commands[this.index].equals(";")){
            if (isStringLiteral(commands[this.index])){ // If the value is a string literal, remove the quotes before storing
                commands[this.index] = removeQuotesFromStringLiteral(commands[this.index]);
            }
            allConditions.add(commands[this.index]); // Add all condition strings to separate arraylist
            this.index++;
        }
        return allConditions;
    }

    private void executeUpdate() throws IOException {
        // [TableName] " SET " <NameValueList> " WHERE " <Condition>
        this.index++; // Skip past "UPDATE"
        Table currentTable = findCurrentTable("<UPDATE> a table");
        this.index = this.index + 2; // Skip to <NameValueList>
        ArrayList<String> nameValueList = storeNameValueList();
        if (commands[this.index].equalsIgnoreCase("WHERE")){ this.index++; }
        applyUpdates(nameValueList, currentTable);
    }

    private ArrayList<String> storeNameValueList(){
        // <NameValueList>   ::=  <NameValuePair> | <NameValuePair> "," <NameValueList>
        ArrayList<String> nameValueList = new ArrayList<>();
        while (!commands[this.index].equalsIgnoreCase("WHERE")){
            if (commands[this.index].equals(",")){ this.index++; } // Skip past commas
            if (isStringLiteral(commands[this.index])){
                commands[this.index] = removeQuotesFromStringLiteral(commands[this.index]);
            }
            nameValueList.add(commands[this.index]); // Store all nameValuePairs
            this.index++;
        }
        return nameValueList;
    }

    private void applyUpdates(ArrayList<String> nameValueList, Table currentTable) throws IOException {
        Attribute idColumn = currentTable.getAttributeFromName("id");
        ArrayList<String> allConditions = storeConditions();
        ConditionProcessor conditionProcessor = new ConditionProcessor();

        for (int nameIndex = 0, valueIndex = 2; valueIndex < nameValueList.size(); nameIndex = nameIndex + 3, valueIndex = valueIndex + 3) {
            for (Value id : idColumn.allValues) {
                if (conditionProcessor.checkRowMeetsConditions(allConditions, id, currentTable)){ // Check if each valueRow meets the conditions
                    String attributeName = nameValueList.get(nameIndex); // Get the attributeName of the current nameValuePair
                    String newValue = nameValueList.get(valueIndex); // Get the reassigned value of the current nameValuePair
                    checkValidityOfReassignment(currentTable, attributeName); // Check for invalid reassignments
                    currentTable.updateValue(id.getDataAsString(), attributeName, newValue); // Reassign the corresponding value in attribute column
                }
            }
        }
    }

    private void checkValidityOfReassignment(Table currentTable, String attributeName) throws IOException {
        if (!currentTable.attributeExists(attributeName)){ // Ensure attribute exists
            throw new IOException("Cannot <UPDATE> an attribute that does not exist");
        } else if (attributeName.equalsIgnoreCase("id")) { // Ensure not an attempt to change id column
            throw new IOException("Cannot <UPDATE> the id column of a table");
        }
    }

    private void executeDelete() throws IOException {
        // "DELETE " "FROM " [TableName] " WHERE " <Condition>
        this.index = this.index+2; // Skip past "DELETE" & "FROM"
        Table currentTable = findCurrentTable("<DELETE> a table");
        this.index = this.index+2; // Skip past "WHERE" safely as already parsed

        // Take a copy of column to avoid concurrent mod exception when modifying tableValues in the loop
        Attribute idColumnHeader = currentTable.getAttributeFromName("id");
        ArrayList<Value> idColumnToModify = new ArrayList<>(idColumnHeader.allValues);

        // Store allConditions applied to deletion
        ArrayList<String> allConditions = storeConditions();
        ConditionProcessor conditionProcessor = new ConditionProcessor();

        for (Value id : idColumnToModify) { // Check if each valueRow meets the conditions
            if (conditionProcessor.checkRowMeetsConditions(allConditions, id, currentTable)){
                currentTable.deleteRow(id.getDataAsString()); // Delete row if conditions satisfied
            }
        }
    }

    private void executeJoin() throws IOException {
        //"JOIN " [TableName] " AND " [TableName] " ON " [AttributeName] " AND " [AttributeName]

        // Assign the table objects and their corresponding attributes
        this.index++; // Skip "JOIN" to first table name
        Table table1 = findCurrentTable("<JOIN> a table");
        this.index = this.index+2; // Skip past "AND" to next table name
        Table table2 = findCurrentTable("<JOIN> a table");
        this.index = this.index+2; // Skip past "ON" to first attribute name
        String table1Attribute = commands[this.index];
        this.index = this.index+2; // Skip past "AND" to next attribute name
        String table2Attribute = commands[this.index];

        if (!table1.attributeExists(table1Attribute) || !table2.attributeExists(table2Attribute)){
            throw new IOException("Cannot <JOIN> a table on an attribute which does not exist");
        }

        // Find the joining attributes and select the other attributes in tables to add
        Attribute t1Attribute = table1.getAttributeFromName(table1Attribute);
        Attribute t2Attribute = table2.getAttributeFromName(table2Attribute);

        ArrayList<Attribute> selectedAttributes = selectAttributesToJoin(table1, table1Attribute);
        ArrayList<Attribute> table2SelectedAttributes = selectAttributesToJoin(table2, table2Attribute);
        selectedAttributes.addAll(table2SelectedAttributes); // Combine all selected attributes from both tables

        if (selectedAttributes.isEmpty()){ // Determine validity of selection
            throw new IOException("Cannot execute <JOIN> on tables without any attributes");
        }

        ResponseTableGenerator responseGenerator = new ResponseTableGenerator();
        this.responseRequired = true; // Generate the appropriate response table and the requirement of response
        this.responseTable = responseGenerator.createJoinedTable(selectedAttributes, t1Attribute, t2Attribute);
    }

    private ArrayList<Attribute> selectAttributesToJoin(Table table, String attributeToJoin){
        ArrayList<Attribute> selectedAttributes = new ArrayList<>();

        // Select all attributes but the joining attribute and id column
        for (Attribute a : table.getAllAttributes()){
            if (!a.getDataAsString().equalsIgnoreCase("id") && !a.getDataAsString().equals(attributeToJoin)){
                selectedAttributes.add(a);
            }
        }
        return selectedAttributes;
    }

    private Table findCurrentTable(String errorMessage) throws IOException {
        // Determine the current use of a database
        Database currentDatabase = currentSession.getDatabaseInUse();
        if (currentDatabase == null) {
            throw new IOException("Cannot " +errorMessage + " as no database is currently selected");
        }

        // Determine desired table exists and is within selected database
        String tableName = commands[this.index];
        if (!currentDatabase.tableExists(tableName)) {
            throw new IOException("Cannot " +errorMessage + " which does not exist");
        }
        return currentDatabase.getTableByName(tableName); // Return the currentTable for querying
    }

}

