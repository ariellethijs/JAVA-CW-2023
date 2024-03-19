package edu.uob;

import java.io.IOException;
import java.util.ArrayList;

public class DBInterpreter {
    private String[] commands;
    private int index;
    private DBSession currentSession;

    public DBInterpreter(String[] commandTokens, DBSession current) {
        this.currentSession = current;
        this.commands = commandTokens;
        this.index = 0;
    }

    public void interpretCommand(int commandStartIndex) throws IOException {
        this.index = commandStartIndex;
        String uppercaseCommand = commands[this.index].toUpperCase();

        switch (uppercaseCommand) {
            case "USE" -> {
                executeUse();
            }
            case "CREATE" -> {
                executeCreate();
            }
            case "DROP" -> {
                executeDrop();
            }
            case "ALTER" -> {
                executeAlter();
            }
            case "INSERT" -> {
                executeInsert();
            }
//            case "SELECT" -> {
//                executeSelect();
//            }
//            case "UPDATE" -> {
//                executeUpdate();
//            }
//            case "DELETE" -> {
//                executeDelete();
//            }
//            case "JOIN" -> {
//                executeJoin();
//            }
            default -> {
                throw new IOException("Attempting to interpret unimplemented command");
            }
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
            Table currentTable = currentDatabase.createTable(commands[this.index], currentSession);
            currentDatabase.createTableFile(commands[this.index], currentTable);
            this.index++;
            if (commands[this.index].equals("(")){
                this.index++;
                while (!commands[this.index].equals(")")){
                    if (!commands[this.index].equals(",")){ // Skip over the commas in attribute list
                        currentTable.createAttribute(commands[this.index], DataType.UNDEFINED);
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

        String tableName = commands[this.index];
        // Find out which alteration type is desired
        int nextIndex = this.index+1;
        String alterationType = commands[nextIndex];

        // Find the current database in use
        Database currentDatabase = currentSession.getDatabaseInUse();
        if (currentDatabase == null) {
            throw new IOException("Cannot <ALTER> as no database is currently selected");
        }

        if (currentDatabase.tableExists(tableName)){
            Table currentTable = currentDatabase.getTableByName(tableName);
            if (alterationType.equalsIgnoreCase("ADD")){
                this.index = nextIndex+1; // Skip past alteration type and execute
                executeAlterAdd(currentTable);
            } else if (alterationType.equalsIgnoreCase("DROP")){
                this.index = nextIndex+1;
                executeAlterDrop(currentTable);
            } else {
                throw new IOException("Invalid alteration type attempted as part of an <ALTER> command");
            }
        } else {
            throw new IOException("Attempting to <ALTER> a table which does not exist in current database");
        }
    }

    public void executeAlterAdd(Table currentTable) throws IOException {
        String attributeName = commands[this.index];
        if (!currentTable.attributeExists(attributeName)){
            currentTable.createAttribute(attributeName, DataType.UNDEFINED);
        } else {
            throw new IOException("Cannot <ALTER> a table by adding an attribute which already exist");
        }
    }

    // NEED TO ADD THIS TO ALTER DROP : !!!!!!!!!!!!!!!!!!!!
    // attempting to remove the ID column from a table

    public void executeAlterDrop(Table currentTable) throws IOException {
        String attributeName = commands[this.index];
        if (currentTable.attributeExists(attributeName)){
            currentTable.deleteAttribute(attributeName);
        } else {
            throw new IOException("Cannot <ALTER> a table by dropping an attribute which does not exist");
        }
    }

    public void executeInsert() throws IOException {
        // "INSERT " "INTO " [TableName] " VALUES" "(" <ValueList> ")"
        this.index = this.index+2; // Safely skip "INSERT " "INTO " as parsed successfully

        // Find the current database in use
        Database currentDatabase = currentSession.getDatabaseInUse();
        if (currentDatabase == null) {
            throw new IOException("Cannot <INSERT> into a table as no database is currently selected");
        }

        if (currentDatabase.tableExists(commands[this.index])){
            Table currentTable = currentDatabase.getTableByName(commands[this.index]);
            this.index = this.index+3; // Navigate to first value in list by skipping " VALUES" "("

            ArrayList<String> valuesInValueList = new ArrayList<>();

            while (!commands[this.index].equals(")")){
                if (!commands[this.index].equals(",")){ // if not a comma, must be a value as already parsed
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

            if (!valuesInValueList.isEmpty()){
                currentTable.storeValueRow(valuesInValueList);
            } else {
                throw new IOException("Couldn't input values into table contents"); // For debugging replace later
            }
        }
    }

    public void executeSelect(){
        //  "SELECT " <WildAttribList> " FROM " [TableName] | "SELECT " <WildAttribList> " FROM " [TableName] " WHERE " <Condition>


    }

    // THROW ERROR WHEN : changing (updating) the ID of a record
    public void executeUpdate(){
        // "UPDATE " [TableName] " SET " <NameValueList> " WHERE " <Condition>

    }


    public void executeDelete(){
        // "DELETE " "FROM " [TableName] " WHERE " <Condition>

    }
    public void executeJoin(){
        //"JOIN " [TableName] " AND " [TableName] " ON " [AttributeName] " AND " [AttributeName]

    }

}
