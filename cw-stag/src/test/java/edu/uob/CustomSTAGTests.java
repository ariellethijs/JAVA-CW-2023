package edu.uob;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.io.File;
import java.nio.file.Paths;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

public class CustomSTAGTests {
    private GameServer server;

    @BeforeEach
    void setup() {
        File entitiesFile = Paths.get("config" + File.separator + "test-entities.dot").toAbsolutePath().toFile();
        File actionsFile = Paths.get("config" + File.separator + "test-actions.xml").toAbsolutePath().toFile();
        server = new GameServer(entitiesFile, actionsFile);
    }

    String sendCommandToServer(String command) {
        // Try to send a command to the server - this call will timeout if it takes too long (in case the server enters an infinite loop)
        return assertTimeoutPreemptively(Duration.ofMillis(1000), () -> { return server.handleCommand(command);},
                "Server took too long to respond (probably stuck in an infinite loop)");
    }

    @Test
    void testCustomGame(){
        sendCommandToServer("arielle: goto forest");
        sendCommandToServer("arielle: get flute");

        // Testing an action which requires a specific current location
        sendCommandToServer("arielle: goto cabin");
        assertEquals("arielle isn't sure what to do - try entering a valid command next time", sendCommandToServer("arielle: play flute"));
        sendCommandToServer("arielle: goto forest");
        assertEquals("You play the magic flute, a magic portal opens up and you feel the hot desert sun through it's glow", sendCommandToServer("arielle: play flute"));

        sendCommandToServer("arielle: goto desert");
        assertTrue(sendCommandToServer("arielle: look").contains("SWORD"));
        sendCommandToServer("arielle: get sword");
        sendCommandToServer("arielle: goto forest");
        sendCommandToServer("arielle: drop sword");
        sendCommandToServer("arielle: goto desert");

        // Testing consumed locations
        sendCommandToServer("arielle: play flute");
        assertFalse(sendCommandToServer("arielle: look").contains("FOREST"));

        // Testing multiple trigger words for the same action
        assertTrue(sendCommandToServer("arielle: look").contains("RAT"));
        assertEquals("You brave your crippling fear of rodents and give the little rat a pat. He quickly burrows into the ground, revealing a tunnel down ...", sendCommandToServer("arielle: stroke pet rat"));
        assertTrue(sendCommandToServer("arielle: look").contains("LAKESIDE"));
        sendCommandToServer("arielle: goto lakeside");

        // Testing death
        sendCommandToServer("arielle: dive into the lake");
        assertTrue(sendCommandToServer("arielle: health").contains("2"));
        sendCommandToServer("arielle: swim in lake");
        sendCommandToServer("arielle: swim in lake");
        assertTrue(sendCommandToServer("arielle: inv").contains("empty"));
        assertTrue(sendCommandToServer("arielle: look").contains("CABIN"));
        sendCommandToServer("arielle: goto forest");
        sendCommandToServer("arielle: get key");
        sendCommandToServer("arielle: get hammer");
        sendCommandToServer("arielle: get sword");
        sendCommandToServer("arielle: goto cabin");

        // Testing resolving multiple open actions with adding more subjects
        assertEquals("arielle isn't sure what to do - which open action do you want to perform?", sendCommandToServer("arielle: unlock trapdoor"));
        assertEquals("You bash open the trapdoor with the hammer and see steps leading down into a cellar", sendCommandToServer("arielle: unlock trapdoor with hammer"));

        sendCommandToServer("arielle: goto cellar");
        sendCommandToServer("arielle: look");

        // Testing an action which consumes a character
        sendCommandToServer("arielle: cut elf");
        assertFalse(sendCommandToServer("arielle: look").contains("ELF"));
    }

}
