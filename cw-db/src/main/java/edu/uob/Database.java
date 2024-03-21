package edu.uob;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;

public class Database {
    String name;
    ArrayList<Table> allTables;
    File databaseDirectory;
    ArrayList<File> allTableFiles;

    public Database(String DbName){
        this.name = DbName;
        this.allTables = new ArrayList<>();
        this.allTableFiles = new ArrayList<>();
    }

    public String getDBName(){
        return this.name;
    }

    public Table createTable(String tableName, boolean fromFile) {
        Table newTable = new Table(tableName, fromFile);
        allTables.add(newTable);
        return newTable;
    }

    public void createTableFile(String tableName, Table newTable) throws IOException {
        String tableNameAndPath = this.databaseDirectory.getAbsolutePath() + File.separator + tableName + ".tab";
        File newFile = new File(tableNameAndPath);
        if (!newFile.exists()) { // If the file doesn't exist yet, create it
            Files.createFile(newFile.toPath());
            allTableFiles.add(newFile);
            newTable.setTableFile(newFile);
        }
    }

    public Table getTableByName(String name){
        for (Table table : allTables){
            if (table.getTableName().equals(name)){
                return table;
            }
        }
        return null; // Should never get here, as should always test for existence first
    }

    public boolean tableExists(String tableName){
        for (Table table : allTables){
            if (table.getTableName().equals(tableName)){
                return true;
            }
        }
        return false;
    }

    public void setDatabaseDirectory(File directory){
        this.databaseDirectory = directory;
    }

    public File getFileByTableName(String tableName) throws IOException {
        for (File tableFile : allTableFiles){
            if (tableFile.getName().equals(tableName + ".tab")){
                return tableFile;
            }
        }
        throw new IOException("No such table exists");
    }

    public void deleteTable(String tableName) throws IOException {
        Table table = getTableByName(tableName);
        File tableFile = getFileByTableName(tableName);
        allTables.remove(table); // Remove from list
        allTableFiles.remove(tableFile);

        // Delete the table file from the file system
        if (tableFile.exists()) {
            if (!tableFile.delete()) {
                throw new IOException("Failed to delete table file: " + tableFile.getAbsolutePath());
            }
        } else {
            throw new IOException("Table file not found: " + tableFile.getAbsolutePath());
        }
    }

}
