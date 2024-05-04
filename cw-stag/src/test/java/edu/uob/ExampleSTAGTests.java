package edu.uob;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.io.File;
import java.nio.file.Paths;
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
  void testGet(){
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
  void testGoto() {
      sendCommandToServer("simon: goto forest");
      String response = sendCommandToServer("simon: look");
      response = response.toLowerCase();
      assertTrue(response.contains("key"), "Failed attempt to use 'goto' command to move to the forest - there is no key in the current location");
  }

  @Test
  void testPlayerIdentification(){
      // Missing player name
      assertTrue(sendCommandToServer("inv").contains("Not sure whose playing - start with your name next time"));
      // Missing :
      assertTrue(sendCommandToServer("Tom inventory").contains("Not sure whose playing - start with your name next time"));

      // Test case-insensitive player name storage
      sendCommandToServer("Kisshan: get potion");
      sendCommandToServer("Kisshan: health");
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
        assertTrue(sendCommandToServer("Tom: Kisshan").contains("tom isn't sure what you mean - try a valid command next time"));

      // Multiple command keywords:
        assertTrue(sendCommandToServer("Kisshan: look goto").contains("kisshan can't multi-task - enter one command at a time"));

      // Get command tests:
        // Attempting furniture pickup:
        assertTrue(sendCommandToServer("Kisshan: get trapdoor").contains("kisshan tries to pick up the trapdoor but it seems to be fixed in place - players cannot pick up items of furniture"));
        // Attempting pickup of an item not in the location:
        assertTrue(sendCommandToServer("Kisshan: get hammer").contains("kisshan isn't sure what you want to do - be more specific"));
        // Attempting multiple item pickup:
        assertTrue(sendCommandToServer("Kisshan: get potion axe").contains("kisshan can't multi-task - you can only goto one location at a time, or handle one item at a time"));

      // Drop command tests:
        // No item specified:
        assertTrue(sendCommandToServer("Kisshan: drop").contains("kisshan isn't sure what you want to do - be more specific"));
        // Item player doesn't have:
        assertTrue(sendCommandToServer("Kisshan: drop axe").contains("kisshan can't see a axe in their inventory - try running inventory or inv to see contents"));
        // Item another player has:
        sendCommandToServer("Tom: get potion");
        assertTrue(sendCommandToServer("Kisshan: get potion").contains("kisshan can't see a potion anywhere - try running look to see objects in the area"));

      // Goto command tests:
        // No destination specified:
        assertTrue(sendCommandToServer("Kisshan: goto").contains("kisshan isn't sure what you want to do - be more specific"));
        // No location with that name:
        assertTrue(sendCommandToServer("Kisshan: goto library").contains("kisshan isn't sure what you want to do - be more specific"));
        // No path to location from current location:
        assertTrue(sendCommandToServer("Kisshan: goto cellar").contains("kisshan can't see a way to cellar from here - try running look to see available paths"));
  }

  @Test
    void testBasicAction(){
      sendCommandToServer("Tom: get axe");
      sendCommandToServer("Tom: get potion");
      sendCommandToServer("Tom: goto forest");
      assertTrue(sendCommandToServer("Tom: please chop the tree with the axe").contains("You cut down the tree with the axe"));
      assertTrue(sendCommandToServer("Tom: look").contains("log"));
      assertFalse(sendCommandToServer("Tom: look").contains("tree"));
      assertTrue(sendCommandToServer("Tom: inv").contains("axe"));

      sendCommandToServer("Tom: get key");
      sendCommandToServer("Tom: goto cabin");
      assertTrue(sendCommandToServer("Tom: unlock trapdoor with key").contains("You unlock the trapdoor and see steps leading down into a cellar"));
      assertTrue(sendCommandToServer("Tom: look").contains("CELLAR"));
      assertFalse(sendCommandToServer("Tom: inventory").contains("key"));

      // Test health loss/gain and player death
      sendCommandToServer("Tom: goto cellar");
      assertTrue(sendCommandToServer("Tom: look").contains("ELF"));
      assertTrue(sendCommandToServer("Tom: attack elf").contains("You attack the elf, but he fights back and you lose some health"));
      assertTrue(sendCommandToServer("Tom: health").contains("2"));
      assertTrue(sendCommandToServer("Tom: drink potion").contains("You drink the potion and your health improves"));
      assertTrue(sendCommandToServer("Tom: health").contains("3"));
      sendCommandToServer("Tom: attack elf");
      sendCommandToServer("Tom: attack elf");
      assertTrue(sendCommandToServer("Tom: attack elf").contains("tom died and lost all of their items"));
      assertTrue(sendCommandToServer("Tom: look").contains("CABIN"));
      assertTrue(sendCommandToServer("Tom: health").contains("3"));
      assertTrue(sendCommandToServer("Tom: inv").contains("empty"));
  }

  @Test
    void testEdgeCaseBasicAction(){
      sendCommandToServer("Tom: get axe");
      sendCommandToServer("Tom: goto forest");


      // Invalid gibberish including trigger word
      assertEquals("tom isn't sure what you mean - try a valid command next time", sendCommandToServer("Tom: gfdjifhskchop tree"));

      // Invalid gibberish including entity name
      assertEquals("tom isn't sure what to do - try entering a valid command next time", sendCommandToServer("Tom: chop treefhdshsl"));
      assertEquals("tom isn't sure what you want to do - be more specific", sendCommandToServer("tom: get fhjakhfksakey"));

      // Invalid composite commands
      assertTrue(sendCommandToServer("Tom: Chop tree and get key").contains("tom can't multi-task - enter one command at a time"));

      // Extraneous entities
      assertTrue(sendCommandToServer("Tom: chop tree with axe and potion").contains("tom isn't sure what to do - don't include extraneous objects in action calls"));

      // Different word order && decorated syntax
      assertTrue(sendCommandToServer("Tom: please use the axe to chop the tree").contains("You cut down the tree with the axe"));

      sendCommandToServer("Tom: get key");
      sendCommandToServer("Tom: goto cabin");

      // Invalid partial command
      assertTrue(sendCommandToServer("Tom: unlock").contains("tom isn't sure what to do - try entering a valid command next time"));
      // Valid partial command
      assertTrue(sendCommandToServer("Tom: unlock trapdoor").contains("You unlock the trapdoor and see steps leading down into a cellar"));

      // Command with missing trigger
      assertTrue(sendCommandToServer("Tom: axe tree").contains("tom isn't sure what you mean - try a valid command next time"));

      // Invalid action trigger
      assertTrue(sendCommandToServer("Tom: use hammer to chop tree").contains("tom isn't sure what to do - try entering a valid command next time"));

      // Invalid subject
      assertTrue(sendCommandToServer("Tom: chop tree with hammer").contains("tom isn't sure what to do - try entering a valid command next time"));

      // Invalid action trigger and subject
      assertTrue(sendCommandToServer("Tom: burn tree with fire").contains("tom isn't sure what you mean - try a valid command next time"));

  }

}
