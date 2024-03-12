package edu.uob;

import java.util.ArrayList;

public class DBParser {
    private ArrayList<String> commands;
    private int index;


    /*

       <Command>         ::=  <CommandType> ";"
       <CommandType>     ::=  <Use> | <Create> | <Drop> | <Alter> | <Insert> | <Select> | <Update> | <Delete> | <Join>
       <Use>             ::=  "USE " [DatabaseName]
       <Create>          ::=  <CreateDatabase> | <CreateTable>
       <CreateDatabase>  ::=  "CREATE " "DATABASE " [DatabaseName]
       <CreateTable>     ::=  "CREATE " "TABLE " [TableName] | "CREATE " "TABLE " [TableName] "(" <AttributeList> ")"
       <Drop>            ::=  "DROP " "DATABASE " [DatabaseName] | "DROP " "TABLE " [TableName]
       <Alter>           ::=  "ALTER " "TABLE " [TableName] " " <AlterationType> " " [AttributeName]
       <Insert>          ::=  "INSERT " "INTO " [TableName] " VALUES" "(" <ValueList> ")"
       <Select>          ::=  "SELECT " <WildAttribList> " FROM " [TableName] | "SELECT " <WildAttribList> " FROM " [TableName] " WHERE " <Condition>
       <Update>          ::=  "UPDATE " [TableName] " SET " <NameValueList> " WHERE " <Condition>
       <Delete>          ::=  "DELETE " "FROM " [TableName] " WHERE " <Condition>
       <Join>            ::=  "JOIN " [TableName] " AND " [TableName] " ON " [AttributeName] " AND " [AttributeName]
     */
    public DBParser(ArrayList<String> commandTokens){
        this.commands = commandTokens;
        this.index = 0;
    }



}
