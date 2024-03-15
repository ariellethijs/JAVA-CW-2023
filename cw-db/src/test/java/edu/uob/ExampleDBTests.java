package edu.uob;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.Duration;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class ExampleDBTests {

    private DBServer server;

    // Create a new server _before_ every @Test
    @BeforeEach
    public void setup() {
        server = new DBServer();
    }

    // Random name generator - useful for testing "bare earth" queries (i.e. where tables don't previously exist)
    private String generateRandomName() {
        String randomName = "";
        for(int i=0; i<10 ;i++) randomName += (char)( 97 + (Math.random() * 25.0));
        return randomName;
    }

    private String sendCommandToServer(String command) {
        // Try to send a command to the server - this call will timeout if it takes too long (in case the server enters an infinite loop)
        return assertTimeoutPreemptively(Duration.ofMillis(1000), () -> { return server.handleCommand(command);},
        "Server took too long to respond (probably stuck in an infinite loop)");
    }

    // A basic test that creates a database, creates a table, inserts some test data, then queries it.
    // It then checks the response to see that a couple of the entries in the table are returned as expected
    @Test
    public void testBasicCreateAndQuery() {
        String randomName = generateRandomName();
        sendCommandToServer("CREATE DATABASE " + randomName + ";");
        sendCommandToServer("USE " + randomName + ";");
        sendCommandToServer("CREATE TABLE marks (name, mark, pass);");
        sendCommandToServer("INSERT INTO marks VALUES ('Simon', 65, TRUE);");
        sendCommandToServer("INSERT INTO marks VALUES ('Sion', 55, TRUE);");
        sendCommandToServer("INSERT INTO marks VALUES ('Rob', 35, FALSE);");
        sendCommandToServer("INSERT INTO marks VALUES ('Chris', 20, FALSE);");
        String response = sendCommandToServer("SELECT * FROM marks;");
        assertTrue(response.contains("[OK]"), "A valid query was made, however an [OK] tag was not returned");
        assertFalse(response.contains("[ERROR]"), "A valid query was made, however an [ERROR] tag was returned");
        assertTrue(response.contains("Simon"), "An attempt was made to add Simon to the table, but they were not returned by SELECT *");
        assertTrue(response.contains("Chris"), "An attempt was made to add Chris to the table, but they were not returned by SELECT *");
    }

    // A test to make sure that querying returns a valid ID (this test also implicitly checks the "==" condition)
    // (these IDs are used to create relations between tables, so it is essential that suitable IDs are being generated and returned !)
    @Test
    public void testQueryID() {
        String randomName = generateRandomName();
        sendCommandToServer("CREATE DATABASE " + randomName + ";");
        sendCommandToServer("USE " + randomName + ";");
        sendCommandToServer("CREATE TABLE marks (name, mark, pass);");
        sendCommandToServer("INSERT INTO marks VALUES ('Simon', 65, TRUE);");
        String response = sendCommandToServer("SELECT id FROM marks WHERE name == 'Simon';");
        // Convert multi-lined responses into just a single line
        String singleLine = response.replace("\n"," ").trim();
        // Split the line on the space character
        String[] tokens = singleLine.split(" ");
        // Check that the very last token is a number (which should be the ID of the entry)
        String lastToken = tokens[tokens.length-1];
        try {
            Integer.parseInt(lastToken);
        } catch (NumberFormatException nfe) {
            fail("The last token returned by `SELECT id FROM marks WHERE name == 'Simon';` should have been an integer ID, but was " + lastToken);
        }
    }

    // A test to make sure that databases can be reopened after server restart
    @Test
    public void testTablePersistsAfterRestart() {
        String randomName = generateRandomName();
        sendCommandToServer("CREATE DATABASE " + randomName + ";");
        sendCommandToServer("USE " + randomName + ";");
        sendCommandToServer("CREATE TABLE marks (name, mark, pass);");
        sendCommandToServer("INSERT INTO marks VALUES ('Simon', 65, TRUE);");
        // Create a new server object
        server = new DBServer();
        sendCommandToServer("USE " + randomName + ";");
        String response = sendCommandToServer("SELECT * FROM marks;");
        assertTrue(response.contains("Simon"), "Simon was added to a table and the server restarted - but Simon was not returned by SELECT *");
    }

    // Test to make sure that the [ERROR] tag is returned in the case of an error (and NOT the [OK] tag)
    @Test
    public void testForErrorTag() {
        String randomName = generateRandomName();
        sendCommandToServer("CREATE DATABASE " + randomName + ";");
        sendCommandToServer("USE " + randomName + ";");
        sendCommandToServer("CREATE TABLE marks (name, mark, pass);");
        sendCommandToServer("INSERT INTO marks VALUES ('Simon', 65, TRUE);");
        String response = sendCommandToServer("SELECT * FROM libraryfines;");
        assertTrue(response.contains("[ERROR]"), "An attempt was made to access a non-existent table, however an [ERROR] tag was not returned");
        assertFalse(response.contains("[OK]"), "An attempt was made to access a non-existent table, however an [OK] tag was returned");
    }

    @Test
    public void testTokeniser(){
        DBTokeniser tokeniser = new DBTokeniser();
        // Tests for tokeniser
        String command = "INSERT   INTO Employee  VALUES (1,   'John Doe', 30);  ";
        String[] expectedTokens = { "INSERT", "INTO", "Employee", "VALUES", "(", "1", ",", "'John Doe'", ",", "30", ")", ";" };
        String[] returnedTokens1 = tokeniser.tokeniseInput(command).toArray(new String[0]);
        assertArrayEquals(expectedTokens, returnedTokens1);

        command = "UPDATE   Employee  SET age =  35 WHERE name =   'John Doe';";
        expectedTokens = new String[]{"UPDATE", "Employee", "SET", "age", "=", "35", "WHERE", "name", "=", "'John Doe'", ";"};
        String[] returnedTokens2 = tokeniser.tokeniseInput(command).toArray(new String[0]);
        assertArrayEquals(expectedTokens, returnedTokens2);

        command = "CRE   ATE TABLE Employee (id , name, age);"; // Invalid extra white space
        expectedTokens = new String[]{"CREATE", "TABLE", "Employee", "(", "id", ",", "name", ",", "age",")", ";"};
        String[] returnedTokens3 = tokeniser.tokeniseInput(command).toArray(new String[0]);
        assertNotEquals(expectedTokens, returnedTokens3);
    }

    @Test
    public void testUseParse() {
        String invalidName = generateRandomName() + "!&£*";

        String validUse = sendCommandToServer("USE " + generateRandomName() + ";");
        assertTrue(validUse.contains("[OK]"));

        String testInvalidSpacing = sendCommandToServer("US E" + generateRandomName() + ";"); // Improper spacing in command
        assertTrue(testInvalidSpacing.contains("[ERROR"));

        String testInvalidName = sendCommandToServer("USE" + invalidName + ";"); // Invalid characters in name
        assertTrue(testInvalidName.contains("[ERROR"));

        String testNoSemiColon = sendCommandToServer("USE" + generateRandomName()); // Missing semicolon at end
        assertTrue(testNoSemiColon.contains("[ERROR]"));
    }

    public boolean testCreateParse(){
        boolean result = false;

        String[] testValidCreateDatabase = new String[]{"CREATE", "DATABASE", generateRandomName(), ";"};
        DBParser parser1 = new DBParser(testValidCreateDatabase);

        String[] testValidCreateTable = new String[]{"CREATE", "TABLE", generateRandomName(), ";"};
        DBParser parser2 = new DBParser(testValidCreateTable);

        String[] testValidCreateTableWithAttributes = new String[]{
                "CREATE", "TABLE", generateRandomName(), "(", generateRandomName(),
                ",", generateRandomName(), ",", generateRandomName(), ")", ";"};
        DBParser parser3 = new DBParser(testValidCreateTableWithAttributes);

        String[] testInvalidCreateDatabase = new String[]{"CREATE", "DATABASE",
                generateRandomName(), generateRandomName(), ";"}; // Two given names
        DBParser parser4 = new DBParser(testInvalidCreateDatabase);

        String[] testInvalidCreateTable = new String[]{"CREATE", "TABLE", ";"}; // Missing table name
        DBParser parser5 = new DBParser(testInvalidCreateTable);

        String[] testInvalidCreateTableWithAttributes = new String[]{ // Missing closing brace of attribute list
                "CREATE", "TABLE", generateRandomName(), "(", generateRandomName(),
                ",", generateRandomName(), ",", generateRandomName(), ";"};
        DBParser parser6 = new DBParser(testInvalidCreateTableWithAttributes);

        try {
            result = (parser1.parseAllTokens() && parser2.parseAllTokens() && parser3.parseAllTokens()
                    && !parser4.parseAllTokens() && !parser5.parseAllTokens() && !parser6.parseAllTokens());
        } catch (ParsingException pe) {
            // System.err.println("ERROR: " + pe.getMessage());
        }
        return result;
    }

    public boolean testDropParse(){
        boolean result = false;

        String[] testValidDropDatabase = new String[]{"DROP", "DATABASE", generateRandomName(), ";"};
        DBParser parser1 = new DBParser(testValidDropDatabase);

        String[] testValidDropTable = new String[]{"DROP", "TABLE", generateRandomName(), ";"};
        DBParser parser2 = new DBParser(testValidDropTable);

        String[] testInvalidDropDatabase = new String[]{"DROP", "DATABASE", ";"};
        DBParser parser3 = new DBParser(testInvalidDropDatabase);

        String[] testInvalidDropTable = new String[]{"DROP", "TALE", generateRandomName(), ";"};
        DBParser parser4 = new DBParser(testInvalidDropTable);

        try {
            result = (parser1.parseAllTokens() && parser2.parseAllTokens() && !parser3.parseAllTokens()
                    && !parser4.parseAllTokens());
        } catch (ParsingException pe) {
            // System.err.println("ERROR: " + pe.getMessage());
        }
        return result;

    }

    public boolean testAlterParse(){
        //  "TABLE " [TableName] " " <AlterationType> " " [AttributeName]
        boolean result = false;

        String[] testValidAlterAdd = new String[]{"ALTER", "TABLE", generateRandomName(), "ADD",
                generateRandomName() , ";"};
        DBParser parser1 = new DBParser(testValidAlterAdd);

        String[] testValidAlterDrop = new String[]{"ALTER", "TABLE", generateRandomName(), "DROP",
                generateRandomName() , ";"};
        DBParser parser2 = new DBParser(testValidAlterDrop);

        String[] testInvalidAlterAdd = new String[]{"ALTER", "TABLE", "ADD", generateRandomName(),
                ";"}; // Missing table name
        DBParser parser3 = new DBParser(testInvalidAlterAdd);

        String[] testInvalidAlterDrop = new String[]{"ALTER", "TABLE", generateRandomName(), "DRIP",
                generateRandomName() , ";"}; // Misspelled key word
        DBParser parser4 = new DBParser(testInvalidAlterDrop);


        try {
            result = (parser1.parseAllTokens() && parser2.parseAllTokens() && !parser3.parseAllTokens()
                   && !parser4.parseAllTokens());
        } catch (ParsingException pe) {
            // System.err.println("ERROR: " + pe.getMessage());
        }
        return result;
    }

    public boolean testInsertParse(){
        // "INSERT" "INTO " [TableName] " VALUES" "(" <ValueList> ")"
        // VALUE ::== "'" [StringLiteral] "'" | [BooleanLiteral] | [FloatLiteral] | [IntegerLiteral] | "NULL"
        boolean result = false;
        String validString = '\'' + generateRandomName() + '\'';
        String invalidString = generateRandomName() + '\''; // Missing opening '

        String[] testValidInsertSingleValue = new String[]{"INSERT", "INTO", generateRandomName(), "VALUES", "(", "45", ")", ";"};
        DBParser parser1 = new DBParser(testValidInsertSingleValue);

        String[] testValidInsertMultipleValues = new String[]{"INSERT", "INTO", generateRandomName(), "VALUES", "(", validString,
                ",", "TRUE", ",", "45.37", ",", "3", ",", "NULL", ")", ";"};
        DBParser parser2 = new DBParser(testValidInsertMultipleValues);

        String[] testInvalidString = new String[]{"INSERT", "INTO", generateRandomName(), "VALUES", "(", invalidString,
                ")", ";"};
        DBParser parser3 = new DBParser(testInvalidString);

        String[] testInvalidInsertBrackets = new String[]{"INSERT", "INTO", generateRandomName(), "VALUES",
                "(", "a", ",", "5.5", ";"}; // Missing closing bracket
        DBParser parser4 = new DBParser(testInvalidInsertBrackets);

        String[] testInvalidInsertComments = new String[]{"INSERT", "INTO", generateRandomName(), "VALUES",
                "(", "a", "5.5", ";"}; // Missing comma separating values
        DBParser parser5 = new DBParser(testInvalidInsertComments);

        try {
            result = (parser1.parseAllTokens() && parser2.parseAllTokens() && !parser3.parseAllTokens()
                    && !parser4.parseAllTokens() && !parser5.parseAllTokens());
        } catch (ParsingException pe) {
            // System.err.println("ERROR: " + pe.getMessage());
        }
        return result;

    }

    public boolean testSelectParse(){
        // <Select>          ::=  "SELECT " <WildAttribList> " FROM " [TableName] |
        //                        "SELECT " <WildAttribList> " FROM " [TableName] " WHERE " <Condition>
        // <WildAttribList>  ::=  <AttributeList> | "*"
        // <AttributeList>   ::=  [AttributeName] | [AttributeName] "," <AttributeList>

        boolean result = false;

        String[] testValidSelectSingleAttribute = new String[]{"SELECT", "name", "FROM", generateRandomName(), ";"};
        DBParser parser1 = new DBParser(testValidSelectSingleAttribute);

        String[] testValidSelectAll = new String[]{"SELECT", "*", "FROM", generateRandomName() , ";"};
        DBParser parser2 = new DBParser(testValidSelectAll);

        String[] testValidSelectMultipleAttributes = new String[]{"SELECT", "name", ",", "age", "FROM", generateRandomName(), ";"};
        DBParser parser3 = new DBParser(testValidSelectMultipleAttributes);

        String[] testValidSelectWhereCondition = new String[]{"SELECT", "name", "FROM", generateRandomName(),
                "WHERE", "age", ">=", "20", ";"}; // Missing table name
        DBParser parser4 = new DBParser(testValidSelectWhereCondition);

        String[] testInvalidSelect = new String[]{"ALTER", "TABLE", generateRandomName(), "DRIP",
                generateRandomName() , ";"}; // Misspelled key word
        DBParser parser5 = new DBParser(testInvalidSelect);

        String[] testInvalidSelectWhereCondition = new String[]{"ALTER", "TABLE", generateRandomName(), "DRIP",
                generateRandomName() , ";"}; // Misspelled key word
        DBParser parser6 = new DBParser(testInvalidSelectWhereCondition);


        try {
            result = (parser1.parseAllTokens() && parser2.parseAllTokens() && parser3.parseAllTokens() &&
                    parser4.parseAllTokens() && !parser5.parseAllTokens() && !parser6.parseAllTokens());
        } catch (ParsingException pe) {
            // System.err.println("ERROR: " + pe.getMessage());
        }
        return result;


    }

//    public boolean testUpdateParse(){
//
//    }

//    public boolean testDeleteParse(){
//
//    }

//    public boolean testJoinParse(){
//
//    }




}
