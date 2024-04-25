package edu.uob;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.io.File;
import java.nio.file.Paths;
import java.io.IOException;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class ExampleSTAGTests {

  private GameServer server;

  // Create a new server _before_ every @Test
  @BeforeEach
  void setup() {
      File entitiesFile = Paths.get("config" + File.separator + "basic-entities.dot").toAbsolutePath().toFile();
      File actionsFile = Paths.get("config" + File.separator + "basic-actions.xml").toAbsolutePath().toFile();
      server = new GameServer(entitiesFile, actionsFile);
  }

  String sendCommandToServer(String command) {
      // Try to send a command to the server - this call will timeout if it takes too long (in case the server enters an infinite loop)
      return assertTimeoutPreemptively(Duration.ofMillis(1000), () -> { return server.handleCommand(command);},
      "Server took too long to respond (probably stuck in an infinite loop)");
  }

  // A lot of tests will probably check the game state using 'look' - so we better make sure 'look' works well !
  @Test
  void testLook() {
    String response = sendCommandToServer("simon: look");
    response = response.toLowerCase();
    assertTrue(response.contains("cabin"), "Did not see the name of the current room in response to look");
    assertTrue(response.contains("log cabin"), "Did not see a description of the room in response to look");
    assertTrue(response.contains("magic potion"), "Did not see a description of artifacts in response to look");
    assertTrue(response.contains("wooden trapdoor"), "Did not see description of furniture in response to look");
    assertTrue(response.contains("forest"), "Did not see available paths in response to look");
  }

  // Test that we can pick something up and that it appears in our inventory
  @Test
  void testGet()
  {
      String response;
      sendCommandToServer("simon: get potion");
      response = sendCommandToServer("simon: inv");
      response = response.toLowerCase();
      assertTrue(response.contains("potion"), "Did not see the potion in the inventory after an attempt was made to get it");
      response = sendCommandToServer("simon: look");
      response = response.toLowerCase();
      assertFalse(response.contains("potion"), "Potion is still present in the room after an attempt was made to get it");
  }

  // Test that we can goto a different location (we won't get very far if we can't move around the game !)
  @Test
  void testGoto()
  {
      sendCommandToServer("simon: goto forest");
      String response = sendCommandToServer("simon: look");
      response = response.toLowerCase();
      assertTrue(response.contains("key"), "Failed attempt to use 'goto' command to move to the forest - there is no key in the current location");
  }

  @Test
  void testPlayerIdentification(){
      // Missing player name
      assertEquals("Expecting a player name at the start of command", sendCommandToServer("inv"));
      // Missing :
      assertEquals("Expecting a player name at the start of command", sendCommandToServer("Tom inventory"));

      // Test case-insensitive player name storage
      sendCommandToServer("Kisshan: get potion");
      assertTrue(sendCommandToServer("kisshan: inv").contains("potion"));

      // Test for separate player inventories and item removal from location
      sendCommandToServer("Tom: get axe");
      assertTrue(sendCommandToServer("Tom: inv").contains("axe"));
      assertFalse(sendCommandToServer("Tom: inv").contains("potion"));
      assertFalse(sendCommandToServer("Tom: look").contains("axe") || sendCommandToServer("Tom: look").contains("potion"));

      // Test multiple players separate locations
      sendCommandToServer("Tom: goto forest");
      assertTrue(sendCommandToServer("Tom: look").contains("forest"));
      assertFalse(sendCommandToServer("Kisshan: look").contains("forest"));
  }

  @Test
    void testInvalidInbuiltCommands(){
      // No command key word:
        assertEquals(sendCommandToServer("Tom: Kisshan"), "Try entering a valid command next time");

      // Multiple command keywords:
        assertEquals(sendCommandToServer("Kisshan: look goto"), "Cannot process multiple commands at once");

      // Get command tests:
        // Attempting furniture pickup:
        assertEquals(sendCommandToServer("Kisshan: get trapdoor"), "Player cannot pick up items of furniture!");
        // Attempting pickup of an item not in the location:
        assertEquals(sendCommandToServer("Kisshan: get hammer"), "There is no hammer in Kisshan's current location");
        // Attempting multiple item pickup:
        assertEquals(sendCommandToServer("Kisshan: get potion axe"), "Players cannot pick up multiple items at once!");

      // Drop command tests:
        // No item specified:
        assertEquals(sendCommandToServer("Kisshan: drop"), "Player must specify which artefact they are referring to");
        // Item player doesn't have:
        assertEquals(sendCommandToServer("Kisshan: drop axe"), "No axe in Kisshan's inventory!");
        // Item another player has:
        sendCommandToServer("Tom: get potion");
        assertEquals(sendCommandToServer("Kisshan: drop potion"), "No potion in Kisshan's inventory!");

      // Goto command tests:
        // No destination specified:
        assertEquals(sendCommandToServer("Kisshan: goto"), "Player must specify the destination they wish to go to!");
        // No location with that name:
        assertEquals(sendCommandToServer("Kisshan: goto library"), "There is no library nearby!");
        // No path to location from current location:
        assertEquals(sendCommandToServer("Kisshan: goto cellar"), "There's no path to cellar from cabin");
  }

  @Test
    void testBasicAction(){
      sendCommandToServer("Tom: get axe");
      sendCommandToServer("Tom: goto forest");
      assertEquals(sendCommandToServer("Tom: please chop the tree with the axe"), "You cut down the tree with the axe");
      assertTrue(sendCommandToServer("Tom: look").contains("log"));
      assertFalse(sendCommandToServer("Tom: look").contains("tree"));
      assertTrue(sendCommandToServer("Tom: inv").contains("axe"));

      sendCommandToServer("Tom: get key");
      sendCommandToServer("Tom: goto cabin");
      assertEquals(sendCommandToServer("Tom: unlock trapdoor with key"), "You unlock the trapdoor and see steps leading down into a cellar");
      assertTrue(sendCommandToServer("Tom: look").contains("CELLAR"));
      assertFalse(sendCommandToServer("Tom: inventory").contains("key"));
  }

}
