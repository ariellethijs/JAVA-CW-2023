package edu.uob;

import java.util.ArrayList;

public class DBSession {
    ArrayList<Database> allDatabases;
    Database currentDB;

    int indexID;

    public DBSession() {
        // needs to search the database folder and read them in for memory to be persistent?
        // Not 100% sure need to do some more research :)
    }

    public void setCurrentDB(Database db){
        this.currentDB = db;
    }

    int getNextIDIndex(){
        this.indexID++;
        return this.indexID;
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


    public void createDatabase(String dbName){
        Database newDB = new Database(dbName, this);
        allDatabases.add(newDB);
    }




}
