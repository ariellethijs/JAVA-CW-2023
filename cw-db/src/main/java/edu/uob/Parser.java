package edu.uob;
import java.util.ArrayList;
import java.io.IOException;

public class Parser {
    private final String[] commands;
    private int index;
    public ArrayList<Integer> validCommandStartingIndexes;
    private final String[] sqlKeywords;

    public Parser(String[] commandTokens){
        this.commands = commandTokens;
        this.index = 0;
        this.validCommandStartingIndexes = new ArrayList<>();
        this.sqlKeywords = new String[]{
                "USE", "CREATE", "DATABASE",
                "TABLE", "DROP", "ALTER",
                "INSERT", "INTO", "VALUES",
                "SELECT", "FROM", "WHERE",
                "UPDATE", "SET", "DELETE",
                "JOIN", "AND", "ON", "OR",
                "ADD", "LIKE", "TRUE",
                "FALSE", "NULL"
        };
    }

    public void parseAllTokens() throws IOException {
        while (this.index < commands.length && !commands[this.index].equals(";")){
            int commandStartIndex = this.index;
            if (parseCommand()) {
                validCommandStartingIndexes.add(commandStartIndex); // Add the start indexes of all valid commands for interpreter
            } else {
                throw new IOException("Invalid command type");
            }
        }

        if (commands.length > 0 && !commands[commands.length - 1].equals(";")){
            throw new IOException("Missing semicolon at the end of the command");
        }
    }

    public boolean parseCommand() throws IOException {
        String uppercaseCommand = commands[this.index].toUpperCase(); // To deal with case insensitivity

        switch (uppercaseCommand) {
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
            default -> throw new IOException("Invalid command type");
        }
    }

    public boolean parseUse() throws IOException {
        // "USE " [DatabaseName]
        this.index++;
        if (parsePlainText()) { // [DatabaseName] == plainText
            this.index++;
            return true;
        } else {
            throw new IOException("Invalid <USE> syntax");
        }
    }

    public boolean parseCreate() throws IOException {
        this.index++;
        // <CreateDatabase> | <CreateTable>
        if (databaseAndDatabaseName() || parseCreateTable()){
            this.index++;
            return true;
        } else {
            throw new IOException("Invalid <CREATE> syntax");
        }
    }

    // Checks for "DATABASE " [DatabaseName]
    public boolean databaseAndDatabaseName() throws IOException {
        //  "DATABASE " [DatabaseName]
        if (commands[this.index].equalsIgnoreCase("DATABASE")){
            this.index++;
            return parsePlainText(); // [DatabaseName] == plainText
        }
        return false;
    }

    // Checks for "TABLE " [TableName]
    public boolean tableAndTableName() throws IOException {
        if (commands[this.index].equalsIgnoreCase("TABLE")) {
            this.index++;
            return parsePlainText();
        }
        return false;
    }


    public boolean parseCreateTable() throws IOException {
        // "TABLE " [TableName] | "TABLE " [TableName] "(" <AttributeList> ")"
        if (tableAndTableName()){
            int nextIndex = this.index + 1;
            if (commands[nextIndex].equals("(")){ // Check ahead to see if followed by attribute list
                this.index = nextIndex + 1; // Skip past opening bracket
                boolean result = parseAttributeList();
                this.index++;
                if (result && commands[this.index].equals(")")) {
                    return true;
                } else {
                    throw new IOException("Opening bracket in <CreateTable>  is not followed by an attribute list and/or closing bracket");
                }
            } else {
                return true; // Return true for "TABLE" [TableName] w/o attribute list
            }
        }
        return false;
    }


    public boolean parseDrop() throws IOException {
        this.index++;
        //  "DATABASE " [DatabaseName] | "DROP " "TABLE " [TableName]
        if (databaseAndDatabaseName() || tableAndTableName()){
            this.index++;
            return true;
        } else {
            throw new IOException("Invalid <DROP> syntax");
        }
    }

