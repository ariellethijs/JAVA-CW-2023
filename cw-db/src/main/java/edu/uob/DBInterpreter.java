package edu.uob;

import java.io.IOException;

public class DBInterpreter {
    private String[] commands;
    private int index;
    private DBSession currentSession;

    private Database currentDB;

    public DBInterpreter(String[] commandTokens, DBSession current) {
        this.currentSession = current;
        this.commands = commandTokens;
        this.index = 0;
    }

    public void interpretCommand(int commandStartIndex) throws IOException {
        this.index = commandStartIndex;

        switch (commands[this.index]) {
            case "USE" -> {
                executeUse();
            }
            case "CREATE" -> {
                executeCreate();
            }
            case "DROP" -> {
                executeDrop();
            }
//            case "ALTER" -> {
//                executeAlter();
//            }
//            case "INSERT" -> {
//                executeInsert();
//            }
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
            throw new IOException("No Database by that name exists");
        }

    }
    public void executeCreate() throws IOException {
        this.index++;

        if (commands[this.index].equals("DATABASE")){
            executeCreateDatabase();
        } else if (commands[this.index].equals("TABLE")){
            executeCreateTable();
        }
    }

    public void executeCreateDatabase() throws IOException {
        this.index++;
        if (!currentSession.dbExists(commands[this.index])){
            currentSession.createDatabase(commands[this.index]);
            currentSession.createDatabaseDirectory(commands[this.index]);
        } else {
            throw new IOException("A database with that name already exists");
        }
    }

    public void executeCreateTable() throws IOException {
        this.index++;

        Database currentDatabase = currentSession.getDatabaseInUse();
        if (currentDatabase == null){
            throw new IOException("No database is currently selected");

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
            throw new IOException("A table with that name already exists");
        }
    }

    public void executeDrop() throws IOException {
        // "DROP " "DATABASE " [DatabaseName] | "DROP " "TABLE " [TableName]
        this.index++;
        if (commands[this.index].equals("DATABASE")){
            executeDropDatabase();
        } else if (commands[this.index].equals("TABLE")){
            executeDropTable();
        }

    }

    public void executeDropDatabase() throws IOException {
        this.index++;
        if (currentSession.dbExists(commands[this.index])){
            currentSession.deleteDatabase(commands[this.index]);
        } else {
            throw new IOException("Cannot delete a database which does not exist");
        }
    }

    public void executeDropTable() throws IOException {
        this.index++;

        Database currentDatabase = currentSession.getDatabaseInUse();
        if (currentDatabase == null) {
            throw new IOException("No database is currently selected");

        } else if (currentDatabase.tableExists(commands[this.index])){
            currentDatabase.deleteTable(commands[this.index]);
        } else {
            throw new IOException("Cannot delete a table which does not exist");
        }

    }

    public void executeAlter(){

    }

    public void executeInsert(){

    }

    public void executeSelect(){

    }

    public void executeUpdate(){

    }


    public void executeDelete(){

    }
    public void executeJoin(){

    }

}
