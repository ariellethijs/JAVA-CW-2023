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

        this.responseRequired = false;
        this.responseTable  = new ArrayList<>();
    }

    public void interpretCommand(int commandStartIndex) throws IOException {
        this.index = commandStartIndex;
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
            default -> throw new IOException("Attempting to interpret unimplemented command");
        }
    }

    public void executeUse() throws IOException {
        this.index++;
        if (currentSession.dbExists(commands[this.index])){
            Database currentDatabase = currentSession.getDatabaseByName(commands[this.index]);
            currentSession.setDatabaseInUse(currentDatabase);
        } else {
            throw new IOException("Cannot <USE> a database that does not exist");
        }
    }

    public void executeCreate() throws IOException {
        this.index++;

        if (commands[this.index].equalsIgnoreCase("DATABASE")){
            executeCreateDatabase();
        } else if (commands[this.index].equalsIgnoreCase("TABLE")){
            executeCreateTable();
        } else {
            throw new IOException("<CREATE> command is only applicable to tables and databases");
        }
    }

    public void executeCreateDatabase() throws IOException {
        this.index++;
        if (!currentSession.dbExists(commands[this.index])){
            currentSession.createDatabase(commands[this.index]);
            currentSession.createDatabaseDirectory(commands[this.index]);
        } else {
            throw new IOException("Cannot <CREATE> a database with a databaseName that already " +
                    "exists within your file structure");
        }
    }

    public void executeCreateTable() throws IOException {
        this.index++;

        Database currentDatabase = currentSession.getDatabaseInUse();
        if (currentDatabase == null){
            throw new IOException("Cannot <CreateTable> as no database is currently selected");

        } else if (!currentDatabase.tableExists(commands[this.index])){
            Table currentTable = currentDatabase.createTable(commands[this.index], false);
            currentDatabase.createTableFile(commands[this.index], currentTable);
            this.index++;
            if (commands[this.index].equals("(")){
                this.index++;
                while (!commands[this.index].equals(")")){
                    if (!commands[this.index].equals(",")){ // Skip over the commas in attribute list
                        currentTable.createAttribute(commands[this.index]);
                    }
                    this.index++;
                }
                currentTable.writeAttributesToFile();
            }
        } else {
            throw new IOException("Cannot <CREATE> a table with a name that already exists within your current database");
        }
    }

    public void executeDrop() throws IOException {
        // "DROP " "DATABASE " [DatabaseName] | "DROP " "TABLE " [TableName]
        this.index++;
        if (commands[this.index].equalsIgnoreCase("DATABASE")){
            executeDropDatabase();
        } else if (commands[this.index].equalsIgnoreCase("TABLE")){
            executeDropTable();
        } else {
            throw new IOException("<DROP> command is only applicable to tables and databases");
        }
    }

    public void executeDropDatabase() throws IOException {
        this.index++;
        if (currentSession.dbExists(commands[this.index])){
            currentSession.deleteDatabase(commands[this.index]);
        } else {
            throw new IOException("Cannot <DROP> a database which does not exist");
        }
    }

    public void executeDropTable() throws IOException {
        this.index++;

        Database currentDatabase = currentSession.getDatabaseInUse();
        if (currentDatabase == null) {
            throw new IOException("Cannot <DROP> table as no database is currently selected");

        } else if (currentDatabase.tableExists(commands[this.index])){
            currentDatabase.deleteTable(commands[this.index]);
        } else {
            throw new IOException("Cannot <DROP> a table which does not exist");
        }
    }

    public void executeAlter() throws IOException {
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

    public void executeAlterAdd(Table currentTable) throws IOException {
        String attributeName = commands[this.index];
        if (!currentTable.attributeExists(attributeName)){
            currentTable.createAttribute(attributeName);
        } else {
            throw new IOException("Cannot <ALTER> a table by adding an attribute which already exist");
        }
    }

    public void executeAlterDrop(Table currentTable) throws IOException {
        String attributeName = commands[this.index];
        if (currentTable.attributeExists(attributeName)){
            if (attributeName.equalsIgnoreCase("id")){
                throw new IOException("Cannot <ALTER> the id column of a table");
            } else {
                currentTable.deleteAttribute(attributeName);
            }
        } else {
            throw new IOException("Cannot <ALTER> a table by dropping an attribute which does not exist");
        }
    }

    public void executeInsert() throws IOException {
        // "INSERT " "INTO " [TableName] " VALUES" "(" <ValueList> ")"
        this.index = this.index+2; // Safely skip "INSERT " "INTO " as parsed successfully
        Table currentTable = findCurrentTable("<INSERT> into a table");
        this.index = this.index+3; // Navigate to first value in list by skipping " VALUES" & opening bracket

        ArrayList<String> valuesInValueList = storeValuesInValueList(currentTable);

        if (!valuesInValueList.isEmpty()){
            currentTable.storeValueRow(valuesInValueList);
        } else {
            throw new IOException("Couldn't input values into table contents"); // For debugging replace later
        }
    }

    public ArrayList<String> storeValuesInValueList(Table currentTable) throws IOException {
        ArrayList<String> valuesInValueList = new ArrayList<>();
        while (!commands[this.index].equals(")")){
            if (!commands[this.index].equals(",")){ // if not a comma, must be a value as already parsed
                if (commands[this.index].charAt(0) == '\'' && commands[this.index].charAt(commands[this.index].length() - 1) == '\''){
                    // If the value is a string literal, remove the quotes before storing
                    commands[this.index] = removeQuotesFromStringLiteral(commands[this.index]);
                }
                valuesInValueList.add(commands[this.index]);
            }
            this.index++;
        }

        if ((valuesInValueList.size() + 1) > currentTable.getNumberOfAttributes()){
            throw new IOException("Cannot input more <VALUES> than there are attributes in the table");
        }
        if ((valuesInValueList.size() + 1) < currentTable.getNumberOfAttributes()){
            throw new IOException("Cannot input less <VALUES> than there are attributes in the table");
        }
        return valuesInValueList;
    }

    public String removeQuotesFromStringLiteral(String token){
        return token.substring(1, token.length() - 1);
    }

    public void executeSelect() throws IOException {
        this.index++; // Skip "SELECT "
        ResponseTableGenerator responseGenerator = new ResponseTableGenerator();

        Database currentDatabase = currentSession.getDatabaseInUse();
        if (currentDatabase == null) {
            throw new IOException("Cannot <SELECT> from tables as no database is currently selected");
        }

        ArrayList<Attribute> selectedAttributes = selectAttributes(currentDatabase);
        Table currentTable = findCurrentTable("<SELECT> from table");
        int nextIndex = this.index+1;

        if (selectedAttributes.isEmpty()){
            throw new IOException("No valid attributes selected"); // FOR DEBUGGING DELETE LATER
        } else if (commands[nextIndex].equalsIgnoreCase("WHERE")){
            this.index = nextIndex+1; // Skip past where
            ArrayList<ArrayList<Attribute>> conditionedValues = conditionSelectedAttributes(selectedAttributes, currentTable);
            this.responseRequired = true;
            this.responseTable = responseGenerator.createConditionedResponseTable(conditionedValues);
        } else {
            this.responseRequired = true;
            this.responseTable = responseGenerator.createUnconditionedResponseTable(selectedAttributes);
        }
    }

    public ArrayList<Attribute> selectAttributes(Database currentDatabase) throws IOException {
        boolean selectAll = false;

        // Store wild attribute list
        ArrayList<String> wildAttributeList = new ArrayList<>();
        while (!commands[this.index].equalsIgnoreCase("FROM")){
            if (commands[this.index].equals("*")){
                selectAll = true; // Mark a select all command for simplicity
                this.index++;
            } else if (commands[this.index].equals(",")){
                this.index++; // Skip commas
            } else {
                wildAttributeList.add(commands[this.index]);
                this.index++;
            }
        }
        this.index++; // skip past "FROM"

        ArrayList<Attribute> selectedAttributes = new ArrayList<>();
        if (currentDatabase.tableExists(commands[this.index])){
            Table currentTable = currentDatabase.getTableByName(commands[this.index]);

            if (selectAll){
                selectedAttributes = currentTable.getAllAttributes();
            } else {
                for (String attributeName : wildAttributeList){
                    if (currentTable.attributeExists(attributeName)){
                        selectedAttributes.add(currentTable.getAttributeFromName(attributeName));
                    } else {
                        throw new IOException("Cannot <SELECT> an attribute that does not exist");
                    }
                }
                return selectedAttributes;
            }
        } else {
            throw new IOException("Cannot <SELECT> from a table that does not exist");
        }
        return selectedAttributes; // Should never reach this but just to silence compiler
    }

    public ArrayList<ArrayList<Attribute>> conditionSelectedAttributes(ArrayList<Attribute> selectedAttributes, Table currentTable) throws IOException {
        ArrayList<String> allConditions = storeConditions();
        ArrayList<ArrayList<Attribute>> conditionedValues = new ArrayList<>();
        conditionedValues.add(selectedAttributes); // Add the attributes to the top row of the response table

        for (Attribute attribute : selectedAttributes){
            int rowIndex = 1; // Reset to 1 (first row of values) for each column
            for (Value value : attribute.allValues){
                ConditionProcessor conditionProcessor = new ConditionProcessor();
                if (conditionProcessor.checkRowMeetsConditions(allConditions, value, currentTable)){
                    if (rowIndex >= conditionedValues.size()){ // Add a new row to arraylist if necessary
                        ArrayList<Attribute> row = new ArrayList<>();
                        conditionedValues.add(row);
                    }
                    conditionedValues.get(rowIndex).add(value);
                    rowIndex++; // increment for each value in attribute that is selected and stored
                }
            }
        }
        return conditionedValues;
    }

    public ArrayList<String> storeConditions(){
        ArrayList<String> allConditions = new ArrayList<>();

        while (!commands[this.index].equals(";")){
            if (commands[this.index].charAt(0) == '\'' && commands[this.index].charAt(commands[this.index].length() - 1) == '\'') {
                // If the value is a string literal, remove the quotes before storing
                commands[this.index] = removeQuotesFromStringLiteral(commands[this.index]);
            }
            allConditions.add(commands[this.index]);
            this.index++;
        }
        return allConditions;
    }

    public void executeUpdate() throws IOException {
        // [TableName] " SET " <NameValueList> " WHERE " <Condition>
        this.index++; // Skip past "UPDATE"
        Table currentTable = findCurrentTable("<UPDATE> a table");
        this.index = this.index + 2; // Skip to <NameValueList>
        ArrayList<String> nameValueList = storeNameValueList();
        if (commands[this.index].equalsIgnoreCase("WHERE")){ this.index++; }
        applyUpdates(nameValueList, currentTable);
    }

    public ArrayList<String> storeNameValueList(){
        // <NameValueList>   ::=  <NameValuePair> | <NameValuePair> "," <NameValueList>

        ArrayList<String> nameValueList = new ArrayList<>();

        // Store all nameValuePairs
        while (!commands[this.index].equalsIgnoreCase("WHERE")){
            if (commands[this.index].equals(",")){ this.index++; } // Skip the commas
            if (commands[this.index].charAt(0) == '\'' && commands[this.index].charAt(commands[this.index].length() - 1) == '\'') {
                commands[this.index] = removeQuotesFromStringLiteral(commands[this.index]);
            }
            nameValueList.add(commands[this.index]);
            this.index++;
        }
        return nameValueList;
    }

    public void applyUpdates(ArrayList<String> nameValueList, Table currentTable) throws IOException {
        Attribute idColumn = currentTable.getAttributeFromName("id");
        ArrayList<String> allConditions = storeConditions();
        ConditionProcessor conditionProcessor = new ConditionProcessor();

        for (int nameIndex = 0, valueIndex = 2; valueIndex < nameValueList.size(); nameIndex = nameIndex + 3, valueIndex = valueIndex + 3) {
            for (Value id : idColumn.allValues) { // Check if each valueRow meets the conditions
                if (conditionProcessor.checkRowMeetsConditions(allConditions, id, currentTable)){
                    String attributeName = nameValueList.get(nameIndex);
                    String newValue = nameValueList.get(valueIndex);
                    if (!currentTable.attributeExists(attributeName)){
                        throw new IOException("Cannot <UPDATE> an attribute that does not exist");
                    } else if (attributeName.equalsIgnoreCase("id")){
                        throw new IOException("Cannot <UPDATE> the id column of a table");
                    } else {
                        currentTable.updateValue(id.getDataAsString(), attributeName, newValue);
                    }
                }
            }
        }
    }

    public void executeDelete() throws IOException {
        // "DELETE " "FROM " [TableName] " WHERE " <Condition>
        this.index = this.index+2; // Skip past "DELETE" & "FROM"
        Table currentTable = findCurrentTable("<DELETE> a table");
        this.index = this.index+2; // Skip past "WHERE" safely as already parsed

        Attribute idColumnHeader = currentTable.getAttributeFromName("id");
        // Take a copy of column to avoid concurrent mod exception when modifying tableValues in the loop
        ArrayList<Value> idColumnToModify = new ArrayList<>(idColumnHeader.allValues);

        ArrayList<String> allConditions = storeConditions();
        ConditionProcessor conditionProcessor = new ConditionProcessor();

        for (Value id : idColumnToModify) { // Check if each valueRow meets the conditions
            if (conditionProcessor.checkRowMeetsConditions(allConditions, id, currentTable)){
                currentTable.deleteRow(id.getDataAsString());
            }
        }
    }

    public void executeJoin() throws IOException {
        //"JOIN " [TableName] " AND " [TableName] " ON " [AttributeName] " AND " [AttributeName]
        // performs an inner join on two tables (returning all permutations of all matching records)

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
        ArrayList<Attribute> table1SelectedAttributes = selectAttributesToJoin(table1, table1Attribute);
        ArrayList<Attribute> table2SelectedAttributes = selectAttributesToJoin(table2, table2Attribute);

        if (table1SelectedAttributes.isEmpty() && table2SelectedAttributes.isEmpty()){
            throw new IOException("Cannot execute <JOIN> if selected tables only have one attribute");
        }

        ResponseTableGenerator responseGenerator = new ResponseTableGenerator();
        this.responseRequired = true;
        this.responseTable = responseGenerator.createJoinedResponseTable(table1SelectedAttributes, table2SelectedAttributes,
                t1Attribute, t2Attribute);
    }

    public ArrayList<Attribute> selectAttributesToJoin(Table table, String attributeToJoin){
        ArrayList<Attribute> selectedAttributes = new ArrayList<>();

        for (Attribute a : table.getAllAttributes()){
            if (!a.getDataAsString().equalsIgnoreCase("id") && !a.getDataAsString().equals(attributeToJoin)){
                selectedAttributes.add(a);
            }
        }
        return selectedAttributes;
    }

    public Table findCurrentTable(String errorMessage) throws IOException {
        Database currentDatabase = currentSession.getDatabaseInUse();
        if (currentDatabase == null) {
            throw new IOException("Cannot " +errorMessage + " as no database is currently selected");
        }

        String tableName = commands[this.index];
        if (!currentDatabase.tableExists(tableName)) {
            throw new IOException("Cannot " +errorMessage + " which does not exist");
        }
        return currentDatabase.getTableByName(tableName);
    }

}