    public boolean parseAlter() throws IOException {
        this.index++;
        //  "TABLE " [TableName] " " <AlterationType> " " [AttributeName]
        boolean validSyntax = false;
        if (tableAndTableName()){
            this.index++;
            if (parseAlterationType()){
                this.index++;
                validSyntax = parsePlainText(); // [AttributeName]   ::=  [PlainText]
            }
        }

        if (!validSyntax){ // Necessary to catch failure at any stage of the nested IF conditions
            throw new IOException("Invalid <ALTER> syntax");
        } else {
            this.index++;
            return true;
        }
    }

    public boolean parseInsert() throws IOException {
        this.index++;
        // "INTO " [TableName] " VALUES" "(" <ValueList> ")"
        boolean validSyntax = false;
        if (commands[this.index].equalsIgnoreCase("INTO")){
            this.index++;
            if (parsePlainText()){
                this.index++;
                if (commands[this.index].equalsIgnoreCase("VALUES")){
                    this.index++;
                    if (commands[this.index].equals("(")){
                        this.index++;
                        validSyntax = parseValueList();
                    }
                }
            }
        }

        if (!validSyntax){ // Necessary to catch failure at any stage of the nested IF conditions
            throw new IOException("Invalid <INSERT> syntax");
        } else {
            this.index++;
            return true;
        }
    }

    public boolean parseSelect() throws IOException {
        this.index++;
        //  <WildAttribList> " FROM " [TableName] |  <WildAttribList> " FROM " [TableName] " WHERE " <Condition>
        if (parseFullSelect()){
            this.index++;
            if (commands[this.index].equalsIgnoreCase("WHERE")){
                return whereCondition();
            } else {
                this.index++;
                return true;
            }
        } else {
            throw new IOException("Invalid <SELECT> syntax");
        }
    }

    public boolean parseFullSelect() throws IOException {
        // <WildAttribList> " FROM " [TableName]
        if (parseWildAttributeList()){
            this.index++;
            if (commands[this.index].equalsIgnoreCase("FROM")){
                this.index++;
                return parsePlainText();
            }
        }
        return false;
    }

    public boolean whereCondition() throws IOException{
        // " WHERE " <Condition>
        if (commands[this.index].equalsIgnoreCase("WHERE")){
            this.index++;
            return parseCondition();
        }
        return false;
    }

    public boolean parseUpdate() throws IOException {
        this.index++;
        // [TableName] " SET " <NameValueList> " WHERE " <Condition>
        boolean validSyntax = false;
        if (parsePlainText()){
            this.index++;
            if (commands[this.index].equalsIgnoreCase("SET")){
                this.index++;
                if (parseNameValueList()){
                    validSyntax = whereCondition();
                }
            }
        }

        if (!validSyntax){ // Necessary to catch failure at any stage of the nested IF conditions
            throw new IOException("Invalid <UPDATE> syntax");
        } else {
            return true;
        }
    }

    public boolean parseDelete() throws IOException {
        this.index++;
        // "FROM " [TableName] " WHERE " <Condition>
        boolean validSyntax = false;
        if (commands[this.index].equalsIgnoreCase("FROM")){
            this.index++;
            if (parsePlainText()){
                this.index++;
                validSyntax = whereCondition();
            }
        }

        if (!validSyntax){ // Necessary to catch failure at any stage of the nested IF conditions
            throw new IOException("Invalid <DELETE> syntax");
        } else {
            return true;
        }
    }

    public boolean parseJoin() throws IOException {
        this.index++;
        // [TableName] " AND " [TableName] " ON " [AttributeName] " AND " [AttributeName]
        boolean validSyntax = false;
        if (nameAndName()){
            this.index++;
            if (commands[this.index].equalsIgnoreCase("ON")) {
                this.index++;
                validSyntax = nameAndName();
                this.index++;
            }
        }

        if (!validSyntax){ // Necessary to catch failure at any stage of the nested IF conditions
            throw new IOException("Invalid <JOIN> syntax");
        } else {
            return true;
        }
    }

    public boolean nameAndName() throws IOException {
        // [TableName] " AND " [TableName] " || " [AttributeName] " AND " [AttributeName]
        if (parsePlainText()) {
            this.index++;
            if (commands[this.index].equalsIgnoreCase("AND")) {
                this.index++;
                return parsePlainText();
            }
        }
        return false;
    }

