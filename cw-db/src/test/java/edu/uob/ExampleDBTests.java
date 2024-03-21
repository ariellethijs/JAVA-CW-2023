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

        command = "UPDATE   Employee  SET age =  35 WHERE name !=   'John Doe';";
        expectedTokens = new String[]{"UPDATE", "Employee", "SET", "age", "=", "35", "WHERE", "name", "!=", "'John Doe'", ";"};
        String[] returnedTokens2 = tokeniser.tokeniseInput(command).toArray(new String[0]);
        assertArrayEquals(expectedTokens, returnedTokens2);

        command = "CRE   ATE TABLE Employee (id , name, age);"; // Invalid extra white space
        expectedTokens = new String[]{"CREATE", "TABLE", "Employee", "(", "id", ",", "name", ",", "age",")", ";"};
        String[] returnedTokens3 = tokeniser.tokeniseInput(command).toArray(new String[0]);
        assertNotEquals(expectedTokens, returnedTokens3);
    }

    @Test
    public void testUseParse() {
        String validDatabaseName = generateRandomName();
        String invalidName = generateRandomName() + "!&£*";

        sendCommandToServer("CREATE DATABASE " +validDatabaseName + ";");
        String validUse = sendCommandToServer("USE " + validDatabaseName + ";");
        assertTrue(validUse.contains("[OK]"));

        String testInvalidSpacing = sendCommandToServer("US E " + generateRandomName() + ";"); // Improper spacing in command
        assertTrue(testInvalidSpacing.contains("[ERROR"));

        String testInvalidName = sendCommandToServer("USE " + invalidName + ";"); // Invalid characters in name
        assertTrue(testInvalidName.contains("[ERROR"));

        String testNoSemiColon = sendCommandToServer("USE " + generateRandomName()); // Missing semicolon at end
        assertTrue(testNoSemiColon.contains("[ERROR]"));
    }

    @Test
    public void testCreateParse(){
        String databaseName = generateRandomName();

        String testValidCreateDatabase = sendCommandToServer("CREATE DATABASE " + databaseName + ";");
        assertTrue(testValidCreateDatabase.contains("[OK]"));

        sendCommandToServer("USE " + databaseName + ";"); // So create tables will be valid

        String testValidCreateTable = sendCommandToServer("CREATE TABLE " + generateRandomName() + ";");
        assertTrue(testValidCreateTable.contains("[OK]"));

        String testValidCreateTableWithAttributes = sendCommandToServer("CREATE TABLE " + generateRandomName() +
                        "( " + generateRandomName() + ", " +  generateRandomName() + ", " + generateRandomName() + " );");
        assertTrue(testValidCreateTableWithAttributes.contains("[OK]"));

        String testInvalidCreateDatabase = sendCommandToServer("CREATE DATABASE " + generateRandomName() + " " +
                generateRandomName() + ";"); // Two given names
        assertTrue(testInvalidCreateDatabase.contains("[ERROR]"));

        String testInvalidCreateTable = sendCommandToServer("CREATE TABLE;"); // Missing table name
        assertTrue(testInvalidCreateTable.contains("[ERROR]"));

        // Missing closing brace of attribute list
        String testInvalidCreateTableWithAttributes = sendCommandToServer("CREATE TABLE " + generateRandomName() + "( "
                        + generateRandomName() + "," + generateRandomName() + "," + generateRandomName() + ";");
        assertTrue(testInvalidCreateTableWithAttributes.contains("[ERROR]"));
    }

    @Test
    public void testDropParse(){
        String databaseName = generateRandomName();
        String tableName = generateRandomName();

        sendCommandToServer("CREATE DATABASE " +databaseName + ";");
        sendCommandToServer("USE " +databaseName + ";");
        sendCommandToServer("CREATE TABLE " +tableName + ";");

        String testValidDropTable = sendCommandToServer("DROP TABLE " +tableName + ";");
        assertTrue(testValidDropTable.contains("[OK]"));

        String testValidDropDatabase = sendCommandToServer("DROP DATABASE " +databaseName + ";");
        assertTrue(testValidDropDatabase.contains("[OK]"));

        String testInvalidDropDatabase = sendCommandToServer("DROP DATABASE;");
        assertTrue(testInvalidDropDatabase.contains("[ERROR]"));

        String testInvalidDropTable = sendCommandToServer("DROP TALE " +generateRandomName() + ";");
        assertTrue(testInvalidDropTable.contains("[ERROR]"));
    }

    @Test
    public void testAlterParse(){
        String attributeName = generateRandomName();
        String databaseName = generateRandomName();
        String tableName = generateRandomName();

        sendCommandToServer("CREATE DATABASE " +databaseName + ";");
        sendCommandToServer("USE " +databaseName + ";");
        sendCommandToServer("CREATE TABLE " +tableName + ";");

        String testValidAlterAdd = sendCommandToServer("ALTER TABLE " +tableName + " ADD "
                +attributeName + ";");
        assertTrue(testValidAlterAdd.contains("[OK]"));

        String response = sendCommandToServer("SELECT * FROM " +tableName + ";");
        System.out.println(response);

        String testValidAlterDrop = sendCommandToServer("ALTER TABLE " +tableName + " DROP "
                +attributeName + ";");
        assertTrue(testValidAlterDrop.contains("[OK]"));

        // Missing alteration type
        String testInvalidAlter = sendCommandToServer("ALTER TABLE " +tableName + " "
                +generateRandomName() + ";");
        assertTrue(testInvalidAlter.contains("[ERROR]"));

        // Misspelled key word
        String testInvalidAlterDrop = sendCommandToServer("ALTER TABLE " +tableName + " DRIP " +
                generateRandomName() + ";");
        assertTrue(testInvalidAlterDrop.contains("[ERROR]"));
    }

    @Test
    public void testInsertParse(){
        String validString = '\'' + generateRandomName() + '\'';
        String invalidString = generateRandomName() + '\''; // Missing opening '

        String databaseName = generateRandomName();
        String tableName = generateRandomName();

        sendCommandToServer("CREATE DATABASE " +databaseName + ";");
        sendCommandToServer("USE " +databaseName + ";");
        sendCommandToServer("CREATE TABLE " +tableName + " ( name, age, height );");

        String testValidInsertSingleValue = sendCommandToServer("INSERT INTO " + generateRandomName() + " VALUES (45);");
        assertTrue(testValidInsertSingleValue.contains("[OK]"));

        String testValidInsertMultipleValues = sendCommandToServer("INSERT INTO " + generateRandomName() + " VALUES (" + validString +
                ", TRUE, 45.37, 3, NULL);");
        assertTrue(testValidInsertMultipleValues.contains("[OK]"));

        // No single quotes around string literal
        String testInvalidString = sendCommandToServer("INSERT INTO " + generateRandomName() + " VALUES (" + invalidString +
                ");");
        assertTrue(testInvalidString.contains("[ERROR]"));

        // Missing closing bracket
        String testInvalidInsertBrackets = sendCommandToServer("INSERT INTO " + generateRandomName() + " VALUES(a, 5.5;");
        assertTrue(testInvalidInsertBrackets.contains("[ERROR]"));

        // Missing comma separating values
        String testInvalidInsertCommas = sendCommandToServer("INSERT INTO " + generateRandomName() + " VALUES(a 5, 17);");
        assertTrue(testInvalidInsertCommas.contains("[ERROR]"));

        // Multiple decimal points in float literal
        String testInvalidInsertFloat = sendCommandToServer("INSERT INTO " + generateRandomName() + " VALUES( 5.1.7 );");
        assertTrue(testInvalidInsertFloat.contains("[ERROR]"));

    }

    @Test
    public void testSelectParse(){
        // <Select>          ::=  "SELECT " <WildAttribList> " FROM " [TableName] |
        //                        "SELECT " <WildAttribList> " FROM " [TableName] " WHERE " <Condition>
        // <WildAttribList>  ::=  <AttributeList> | "*"
        // <AttributeList>   ::=  [AttributeName] | [AttributeName] "," <AttributeList>

        // "(" <Condition> <BoolOperator> <Condition> ")" | <Condition> <BoolOperator> <Condition> |
        // "(" [AttributeName] <Comparator> [Value] ")" |  [AttributeName] <Comparator> [Value]

        String databaseName = generateRandomName();
        String tableName = generateRandomName();

        sendCommandToServer("CREATE DATABASE " +databaseName + ";");
        sendCommandToServer("USE " +databaseName + ";");
        sendCommandToServer("CREATE TABLE " +tableName + " ( name, age, height );");
        sendCommandToServer("INSERT INTO " +tableName + " VALUES ('Keith', 45, 172);");
        sendCommandToServer("INSERT INTO " +tableName + " VALUES ('Sarah', 23, 121);");
        sendCommandToServer("INSERT INTO " +tableName + " VALUES ('Amy', 30, 153);");
        sendCommandToServer("INSERT INTO " +tableName + " VALUES ('Keith', 34, 160);");

        String testValidSelectSingleAttribute = sendCommandToServer("SELECT name FROM " +tableName + ";");
        assertTrue(testValidSelectSingleAttribute.contains("[OK]"));

        String testValidSelectAll = sendCommandToServer("SELECT * FROM " +tableName + ";");
        assertTrue(testValidSelectAll.contains("[OK]"));

        String testValidSelectMultipleAttributes = sendCommandToServer("SELECT name, age FROM " +tableName + ";");
        assertTrue(testValidSelectMultipleAttributes.contains("[OK]"));

        String testValidSelectWhereCondition = sendCommandToServer("SELECT name FROM " +tableName +
                " WHERE age >= 20;");
        assertTrue(testValidSelectWhereCondition.contains("[OK]"));

        String testMissingComparator = sendCommandToServer("SELECT name FROM " +tableName + " WHERE age 20;");
        assertTrue(testMissingComparator.contains("[ERROR]"));

        String testMissingAttribute = sendCommandToServer("SELECT FROM " +tableName + " WHERE age == 20;");
        assertTrue(testMissingAttribute.contains("[ERROR]"));

        String testNestedConditions = sendCommandToServer("SELECT * FROM " +tableName + " WHERE ((name == 'Keith') AND (age <= 40));");
        assertTrue(testNestedConditions.contains("[OK]"));
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

    @Test
    public void testInterpretCreate(){
        String randomName = generateRandomName();

        String testValidCreateDatabase = sendCommandToServer("CREATE DATABASE " +randomName + ";");
        assertTrue(testValidCreateDatabase.contains("[OK]"));

        String testInvalidCreateDatabase = sendCommandToServer("CREATE DATABASE database;"); // Attempt to use keyword as name
        assertTrue(testInvalidCreateDatabase.contains("[ERROR]"));

        String testValidUseDatabase = sendCommandToServer("USE " +randomName + ";");
        assertTrue(testValidUseDatabase.contains("[OK]"));

        String testValidCreateTable = sendCommandToServer("CREATE TABLE " + generateRandomName() + ";");
        assertTrue(testValidCreateTable.contains("[OK]"));

        String testValidCreateTableWithAttributes = sendCommandToServer("CREATE TABLE " +generateRandomName() + " ( "
                + generateRandomName() + ", " +  generateRandomName() + ", " + generateRandomName() + " );");
        assertTrue(testValidCreateTableWithAttributes.contains("[OK]"));
    }

    @Test
    public void testInterpretDrop(){
        String databaseName = generateRandomName();
        String table1Name = generateRandomName(); // Set the name to try an individual table drop

        String testValidCreateDatabase = sendCommandToServer("CREATE DATABASE " +databaseName + ";");
        assertTrue(testValidCreateDatabase.contains("[OK]"));

        String testValidUseDatabase = sendCommandToServer("USE " +databaseName + ";");
        assertTrue(testValidUseDatabase.contains("[OK]"));

        String testValidCreateTable = sendCommandToServer("CREATE TABLE " + table1Name + ";");
        assertTrue(testValidCreateTable.contains("[OK]"));

        String testValidCreateTableWithAttributes = sendCommandToServer("CREATE TABLE " +generateRandomName() + " ( "
                + generateRandomName() + ", " +  generateRandomName() + ", " + generateRandomName() + " );");
        assertTrue(testValidCreateTableWithAttributes.contains("[OK]"));

        sendCommandToServer("CREATE TABLE " +generateRandomName() + " ( " + generateRandomName() + ", " +
                generateRandomName() + ", " + generateRandomName() + " );");

        String validTableDrop = sendCommandToServer("DROP TABLE " +table1Name + ";");
        assertTrue(validTableDrop.contains("[OK]"));

        String testValidCreateNewTable = sendCommandToServer("CREATE TABLE " + table1Name + ";");
        // Create table with the same name and see if permitted
        assertTrue(testValidCreateNewTable.contains("[OK]")); // Modify this for an insert or smthn when implemented


        String validDatabaseDrop = sendCommandToServer("DROP DATABASE " +databaseName + ";");
        assertTrue(validDatabaseDrop.contains("[OK]"));

        String testUseDeletedDatabase = sendCommandToServer("USE " +databaseName + ";");
        assertTrue(testUseDeletedDatabase.contains("[ERROR]"));
    }

    @Test
    public void testInterpretAlter(){
        // //  "ALTER " "TABLE " [TableName] " " <AlterationType> " " [AttributeName]
        String databaseName = generateRandomName();
        String tableName = generateRandomName();

        String testValidCreateDatabase = sendCommandToServer("CreAte DATabaSE " +databaseName + ";");
        assertTrue(testValidCreateDatabase.contains("[OK]"));

        String testValidUseDatabase = sendCommandToServer("Use " +databaseName + ";");
        assertTrue(testValidUseDatabase.contains("[OK]"));

        String testValidCreateTableWithAttributes = sendCommandToServer("CREATE TABLE " +tableName + " ( name, age );");
        assertTrue(testValidCreateTableWithAttributes.contains("[OK]"));

        String testValidAlterAdd = sendCommandToServer("AlTER TaBLE " +tableName + " Add height;");
        assertTrue(testValidAlterAdd.contains("[OK]"));

        String testInvalidAlterAdd = sendCommandToServer("ALTER TABLE " +tableName + " ADD name;"); // Already exists !
        assertTrue(testInvalidAlterAdd.contains("[ERROR]"));

        String testValidAlterDrop = sendCommandToServer("ALTER TABLE " +tableName + " DRoP age;");
        assertTrue(testValidAlterDrop.contains("[OK]"));

        String testInvalidAlterDrop = sendCommandToServer("ALTER TABLE " +tableName + " DROP gender;"); // Doesn't exist !
        assertTrue(testInvalidAlterDrop.contains("[ERROR]"));
    }

    @Test
    public void testInterpretInsert(){
        // "INSERT " "INTO " [TableName] " VALUES" "(" <ValueList> ")"

        // Create and use database & create table
        String databaseName = generateRandomName();
        String tableName = generateRandomName();
        sendCommandToServer("CREATE DATABASE " +databaseName + ";");
        sendCommandToServer("USE " +databaseName + ";");
        sendCommandToServer("CREATE TABLE " +tableName + " ( name, age, height );");

        String validInsertValues = sendCommandToServer("INSERT INTO " +tableName + " VALUES ('Keith', 45, 172);");
        assertTrue(validInsertValues.contains("[OK]"));

        sendCommandToServer("INSERT INTO " +tableName + " VALUES ('Sarah', 23, 121);");
        sendCommandToServer("INSERT INTO " +tableName + " VALUES ('Amy', 30, 153);");
        sendCommandToServer("INSERT INTO " +tableName + " VALUES ('Keith', 34, NULL);");

        String testMoreValuesThanAttributes = sendCommandToServer("INSERT INTO " +tableName + " VALUES ('Kevin', 32, 184, 'male');");
        assertTrue(testMoreValuesThanAttributes.contains("[ERROR]"));

    }

    @Test
    public void testInterpretSelect(){
        // "SELECT " <WildAttribList> " FROM " [TableName] |
        // "SELECT " <WildAttribList> " FROM " [TableName] " WHERE " <Condition>

        //  <AttributeList> | "*"
        //  [AttributeName] | [AttributeName] "," <AttributeList>

        // "(" <Condition> <BoolOperator> <Condition> ")" | <Condition> <BoolOperator> <Condition> | "(" [AttributeName] <Comparator> [Value] ")" | [AttributeName] <Comparator> [Value]
        // <BoolOperator>    ::= "AND" | "OR"
        // <Comparator>      ::=  "==" | ">" | "<" | ">=" | "<=" | "!=" | " LIKE "

        String databaseName = generateRandomName();
        String tableName = generateRandomName();
        sendCommandToServer("CREATE DATABASE " +databaseName + ";");
        sendCommandToServer("USE " +databaseName + ";");
        sendCommandToServer("CREATE TABLE " +tableName + " ( name, age, height );");
        sendCommandToServer("INSERT INTO " +tableName + " VALUES ('Keith', 45, 172);");
        sendCommandToServer("INSERT INTO " +tableName + " VALUES ('Sarah', 23, 121);");
        sendCommandToServer("INSERT INTO " +tableName + " VALUES ('Amy', 30, 153);");
        sendCommandToServer("INSERT INTO " +tableName + " VALUES ('Keith', 34, 160);");

        String validSelectAll = sendCommandToServer("SELECT * FROM " +tableName + ";");
        System.out.println(validSelectAll);
        assertTrue(validSelectAll.contains("[OK]"));

        String validSelectSome = sendCommandToServer("SELECT name, age FROM " +tableName + ";");
        System.out.println(validSelectSome);
        assertTrue(validSelectSome.contains("[OK]"));

        String validSelectOne = sendCommandToServer("SELECT age FROM " +tableName + ";");
        System.out.println(validSelectOne);
        assertTrue(validSelectOne.contains("[OK]"));

        String invalidSelectOne = sendCommandToServer("SELECT gender FROM " +tableName + ";");
        assertTrue(invalidSelectOne.contains("[ERROR]"));

        String validConditionedSelectSome = sendCommandToServer("SELECT id, name FROM " +tableName + " WHERE age <= 34;");
        System.out.println(validConditionedSelectSome);
        assertTrue(validConditionedSelectSome.contains("[OK]"));

        String validNestedConditionSelect = sendCommandToServer("SELECT id, name FROM " +tableName + " WHERE (age < 30 OR age > 40) AND height > 150;");
        System.out.println(validNestedConditionSelect);
        assertTrue(validNestedConditionSelect.contains("[OK]"));

        String validSelectHeadersOnly = sendCommandToServer("SELECT * FROM " +tableName + " WHERE age <= 10;");
        assertTrue(validSelectHeadersOnly.contains("[OK]"));
    }

}
