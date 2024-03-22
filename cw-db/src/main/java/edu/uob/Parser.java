package edu.uob;
import java.util.ArrayList;
import java.io.IOException;

public class Parser {
    private String[] commands;
    private int index;
    public ArrayList<Integer> validCommandStartingIndexes;
    private final String[] sqlKeywords;

    public Parser(){
        this.index = 0;
        this.validCommandStartingIndexes = new ArrayList<>();
        this.sqlKeywords = new String[]{ // Store all keywords to ensure no use as names
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

    public void parseAllTokens(String[] commandTokens) throws IOException {
        this.commands = commandTokens;
        while (this.index < commands.length && !commands[this.index].equals(";")){ // For all tokens in commands
            int commandStartIndex = this.index;
            if (parseCommand()){
                validCommandStartingIndexes.add(commandStartIndex); // Add the start indexes of all valid commands for interpreter
            } else {
                throw new IOException("Invalid command type");
            }
        }

        if (commands.length > 0 && !commands[commands.length - 1].equals(";")){ // Ensure last token is ;
            throw new IOException("Missing semicolon at the end of the command");
        }
    }

    private boolean parseCommand() throws IOException {
        String uppercaseCommand = commands[this.index].toUpperCase(); // To account for case insensitivity

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

    /*  Main Command Parsing   */

    private boolean parseUse() throws IOException {
        // "USE " [DatabaseName]
        this.index++;
        if (parsePlainText(commands[this.index])) { // [DatabaseName] == plainText
            this.index++;
            return true;
        } else {
            throw new IOException("Invalid <USE> syntax");
        }
    }

    private boolean parseCreate() throws IOException {
        this.index++;
        // <CreateDatabase> | <CreateTable>
        if (databaseAndDatabaseName() || parseCreateTable()){
            this.index++;
            return true;
        } else {
            throw new IOException("Invalid <CREATE> syntax");
        }
    }

    private boolean parseCreateTable() throws IOException {
        // "TABLE " [TableName] | "TABLE " [TableName] "(" <AttributeList> ")"
        if (tableAndTableName()){
            int nextIndex = this.index + 1;
            if (commands[nextIndex].equals("(")){ // Check ahead to see if followed by attribute list
                this.index = nextIndex + 1; // Skip past opening bracket
                boolean result = parseAttributeList();
                this.index++;
                if (result && commands[this.index].equals(")")){ // Contains an attribute list and has a matching bracket
                    return true;
                } else {
                    throw new IOException("Opening bracket in <CreateTable> is not followed by an attribute list and/or closing bracket");
                }
            } else {
                return true; // Return true for "TABLE" [TableName] without attribute list
            }
        }
        return false;
    }

    private boolean parseDrop() throws IOException {
        this.index++;
        //  "DATABASE " [DatabaseName] | "DROP " "TABLE " [TableName]
        if (databaseAndDatabaseName() || tableAndTableName()){
            this.index++;
            return true;
        } else {
            throw new IOException("Invalid <DROP> syntax");
        }
    }

    private boolean parseAlter() throws IOException {
        //  "TABLE " [TableName] " " <AlterationType> " " [AttributeName]
        this.index++;
        boolean validSyntax = false;
        if (tableAndTableName()){
            this.index++;
            if (parseAlterationType()){
                this.index++;
                validSyntax = parsePlainText(commands[this.index]); // [AttributeName]   ::=  [PlainText]
            }
        }

        if (!validSyntax){ // Necessary to catch failure at any stage of the nested IF conditions
            throw new IOException("Invalid <ALTER> syntax");
        } else {
            this.index++;
            return true;
        }
    }

    private boolean parseInsert() throws IOException {
        this.index++;
        // "INTO " [TableName] " VALUES" "(" <ValueList> ")"
        boolean validSyntax = false;
        if (commands[this.index].equalsIgnoreCase("INTO")){
            this.index++;
            if (parsePlainText(commands[this.index])){
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

        if (!validSyntax){ // Necessary to catch failure at any stage of nested if and return desired exception
            throw new IOException("Invalid <INSERT> syntax");
        } else {
            this.index++;
            return true;
        }
    }

    private boolean parseSelect() throws IOException {
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

    private boolean parseFullSelect() throws IOException {
        // <WildAttribList> " FROM " [TableName]
        if (parseWildAttributeList()){
            this.index++;
            if (commands[this.index].equalsIgnoreCase("FROM")){
                this.index++;
                return parsePlainText(commands[this.index]);
            }
        }
        return false;
    }

    private boolean parseUpdate() throws IOException {
        this.index++;
        // [TableName] " SET " <NameValueList> " WHERE " <Condition>
        boolean validSyntax = false;
        if (parsePlainText(commands[this.index])){
            this.index++;
            if (commands[this.index].equalsIgnoreCase("SET")){
                this.index++;
                if (parseNameValueList()){
                    validSyntax = whereCondition();
                }
            }
        }

        if (!validSyntax){ // Necessary to catch command specific exception
            throw new IOException("Invalid <UPDATE> syntax");
        } else {
            return true;
        }
    }

    private boolean parseDelete() throws IOException {
        this.index++;
        // "FROM " [TableName] " WHERE " <Condition>
        boolean validSyntax = false;
        if (commands[this.index].equalsIgnoreCase("FROM")){
            this.index++;
            if (parsePlainText(commands[this.index])){
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

    private boolean parseJoin() throws IOException {
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

    /* Literal Parsing */

    private boolean parseDigit(char c){ return (c >= '0' && c <= '9'); }

    private boolean parseUpper(char c){
        return (c >= 'A' && c <= 'Z');
    }

    private boolean parseLower(char c){
        return (c >= 'a' && c <= 'z');
    }

    private boolean parseLetter(char c) {
        return parseUpper(c) || parseLower(c);
    }

    public boolean parsePlainText(String token) throws IOException {
        // [TableName] && [AttributeName] && [DatabaseName] ::==
        // [Letter] | [Digit] | [PlainText] [Letter] | [PlainText] [Digit]

        if (isKeyword(token)){
            throw new IOException("Cannot use KEYWORD " +token + " as a [TableName] || [AttributeName] || [DatabaseName]");
        } else {
            for (char c : token.toCharArray()){
                if (!(parseDigit(c) || parseLetter(c))){
                    return false;
                }
            }
        }
        return true;
    }

    private boolean isKeyword(String token) {
        for (String keyword : this.sqlKeywords){
            if (token.equalsIgnoreCase(keyword)){
                return true;
            }
        }
        return false;
    }

    private boolean parseSymbol(char c){
        String symbols = "!#$%&()*+,-./:;>=<?@[]^_`{}~";
        for (char x : symbols.toCharArray()){
            if (c == x){
                return true;
            }
        }
        return false;
    }

    private boolean parseSpace(char c){
        return (c == ' ');
    }

    private boolean parseAlterationType(){
        //  "ADD" | "DROP"
        return (commands[this.index].equalsIgnoreCase("ADD") || commands[this.index].equalsIgnoreCase("DROP"));
    }

    private boolean parseDigitSequence(){
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

    private boolean parseIntegerLiteral(){
        // [DigitSequence] | "-" [DigitSequence] | "+" [DigitSequence]
        return parseDigitSequence();
    }

    private boolean parseFloatLiteral(){
        // [DigitSequence] "." [DigitSequence] | "-" [DigitSequence] "." [DigitSequence] | "+" [DigitSequence] "." [DigitSequence]
        return parseDigitSequence();
    }

    private boolean plusOrMinus(){
        return (commands[this.index].equals("-") || commands[this.index].equals("+"));
    }

    private boolean parseBooleanLiteral(){
        return (commands[this.index].equalsIgnoreCase("TRUE") || commands[this.index].equalsIgnoreCase("FALSE"));
    }

    private boolean parseCharLiteral(char c){
        return (parseSpace(c) || parseLetter(c) || parseSymbol(c) || parseDigit(c));
    }

    private boolean parseStringLiteral(){
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


    /*  List Parsing  */

    private boolean parseWildAttributeList() throws IOException {
        // <AttributeList> | "*"
        return (parseAttributeList() || commands[this.index].equals("*"));
    }

    private boolean parseAttributeList() throws IOException{
        // [AttributeName] | [AttributeName] "," <AttributeList>
        if (parsePlainText(commands[this.index])){
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

    private boolean parseNameValueList() throws IOException {
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

    private boolean parseNameValuePair() throws IOException {
        // [AttributeName] "=" [Value]
        if (parsePlainText(commands[this.index])){
            this.index++;
            if (commands[this.index].equals("=")){
                this.index++;
                return parseValue();
            }
        }
        return false;
    }

    private boolean containsAtLeastOneValue = false;

    private boolean parseValueList() {
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

    private boolean parseValue() {
        // "'" [StringLiteral] "'" | [BooleanLiteral] | [FloatLiteral] | [IntegerLiteral] | "NULL"
        return (parseStringLiteral() || parseBooleanLiteral() || parseFloatLiteral() || parseIntegerLiteral() ||
                commands[this.index].equalsIgnoreCase("NULL"));
    }


    /* Condition Parsing */

    private boolean whereCondition() throws IOException{
        // " WHERE " <Condition>
        if (commands[this.index].equalsIgnoreCase("WHERE")){
            this.index++;
            return parseCondition();
        }
        return false;
    }

    private boolean parseCondition() throws IOException {
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

    private boolean parseBracketedCondition(int openingBracketCount, int closingBracketCount) throws IOException {

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

    private boolean unbracketedCondition() throws IOException {
        // [AttributeName] <Comparator> [Value]
        if (parsePlainText(commands[this.index])){ // Attribute names just require plain text
            this.index++;
            if (parseComparator()){ // Check followed by comparator
                this.index++;
                // Check followed by value
                return parseValue();
            }
        }
        return false;
    }

    private boolean parseBoolOperator(String token){
        // "AND" || "OR"
        return (token.equalsIgnoreCase("AND") || token.equalsIgnoreCase("OR"));
    }

    private boolean parseComparator(){
        // Check if comparator is from the valid set
        String[] comparators = {"==", ">", "<", ">=", "<=", "!=", "LIKE"};

        for (String x : comparators) {
            if (commands[this.index].equalsIgnoreCase(x)){
                return true;
            }
        }
        return false;
    }

    /* Helper Functions for repetitive checks */

    private boolean nameAndName() throws IOException {
        // Helper function for <JOIN>
        // [TableName] " AND " [TableName] " || " [AttributeName] " AND " [AttributeName]
        if (parsePlainText(commands[this.index])) {
            this.index++;
            if (commands[this.index].equalsIgnoreCase("AND")) {
                this.index++;
                return parsePlainText(commands[this.index]);
            }
        }
        return false;
    }

    private boolean databaseAndDatabaseName() throws IOException {
        //  Checks for "DATABASE " [DatabaseName]
        if (commands[this.index].equalsIgnoreCase("DATABASE")){
            this.index++;
            return parsePlainText(commands[this.index]); // [DatabaseName] == plainText
        }
        return false;
    }

    private boolean tableAndTableName() throws IOException {
        // Checks for "TABLE " [TableName]
        if (commands[this.index].equalsIgnoreCase("TABLE")) {
            this.index++;
            return parsePlainText(commands[this.index]); // [TableName] == plainText
        }
        return false;
    }

}
