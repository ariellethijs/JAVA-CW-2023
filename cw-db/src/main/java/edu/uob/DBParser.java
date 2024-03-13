package edu.uob;

import java.util.ArrayList;

public class DBParser {
    private ArrayList<String> commands;
    private int index;

    public DBParser(ArrayList<String> commandTokens){
        this.commands = commandTokens;
        this.index = 0;
    }

    public void parseAllTokens() throws ParsingException {
        while (index <= commands.size()){
            if (!parseCommand()){
                throw new ParsingException("ERROR: Invalid command format");
            }
        }
    }

    public boolean parseCommand() throws ParsingException {
        switch (commands.get(index)) {
            case "USE" -> {
                return parseUse();
            }
            case "CREATE" -> {
                return parseCreate();
            }
            case "DROP" -> {
                return parseDrop();
            }
            case "ALTER" -> {
                return parseAlter();
            }
            case "INSERT" -> {
                return parseInsert();
            }
            case "SELECT" -> {
                return parseSelect();
            }
            case "UPDATE" -> {
                return parseUpdate();
            }
            case "DELETE" -> {
                return parseDelete();
            }
            case "JOIN" -> {
                return parseJoin();
            }
            default -> {
                throw new ParsingException("ERROR: Invalid command entered");
            }
        }
    }

    public boolean parseUse() throws ParsingException {
        // <Use>             ::=  "USE " [DatabaseName]
    }

    public boolean parseCreate() throws ParsingException {
        // <CreateDatabase> | <CreateTable>
        // <CreateDatabase>  ::=  "CREATE " "DATABASE " [DatabaseName]
        // <CreateTable>     ::=  "CREATE " "TABLE " [TableName] | "CREATE " "TABLE " [TableName] "(" <AttributeList> ")"
    }

    public boolean parseDrop() throws ParsingException {
        //  "DROP " "DATABASE " [DatabaseName] | "DROP " "TABLE " [TableName]


    }

    public boolean parseAlter() throws ParsingException {
        //  "ALTER " "TABLE " [TableName] " " <AlterationType> " " [AttributeName]

    }

    public boolean parseInsert() throws ParsingException {
        //  "INSERT " "INTO " [TableName] " VALUES" "(" <ValueList> ")"

    }

    public boolean parseSelect() throws ParsingException {
        //  "SELECT " <WildAttribList> " FROM " [TableName] | "SELECT " <WildAttribList> " FROM " [TableName] " WHERE " <Condition>

    }

    public boolean parseUpdate() throws ParsingException {
        // "UPDATE " [TableName] " SET " <NameValueList> " WHERE " <Condition>
    }


    public boolean parseDelete() throws ParsingException {
        // "DELETE " "FROM " [TableName] " WHERE " <Condition>

    }

    public boolean parseJoin() throws ParsingException {
        // "JOIN " [TableName] " AND " [TableName] " ON " [AttributeName] " AND " [AttributeName]
    }

    public boolean parseDigit(char c){

        return (c >= '0' && c <= '9');
    }

    public boolean parseLetter(char c) {
        return parseUpper(c) || parseLower(c);
    }


    public boolean parseUpper(char c){
        return (c >= 'A' && c <= 'Z');
    }

    public boolean parseLower(char c){
        return (c >= 'a' && c <= 'z');
    }

    public boolean parseSymbol(char c){
        String symbols = "!#$%&()*+,-./:;>=<?@[]^_`{}~";
        for (char x : symbols.toCharArray()){
            if (c == x){
                return true;
            }
        }
        return false;
    }

    public boolean parseSpace(char c){
        return (c == ' ');
    }

    public boolean parseBoolean(){
        return (commands.get(index).equals("TRUE") || commands.get(index).equals("FALSE"));
    }

    public boolean parseBoolOperator(){
        return (commands.get(index).equals("AND") || commands.get(index).equals("OR"));

    }

    public boolean parseAlterationType(){
        return (commands.get(index).equals("ADD") || commands.get(index).equals("DROP"));
    }

    public boolean parseComparator() {
        String[] comparators = {"==", ">", "<", ">=", "<=", "!=", "LIKE"};

        for (String x : comparators) {
            if (commands.get(index).equals(x)) {
                return true;
            }
        }
        return false;
    }



}
