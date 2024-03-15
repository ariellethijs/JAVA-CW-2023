package edu.uob;
import java.util.Arrays;
import java.util.ArrayList;

public class DBParser {
    private String[] commands;
    private int index;

    public DBParser(String[] commandTokens){
        this.commands = commandTokens;
        this.index = 0;
    }

    public boolean parseAllTokens() throws ParsingException {
            try {
                while (this.index < commands.length && !commands[this.index].equals(";")){
                    if (!parseCommand()){
                        throw new ParsingException("Invalid command type");
                    }
                }

                if (commands.length > 0 && !commands[commands.length - 1].equals(";")){
                    throw new ParsingException("Missing semicolon at the end of the command");
                }

                if (this.index == (commands.length - 1)){
                    System.out.println("OK: parsed successfully!");
                    return true;
                }
            } catch (ParsingException pe) {
                // Handle parsing exceptions
                System.err.println("ERROR: " + pe.getMessage());
            }
            return false;
    }

    public boolean parseCommand() throws ParsingException {
        System.out.println("Testing command: " +commands[this.index]);
        switch (commands[this.index]) {
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
                return false;
            }
        }
    }

    public boolean parseUse() throws ParsingException {
        // "USE " [DatabaseName]
        this.index++;
        if (parsePlainText()) { // [DatabaseName] == plainText
            this.index++;
            return true;
        } else {
            throw new ParsingException("Invalid <USE> syntax");
        }
    }

    public boolean parseCreate() throws ParsingException {
        this.index++;
        // <CreateDatabase> | <CreateTable>
        if (databaseAndDatabaseName() || parseCreateTable()){
            this.index++;
            return true;
        } else {
            throw new ParsingException("Invalid <CREATE> syntax");
        }
    }

    // Checks for "DATABASE " [DatabaseName]
    public boolean databaseAndDatabaseName() throws ParsingException {
        //  "DATABASE " [DatabaseName]
        if (commands[this.index].equals("DATABASE")){
            this.index++;
            return parsePlainText(); // [DatabaseName] == plainText
        }
        return false;
    }

    // Checks for "TABLE " [TableName]
    public boolean tableAndTableName() throws ParsingException {
        if (commands[this.index].equals("TABLE")) {
            this.index++;
            return parsePlainText();
        }
        return false;
    }


    public boolean parseCreateTable() throws ParsingException {
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
                    throw new ParsingException("Opening bracket in <CreateTable>  is not followed by an attribute list and/or closing bracket");
                }
            } else {
                return true; // Return true for "TABLE" [TableName] w/o attribute list
            }
        }
        return false;
    }


    public boolean parseDrop() throws ParsingException {
        this.index++;
        //  "DATABASE " [DatabaseName] | "DROP " "TABLE " [TableName]
        if (databaseAndDatabaseName() || tableAndTableName()){
            this.index++;
            return true;
        } else {
            throw new ParsingException("Invalid <DROP> syntax");
        }
    }

    public boolean parseAlter() throws ParsingException {
        this.index++;
        //  "TABLE " [TableName] " " <AlterationType> " " [AttributeName]
        boolean validSyntax = false;
        if (tableAndTableName()){
            if (parseAlterationType()){
                validSyntax = parsePlainText(); // [AttributeName]   ::=  [PlainText]
            }
        }

        if (!validSyntax){ // Necessary to catch failure at any stage of the nested IF conditions
            throw new ParsingException("Invalid <ALTER> syntax");
        } else {
            return true;
        }
    }

    public boolean parseInsert() throws ParsingException {
        this.index++;
        // "INTO " [TableName] " VALUES" "(" <ValueList> ")"
        boolean validSyntax = false;
        if (commands[this.index].equals("INTO ")){
            this.index++;
            if (parsePlainText()){
                this.index++;
                if (commands[this.index].equals("VALUES")){
                    this.index++;
                    if (commands[this.index].equals("(")){
                        validSyntax = parseValueList();
                    }
                }
            }
        }

        if (!validSyntax){ // Necessary to catch failure at any stage of the nested IF conditions
            throw new ParsingException("Invalid <INSERT> syntax");
        } else {
            return true;
        }
    }

    public boolean parseSelect() throws ParsingException {
        this.index++;
        //  <WildAttribList> " FROM " [TableName] |  <WildAttribList> " FROM " [TableName] " WHERE " <Condition>
        if (parseFullSelect() || parseConditionedSelect()){
            return true;
        } else {
            throw new ParsingException("Invalid <SELECT> syntax");
        }
    }

    public boolean parseFullSelect() throws ParsingException {
        // <WildAttribList> " FROM " [TableName]
        if (parseWildAttributeList()){
            this.index++;
            if (commands[this.index].equals("FROM ")){
                this.index++;
                return parsePlainText();
            }
        }
        return false;
    }

    public boolean parseConditionedSelect() throws ParsingException {
        // <WildAttribList> " FROM " [TableName] " WHERE " <Condition>
        if (parseFullSelect()){
            return whereCondition();
        }
        return false;
    }

    public boolean whereCondition() throws ParsingException{
        // " WHERE " <Condition>
        if (commands[this.index].equals(" WHERE ")){
            this.index++;
            return parseCondition();
        }
        return false;
    }

    public boolean parseUpdate() throws ParsingException {
        this.index++;
        // [TableName] " SET " <NameValueList> " WHERE " <Condition>
        boolean validSyntax = false;
        if (parsePlainText()){
            if (commands[this.index].equals(" SET ")){
                if (parseNameValueList()){
                    validSyntax = whereCondition();
                }
            }
        }

        if (!validSyntax){ // Necessary to catch failure at any stage of the nested IF conditions
            throw new ParsingException("Invalid <UPDATE> syntax");
        } else {
            return true;
        }
    }


    public boolean parseDelete() throws ParsingException {
        this.index++;
        // "FROM " [TableName] " WHERE " <Condition>
        boolean validSyntax = false;
        if (commands[this.index].equals("FROM ")){
            this.index++;
            if (parsePlainText()){
                this.index++;
                validSyntax = whereCondition();
            }
        }

        if (!validSyntax){ // Necessary to catch failure at any stage of the nested IF conditions
            throw new ParsingException("Invalid <DELETE> syntax");
        } else {
            return true;
        }
    }

    public boolean parseJoin() throws ParsingException {
        this.index++;
        // [TableName] " AND " [TableName] " ON " [AttributeName] " AND " [AttributeName]
        boolean validSyntax = false;
        if (nameAndName()){
            this.index++;
            if (commands[this.index].equals(" ON ")) {
                this.index++;
                validSyntax = nameAndName();
                this.index++;
            }
        }

        if (!validSyntax){ // Necessary to catch failure at any stage of the nested IF conditions
            throw new ParsingException("Invalid <JOIN> syntax");
        } else {
            return true;
        }
    }

    public boolean nameAndName() throws ParsingException {
        // [TableName] " AND " [TableName] " || " [AttributeName] " AND " [AttributeName]
        if (parsePlainText()) {
            this.index++;
            if (commands[this.index].equals("AND ")) {
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

    // [TableName]       ::=  [PlainText]
    // [AttributeName]   ::=  [PlainText]
    // [DatabaseName]    ::=  [PlainText]
    public boolean parsePlainText() throws ParsingException {
        // [Letter] | [Digit] | [PlainText] [Letter] | [PlainText] [Digit]
        for (char c : commands[this.index].toCharArray()){
            if (!(parseDigit(c) || parseLetter(c))){
                return false;
            }
        }
        return true;
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

    public boolean parseNameValueList() throws ParsingException {
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

    public boolean parseNameValuePair() throws ParsingException {
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
        return (commands[this.index].equals("ADD") || commands[this.index].equals("DROP"));
    }

    public boolean parseValueList() throws ParsingException {
        // [Value] | [Value] "," <ValueList>
        if (parseValue()){
            this.index++;
            if (commands[this.index].equals(",")){
                return parseValueList();
            } else {
                return true;
            }
        }
        return false;
    }

    public boolean parseDigitSequence(){
        // [Digit] | [Digit] [DigitSequence]
        for (char c : commands[this.index].toCharArray()){
            if (!(parseDigit(c))){
                return false;
            }
        }
        return true;
    }

    public boolean parseIntegerLiteral(){
        // [DigitSequence] | "-" [DigitSequence] | "+" [DigitSequence]
        if (plusOrMinus()){ this.index++; }
        return parseDigitSequence();
    }

    public boolean parseFloatLiteral(){
        // [DigitSequence] "." [DigitSequence] | "-" [DigitSequence] "." [DigitSequence] | "+" [DigitSequence] "." [DigitSequence]
        if (plusOrMinus()){ this.index++; }
        if (parseDigitSequence()){
            this.index++;
            if (commands[this.index].equals(".")){
                return parseDigitSequence();
            }
        }
        return false;
    }

    public boolean plusOrMinus(){
        return (commands[this.index].equals("-") || commands[this.index].equals("+"));
    }


    public boolean parseBooleanLiteral(){
        return (commands[this.index].equals("TRUE") || commands[this.index].equals("FALSE"));
    }

    public boolean parseCharLiteral(char c){
        return (parseSpace(c) || parseLetter(c) || parseSymbol(c) || parseDigit(c));
    }

    public boolean parseStringLiteral(){
        // "" | [CharLiteral] | [StringLiteral] [CharLiteral]
        if (commands[this.index].isEmpty()){
            this.index++;
            return true;
        } else {
            for (char c : commands[this.index].toCharArray()) {
                if (!(parseCharLiteral(c))){
                    return false;
                }
            }
            this.index++;
            return true;
        }
    }

    public boolean parseValue() throws ParsingException {
        // "'" [StringLiteral] "'" | [BooleanLiteral] | [FloatLiteral] | [IntegerLiteral] | "NULL"
        if (commands[this.index].equals("'")){
            this.index++;
            boolean result = parseStringLiteral();
            this.index++;
            return (result && commands[this.index].equals("'"));
        } else if (parseBooleanLiteral() || parseFloatLiteral() || parseIntegerLiteral() || commands[this.index].equals("NULL")){
            return true;
        } else {
            throw new ParsingException("Invalid [VALUE] format");
        }
    }

    public boolean parseWildAttributeList() throws ParsingException {
        // <AttributeList> | "*"
        return (parseAttributeList() || commands[this.index].equals("*"));
    }

    public boolean parseAttributeList() throws ParsingException{
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

    public boolean parseCondition() throws ParsingException {
        // "(" <Condition> <BoolOperator> <Condition> ")" | <Condition> <BoolOperator> <Condition> |
        // "(" [AttributeName] <Comparator> [Value] ")" |  [AttributeName] <Comparator> [Value]
        boolean bracketNeedsMatching = false;
        if (commands[this.index].equals("(")){ // If condition opens w/bracket skip it and mark the condition as needing closing bracket
            this.index++;
            bracketNeedsMatching = true;
        }

        if (nestedCondition()){
            this.index++;
            if (parseBoolOperator()){
                this.index++;
                if (nestedCondition()){
                    this.index++;
                    // Either needs a closing bracket and has one || doesn't need one and doesn't have one
                    if ((bracketNeedsMatching && commands[this.index].equals(")")) || (!bracketNeedsMatching && !commands[this.index].equals(")"))){
                        return true;
                    } else {
                        throw new ParsingException("Condition has incorrect bracketing format");
                    }
                }
            }
        }
        return false;
    }

    public boolean nestedCondition() throws ParsingException {
        return (bracketedCondition() || unbracketedCondition());
    }

    public boolean bracketedCondition() throws ParsingException {
        // "(" [AttributeName] <Comparator> [Value] ")"
        if (commands[this.index].equals("(")){
            this.index++;
            if (unbracketedCondition()){
                return (commands[this.index].equals(")"));
            }
        }
        return false;
    }

    public boolean unbracketedCondition() throws ParsingException {
        // [AttributeName] <Comparator> [Value]
        if (parsePlainText()){ // Attribute names just require plain text
            this.index++;
            if (parseComparator()){ // Check followed by comparator
                this.index++;
                if (parseValue()){ // Check followed by value
                    this.index++;
                    return true; // Only if correct sequence met
                }
            }
        }
        return false;
    }

    public boolean parseBoolOperator(){
        // "AND" || "OR"
        return (commands[this.index].equals("AND") || commands[this.index].equals("OR"));
    }


    public boolean parseComparator(){
        // Check if comparator is from the valid set
        String[] comparators = {"==", ">", "<", ">=", "<=", "!=", "LIKE"};

        for (String x : comparators) {
            if (commands[this.index].equals(x)){
                return true;
            }
        }
        return false;
    }



}
