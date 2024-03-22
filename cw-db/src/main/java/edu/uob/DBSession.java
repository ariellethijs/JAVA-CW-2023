package edu.uob;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;

public class DBSession {
    ArrayList<Database> allDatabases;
    ArrayList<File> allDatabaseDirectories;
    Database databaseInUse;
    String storageFolderPath;

    public DBSession(String folderPath) throws IOException {
        this.storageFolderPath = folderPath;
        databaseInUse = new Database("initializer");
        this.allDatabases = new ArrayList<>();
        this.allDatabaseDirectories = new ArrayList<>();
        storeDatabasesInDataFolder();
    }

    private void storeDatabasesInDataFolder() throws IOException {
        File directory = new File(storageFolderPath);

        // Get subdirectories (databases) in the main directory
        File[] databaseDirectories = directory.listFiles(File::isDirectory);
        if (databaseDirectories != null) {
            for (File databaseDirectory : databaseDirectories) {
                String databaseName = databaseDirectory.getName();
                if (!dbExists(databaseName)) { // Skips over databases which are already stored
                    Database currentDatabase = createDatabase(databaseName); // Create a new database
                    storeFilesInDatabaseDirectory(databaseDirectory, currentDatabase); // Store files as tables
                }
            }
        }
    }

    private void storeFilesInDatabaseDirectory(File databaseDirectory, Database currentDatabase) throws IOException {
        File[]databaseFiles = databaseDirectory.listFiles();
        Parser parser = new Parser();

        if (databaseFiles != null){
            for (File databaseFile : databaseFiles){
                if (databaseFile.isFile() && databaseFile.getName().endsWith(".tab")){ // Check the file format is correct
                    String tableName = getNameWithoutExtension(databaseFile, parser); // Store the table name from fileName
                    if (!tableName.isEmpty() && !currentDatabase.tableExists(tableName)){
                        // Avoids reading in multiple files with the same name || invalid names
                        Table currentTable = currentDatabase.createTable(tableName, true);
                        storeFile(databaseFile, currentTable); // Store the data in newly created table
                    }
                }
            }
        }
    }

    private String getNameWithoutExtension(File file, Parser parser) throws IOException {
        String fileName = file.getName();
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0) {
            fileName = fileName.substring(0, dotIndex);
            if (!parser.parsePlainText(fileName)){ // Parse the tableName to ensure its valid
                return "";
            } else {
                return fileName;
            }
        } else {
            throw new IOException("Attempting to read a file of invalid format");
        }
    }

    private void storeFile(File currentFile, Table currentTable) throws IOException {
            BufferedReader reader = new BufferedReader(new FileReader(currentFile));
            String currentLine;
            boolean isHeaderLine = true;
            while ((currentLine = reader.readLine()) != null) {
                String[] values = currentLine.split("\t");
                if (isHeaderLine) {
                    storeAttributesFromFile(values, currentTable);
                    isHeaderLine = false;
                } else {
                    storeValuesFromFile(values, currentTable);
                }
            }
    }

    private void storeAttributesFromFile(String[] attributes, Table currentTable) {
        for (String attributeName : attributes){
            currentTable.createAttribute(attributeName);
        }
    }

    private void storeValuesFromFile(String[] values, Table currentTable) throws IOException {
        int rowID = -1;
        int columnIndex = 0;
        for (String value : values){
            if (columnIndex == 0){
                rowID = Integer.parseInt(value); // Find the indexID of the file row you're on
                if (rowID < 0) {
                    throw new IOException("id column is stored incorrectly in Table " + currentTable.getTableName());
                }
                currentTable.createValueFromFile(columnIndex, value, rowID);
            } else {
                currentTable.createValueFromFile(columnIndex, value, rowID); // Store all values in file into table
            }
            columnIndex++;
        }
    }

    public void setDatabaseInUse(Database db){
        this.databaseInUse = db;
    }

    public Database getDatabaseInUse(){
        return this.databaseInUse;
    }

    public boolean dbExists(String dbName){
        for (Database db : allDatabases){
            if (db.getDBName().equalsIgnoreCase(dbName)){
                return true;
            }
        }
        return false;
    }

    public Database getDatabaseByName(String dbName) throws IOException {
        for (Database db : allDatabases){
            if (db.getDBName().equalsIgnoreCase(dbName)){
                return db;
            }
        }
        throw new IOException("No such database exists");
    }

    public Database createDatabase(String dbName) {
        Database newDB = new Database(dbName);
        allDatabases.add(newDB);
        return newDB;
    }

    public void createDatabaseDirectory(String databaseName) throws IOException {
        String directoryNameAndPath = storageFolderPath + File.separator + databaseName;
        File newDirectory = new File(directoryNameAndPath);
        Files.createDirectory(newDirectory.toPath());
        Database currentDatabase = getDatabaseByName(databaseName);
        currentDatabase.setDatabaseDirectory(newDirectory);
        allDatabaseDirectories.add(newDirectory);
    }

    public File getDatabaseDirectoryFromName(String databaseName){
        for (File dbDirectory : allDatabaseDirectories){
            if (dbDirectory.getName().equalsIgnoreCase(databaseName)){
                return dbDirectory;
            }
        }
        return null;
    }

    public void deleteDatabase(String databaseName) throws IOException {
        Database database = getDatabaseByName(databaseName);
        File databaseDirectory = getDatabaseDirectoryFromName(databaseName);

        // Create a copy of the allTables list to avoid concurrent mod except
        ArrayList<Table> tablesToRemove = new ArrayList<>(database.allTables);

        // Delete all tables & tableFiles
        for (Table table : tablesToRemove) {
            String tableName = table.getTableName();
            database.deleteTable(tableName);
        }

        // Remove database directory & database
        if (databaseDirectory.exists()) {
            if (!deleteDirectory(databaseDirectory)) {
                throw new IOException("Failed to delete database directory: " + databaseDirectory.getAbsolutePath());
            }
        } else {
            throw new IOException("Database directory not found: " + databaseDirectory.getAbsolutePath());
        }
        // Remove directory and database from active storage
        allDatabaseDirectories.remove(databaseDirectory);
        allDatabases.remove(database);
    }

    public boolean deleteDirectory(File databaseDirectory) {
        if (!databaseDirectory.exists()) {
            return false;
        }

        File[] tableFiles = databaseDirectory.listFiles();
        if (tableFiles != null) {
            for (File tableFile : tableFiles) {
                if (!tableFile.delete()) {
                    return false;
                }
            }
        }
        return databaseDirectory.delete();
    }

}
