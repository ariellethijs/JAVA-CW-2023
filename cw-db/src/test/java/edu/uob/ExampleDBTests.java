package edu.uob;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.Duration;
import java.util.Arrays;

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

        command = "CREATE TABLE Employee (id , name, age);";
        expectedTokens = new String[]{"CREATE", "TABLE", "Employee", "(", "id", ",", "name", ",", "age",")", ";"};
        String[] returnedTokens3 = tokeniser.tokeniseInput(command).toArray(new String[0]);
        assertArrayEquals(expectedTokens, returnedTokens3);
    }

    @Test
    public void testParser(){
        assertTrue(testUseParse() && testCreateParse() && testDropParse()); //&& testAlterParse() && testInsertParse() &&
                // testSelectParse() && testUpdateParse() && testDeleteParse() && testJoinParse());
    }

    public boolean testUseParse() {
        boolean result = false;
        String validName = generateRandomName();
        String invalidName = generateRandomName() + "!&£*";

        String[] testValidUse = new String[]{"USE", generateRandomName(), ";"};
        DBParser parser1 = new DBParser(testValidUse);

        String[] testInvalidSpacing = new String[]{"US E", generateRandomName(), ";"}; // Improper spacing in command
        DBParser parser2 = new DBParser(testInvalidSpacing);

        String[] testInvalidName = new String[]{"USE", invalidName, ";"}; // Invalid characters in name
        DBParser parser3 = new DBParser(testInvalidName);

        String[] testNoSemiColon = new String[]{"USE", generateRandomName()}; // Missing semicolon at end
        DBParser parser4 = new DBParser(testNoSemiColon);

        try {
            result = (parser1.parseAllTokens() && !parser2.parseAllTokens() && !parser3.parseAllTokens() && !parser4.parseAllTokens());
        } catch (ParsingException pe) {
            // Handle parsing exceptions
            System.err.println("ERROR: " + pe.getMessage());
        }
        return result;
    }

    public boolean testCreateParse(){
        //<CreateDatabase>  ::=  "CREATE " "DATABASE " [DatabaseName]
        //<CreateTable>     ::=  "CREATE " "TABLE " [TableName] | "CREATE " "TABLE " [TableName] "(" <AttributeList> ")"
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
            // Handle parsing exceptions
            System.err.println("ERROR: " + pe.getMessage());
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
            // Handle parsing exceptions
            System.err.println("ERROR: " + pe.getMessage());
        }
        return result;

    }

//    public boolean testAlterParse(){
//
//    }

//    public boolean testInsertParse(){
//
//    }
//
//    public boolean testSelectParse(){
//
//    }

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
