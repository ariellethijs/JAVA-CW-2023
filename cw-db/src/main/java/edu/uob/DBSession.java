package edu.uob;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class DBSession {
    ArrayList<Database> allDatabases;
    Database currentDB;

    public DBSession(String storageFolderPath) throws IOException {
        // needs to search the database folder and read them in for memory to be persistent?
        // Not 100% sure need to do some more research :)
        File directory = new File(storageFolderPath);

        // Get subdirectories (databases) in the main directory
        File[] databaseDirectories = directory.listFiles(File::isDirectory);
        if (databaseDirectories != null) {
            for (File databaseDirectory : databaseDirectories) {
                String databaseName = getNameWithoutExtension(databaseDirectory);
                if (!dbExists(databaseName)) { // Skips over databases which are already stored
                    Database currentDatabase = createDatabase(databaseName); // Create a new database
                    storeFilesInDatabaseDirectory(databaseDirectory, currentDatabase); // Store files as tables
                }
            }
        }
    }

    private void storeFilesInDatabaseDirectory(File databaseDirectory, Database currentDatabase) throws IOException {
        File[]databaseFiles = databaseDirectory.listFiles();
        if (databaseFiles != null){
            for (File databaseFile : databaseFiles){
                if (databaseFile.isFile() && databaseFile.getName().endsWith(".tab")){
                    String tableName = getNameWithoutExtension(databaseFile);
                    if (!currentDatabase.tableExists(tableName)){ // Wouldn't really get here but just in case
                        Table currentTable = currentDatabase.createTable(tableName, this);
                        storeFile(databaseFile, currentTable);
                    }
                }
            }
        }
    }

    public String getNameWithoutExtension(File file) throws IOException {
        String fileName = file.getName();
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0) {
            return fileName.substring(0, dotIndex);
        } else {
            throw new IOException("Attempting to read a file of invalid format");
            // Should never reach this as checks for .tab files prior, but just in case
        }
    }

    public void storeFile(File currentFile, Table currentTable){
        try (BufferedReader reader = new BufferedReader(new FileReader(currentFile))){
            String currentLine;
            boolean isHeaderLine = true;
            while ((currentLine = reader.readLine()) != null) {
                String[] values = currentLine.split("\t");
                if (isHeaderLine) {
                    storeAttributes(values, currentTable);
                    isHeaderLine = false;
                } else {
                    storeValues(values, currentTable);
                }
            }
        } catch (IOException e){
            System.err.println(" Failed to read file: " + e.getMessage());
        }
    }

    public void storeAttributes(String[] attributes, Table currentTable){
        for (String attributeName : attributes){
            currentTable.createAttribute(attributeName, DataType.UNDEFINED);
        }
    }

    public void storeValues(String[] values, Table currentTable){
        int columnIndex = 0;
        for (String value : values){
            currentTable.createValueFromString(currentTable.getAttributeNameFromIndex(columnIndex), value);
            columnIndex++;
        }
    }

    public void setCurrentDB(Database db){
        this.currentDB = db;
    }

    public boolean dbExists(String dbName){
        for (Database db : allDatabases){
            if (db.getDBName().equals(dbName)){
                return true;
            }
        }
        return false;
    }

    public Database getDatabaseByName(String dbName){
        for (Database db : allDatabases){
            if (db.getDBName().equals(dbName)){
                return db;
            }
        }
        return null;
    }


    public Database createDatabase(String dbName){
        Database newDB = new Database(dbName, this);
        allDatabases.add(newDB);
        return newDB;
    }




}