    public boolean parseDigit(char c){ return (c >= '0' && c <= '9'); }

    public boolean parseUpper(char c){
        return (c >= 'A' && c <= 'Z');
    }

    public boolean parseLower(char c){
        return (c >= 'a' && c <= 'z');
    }

    public boolean parseLetter(char c) {
        return parseUpper(c) || parseLower(c);
    }

    public boolean parsePlainText() throws IOException {
        // [TableName] && [AttributeName] && [DatabaseName] ::==
        // [Letter] | [Digit] | [PlainText] [Letter] | [PlainText] [Digit]

        if (isKeyword()){
            throw new IOException("Cannot use KEYWORD " +commands[this.index] + " as a [TableName] || [AttributeName] || [DatabaseName]");
        } else {
            for (char c : commands[this.index].toCharArray()){
                if (!(parseDigit(c) || parseLetter(c))){
                    return false;
                }
            }
        }
        return true;
    }

    public boolean isKeyword() {
        for (String keyword : this.sqlKeywords){
            if (commands[this.index].equalsIgnoreCase(keyword)){
                return true;
            }
        }
        return false;
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

    public boolean parseNameValueList() throws IOException {
        // <NameValuePair> | <NameValuePair> "," <NameValueList>
        if (parseNameValuePair()){
            this.index++;
            if (commands[this.index].equals(",")){
                this.index++;
                return parseNameValueList();
            } else {
                return true;
            }
        }
        return false;
    }

    public boolean parseNameValuePair() throws IOException {
        // [AttributeName] "=" [Value]
        if (parsePlainText()){
            this.index++;
            if (commands[this.index].equals("=")){
                this.index++;
                return parseValue();
            }
        }
        return false;
    }

    public boolean parseAlterationType(){
        //  "ADD" | "DROP"
        return (commands[this.index].equalsIgnoreCase("ADD") || commands[this.index].equalsIgnoreCase("DROP"));
    }

    private boolean containsAtLeastOneValue = false;

    public boolean parseValueList() {
        // [Value] | [Value] "," <ValueList>
        if (containsAtLeastOneValue && commands[this.index].equals(")")){
            return true;
        }

        if (parseValue()){
            containsAtLeastOneValue = true;
            int nextIndex = this.index+1;
            // If at least one value and closing brace
            if (commands[nextIndex].equals(",")){ // If more values to parse
                this.index = nextIndex+1;
            } else {
                this.index++;
            }
            return parseValueList();
        }
        return false;
    }

    public boolean parseDigitSequence(){
        // [Digit] | [Digit] [DigitSequence]
        int decimalCount = 0;

        for (char c : commands[this.index].toCharArray()){
            if (c == '.') {
                decimalCount++;
                if (decimalCount > 1) { // Cannot be more than one decimal place in float
                    return false;
                }
            }

            if (!(parseDigit(c)) && c != '.' && !plusOrMinus()){
                return false;
            }
        }
        return true;
    }

    public boolean parseIntegerLiteral(){
        // [DigitSequence] | "-" [DigitSequence] | "+" [DigitSequence]
        return parseDigitSequence();
    }

    public boolean parseFloatLiteral(){
        // [DigitSequence] "." [DigitSequence] | "-" [DigitSequence] "." [DigitSequence] | "+" [DigitSequence] "." [DigitSequence]
        return parseDigitSequence();
    }

    public boolean plusOrMinus(){
        return (commands[this.index].equals("-") || commands[this.index].equals("+"));
    }

    public boolean parseBooleanLiteral(){
        return (commands[this.index].equalsIgnoreCase("TRUE") || commands[this.index].equalsIgnoreCase("FALSE"));
    }

    public boolean parseCharLiteral(char c){
        return (parseSpace(c) || parseLetter(c) || parseSymbol(c) || parseDigit(c));
    }

    public boolean parseStringLiteral(){
        // "" | [CharLiteral] | [StringLiteral] [CharLiteral]
        if (commands[this.index].isEmpty()){
            this.index++;
            return true;
        } else if (commands[this.index].length() >= 2 && commands[this.index].charAt(0) == '\''
                && commands[this.index].charAt(commands[this.index].length() - 1) == '\''){
            // If at least 2 chars long and starts and end w/'
            for (char c : commands[this.index].substring(1, commands[this.index].length() - 1).toCharArray()){
                // Create and parse substring of the enclosed string
                if (!parseCharLiteral(c)) {
                return false;
                }
           }
           return true;
        } else {
            return false;
        }
    }

    public boolean parseValue() {
        // "'" [StringLiteral] "'" | [BooleanLiteral] | [FloatLiteral] | [IntegerLiteral] | "NULL"
        return (parseStringLiteral() || parseBooleanLiteral() || parseFloatLiteral() || parseIntegerLiteral() ||
                commands[this.index].equalsIgnoreCase("NULL"));
    }

    public boolean parseWildAttributeList() throws IOException {
        // <AttributeList> | "*"
        return (parseAttributeList() || commands[this.index].equals("*"));
    }

    public boolean parseAttributeList() throws IOException{
        // [AttributeName] | [AttributeName] "," <AttributeList>
        if (parsePlainText()){
            int nextIndex = this.index+1; // Look ahead to see if list continues
            if (commands[nextIndex].equals(",")){
                this.index = nextIndex+1; // Skip past the ","
                return parseAttributeList(); // Check other attributes are valid
            } else {
                return true;
            }
        }
        return false;
    }

    public boolean parseCondition() throws IOException {
        // "(" <Condition> <BoolOperator> <Condition> ")" | <Condition> <BoolOperator> <Condition> |
        // "(" [AttributeName] <Comparator> [Value] ")" |  [AttributeName] <Comparator> [Value]

        if (commands[this.index].equals("(")) {
            this.index++;
            return parseBracketedCondition(1, 0);
        } else {
            if (unbracketedCondition()){
                int nextIndex = this.index + 1;
                if (parseBoolOperator(commands[nextIndex])){ // If there's further conditions
                    this.index = nextIndex+1;
                    return parseCondition();
                } else {
                    this.index = nextIndex;
                    return true;
                }
            }
        }
        return false;
    }

    public boolean parseBracketedCondition(int openingBracketCount, int closingBracketCount) throws IOException {

        while (this.index < commands.length) { // Stops infinite loop if brackets not matched
            if (commands[this.index].equals("(")) {
                this.index++;
                openingBracketCount++;
            } else if (commands[this.index].equals(")")) {
                closingBracketCount++;
                int nextIndex = this.index + 1;
                if ((openingBracketCount - closingBracketCount) == 0 && !parseBoolOperator(commands[nextIndex])) { // If no more conditions and all brackets matched
                    this.index = nextIndex +1;
                    return true;
                }
            } else if (unbracketedCondition()) {
                this.index++;
                if ((openingBracketCount - closingBracketCount == 0) && commands[this.index].equals(";")){ return true; }
                if (commands[this.index].equals(")")) {
                    closingBracketCount++;
                    this.index++; // Skip past closing bracket
                    if ((openingBracketCount - closingBracketCount == 0) && commands[this.index].equals(";")){ return true; }
                }
                if (parseBoolOperator(commands[this.index])) {
                    this.index++;
                    return parseBracketedCondition(openingBracketCount, closingBracketCount);
                }
            }
        }
        return false;
    }

    public boolean unbracketedCondition() throws IOException {
        // [AttributeName] <Comparator> [Value]
        if (parsePlainText()){ // Attribute names just require plain text
            this.index++;
            if (parseComparator()){ // Check followed by comparator
                this.index++;
                // Check followed by value
                return parseValue();
            }
        }
        return false;
    }

    public boolean parseBoolOperator(String token){
        // "AND" || "OR"
        return (token.equalsIgnoreCase("AND") || token.equalsIgnoreCase("OR"));
    }

    public boolean parseComparator(){
        // Check if comparator is from the valid set
        String[] comparators = {"==", ">", "<", ">=", "<=", "!=", "LIKE"};

        for (String x : comparators) {
            if (commands[this.index].equalsIgnoreCase(x)){
                return true;
            }
        }
        return false;
    }

}