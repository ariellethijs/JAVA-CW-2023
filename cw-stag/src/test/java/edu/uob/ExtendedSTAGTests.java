package edu.uob;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.io.File;
import java.nio.file.Paths;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

public class ExtendedSTAGTests {
    private GameServer server;

    @BeforeEach
    void setup() {
        File entitiesFile = Paths.get("config" + File.separator + "extended-entities.dot").toAbsolutePath().toFile();
        File actionsFile = Paths.get("config" + File.separator + "extended-actions.xml").toAbsolutePath().toFile();
        server = new GameServer(entitiesFile, actionsFile);
    }

    String sendCommandToServer(String command) {
        // Try to send a command to the server - this call will timeout if it takes too long (in case the server enters an infinite loop)
        return assertTimeoutPreemptively(Duration.ofMillis(1000), () -> { return server.handleCommand(command);},
                "Server took too long to respond (probably stuck in an infinite loop)");
    }

    @Test
    void testValidExtendedActions(){
        sendCommandToServer("Tom: get potion");
        sendCommandToServer("Tom: get axe");
        sendCommandToServer("Tom: get coin");
        sendCommandToServer("Tom: goto forest");
        assertTrue(sendCommandToServer("Tom: chop the tree").contains("You cut down the tree with the axe"));
        sendCommandToServer("Tom: get log");

        sendCommandToServer("Tom: get key");
        sendCommandToServer("Tom: goto cabin");
        assertTrue(sendCommandToServer("Tom: unlock trapdoor").contains("You unlock the door and see steps leading down into a cellar"));

        sendCommandToServer("Tom: goto cellar");
        assertTrue(sendCommandToServer("Tom: attack elf").contains("You attack the elf, but he fights back and you lose some health"));
        assertTrue(sendCommandToServer("Tom: pay elf with coin").contains("You pay the elf your silver coin and he produces a shovel"));
        sendCommandToServer("Tom: get shovel");

        sendCommandToServer("Tom: goto cabin");
        sendCommandToServer("Tom: goto forest");
        sendCommandToServer("Tom: goto riverbank");
        sendCommandToServer("Tom: get horn");
        assertTrue(sendCommandToServer("Tom: blow horn").contains("You blow the horn and as if by magic, a lumberjack appears !"));
        assertTrue(sendCommandToServer("Tom: look").contains("LUMBERJACK"));
        assertTrue(sendCommandToServer("Tom: get lumberjack").contains("players cannot pick other characters"));

        assertTrue(sendCommandToServer("Tom: bridge river with log").contains("You bridge the river with the log and can now reach the other side"));
        assertTrue(sendCommandToServer("Tom: look").contains("CLEARING"));
        sendCommandToServer("Tom: goto clearing");

        assertTrue(sendCommandToServer("Tom: dig ground with shovel").contains("You dig into the soft ground and unearth a pot of gold !!!"));
        assertFalse(sendCommandToServer("Tom: look").contains("GROUND"));
        assertTrue(sendCommandToServer("Tom: look").contains("HOLE"));
        assertTrue(sendCommandToServer("Tom: look").contains("GOLD"));
    }
}
