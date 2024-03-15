package edu.uob;

import java.util.ArrayList;

public class Database {
    String name;
    ArrayList<Table> allTables;
    DBSession currentSession;

    public Database(String DbName, DBSession current){
        this.name = DbName;
        this.currentSession = current;
        this.allTables = new ArrayList<Table>();
    }

    public String getDBName(){
        return this.name;
    }

    public void addTable(Table newTable){
        allTables.add(newTable);
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



}
