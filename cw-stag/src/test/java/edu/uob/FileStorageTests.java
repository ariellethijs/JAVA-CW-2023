package edu.uob;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;


public class FileStorageTests {
    private DotFileReader dotFileReader;
    private XMLFileReader basicXMLFileReader;
    private XMLFileReader extendedXMLFileReader;

    @BeforeEach
    public void setUp() {
        dotFileReader = new DotFileReader();
    }

    @Test
    public void testBasicEntitiesDataStorage() {
        File file = Paths.get("config" + File.separator + "basic-entities.dot").toAbsolutePath().toFile();
        try {
            dotFileReader.openAndReadEntityFile(file);
            HashMap<String, Location> gameLocations = dotFileReader.getGameLocations();
            assertNotNull(gameLocations);
            assertEquals(4, gameLocations.size()); // Assuming there are 4 locations in the test file
            assertNotNull(dotFileReader.getStartLocation());

            // Verify specific data for the first dot file
            Location cabin = gameLocations.get("cabin");
            assertNotNull(cabin);
            assertEquals("A log cabin in the woods", cabin.getDescription());
            assertEquals(2, cabin.getLocationArtefacts().size());
            assertEquals(1, cabin.getLocationFurniture().size());

            Location forest = gameLocations.get("forest");
            assertNotNull(forest);
            assertEquals("A dark forest", forest.getDescription());
            assertEquals(1, forest.getLocationArtefacts().size());
            assertEquals(1, forest.getLocationFurniture().size());

            Location cellar = gameLocations.get("cellar");
            assertNotNull(cellar);
            assertEquals("A dusty cellar", cellar.getDescription());
            assertEquals(1, cellar.getLocationCharacters().size());

            Location storeroom = gameLocations.get("storeroom");
            assertNotNull(storeroom);
            assertEquals("Storage for any entities not placed in the game", storeroom.getDescription());
            assertEquals(1, storeroom.getLocationArtefacts().size());
            assertEquals(0, storeroom.getLocationFurniture().size());
        } catch (IOException e){
            System.out.println(e.getMessage());
        }
    }

    @Test
    public void testExtendedEntitiesDataStorage() {
        File file = Paths.get("config" + File.separator + "extended-entities.dot").toAbsolutePath().toFile();
        try {
            dotFileReader.openAndReadEntityFile(file);
            HashMap<String, Location> gameLocations = dotFileReader.getGameLocations();
            assertNotNull(gameLocations);
            assertEquals(6, gameLocations.size()); // Assuming there are 6 locations in the test file
            assertNotNull(dotFileReader.getStartLocation());

            // Verify specific data for the second dot file
            Location cabin = gameLocations.get("cabin");
            assertNotNull(cabin);
            assertEquals("A log cabin in the woods", cabin.getDescription());
            assertEquals(3, cabin.getLocationArtefacts().size());
            assertEquals(1, cabin.getLocationFurniture().size());

            Location forest = gameLocations.get("forest");
            assertNotNull(forest);
            assertEquals("A deep dark forest", forest.getDescription());
            assertEquals(1, forest.getLocationArtefacts().size());
            assertEquals(1, forest.getLocationFurniture().size());

            Location cellar = gameLocations.get("cellar");
            assertNotNull(cellar);
            assertEquals("A dusty cellar", cellar.getDescription());
            assertEquals(1, cellar.getLocationCharacters().size());

            Location riverbank = gameLocations.get("riverbank");
            assertNotNull(riverbank);
            assertEquals("A grassy riverbank", riverbank.getDescription());
            assertEquals(1, riverbank.getLocationArtefacts().size());
            assertEquals(1, riverbank.getLocationFurniture().size());

            Location clearing = gameLocations.get("clearing");
            assertNotNull(clearing);
            assertEquals("A clearing in the woods", clearing.getDescription());
            assertEquals(0, clearing.getLocationArtefacts().size());
            assertEquals(1, clearing.getLocationFurniture().size());

            Location storeroom = gameLocations.get("storeroom");
            assertNotNull(storeroom);
            assertEquals("Storage for any entities not placed in the game", storeroom.getDescription());
            assertEquals(3, storeroom.getLocationArtefacts().size());
            assertEquals(1, storeroom.getLocationFurniture().size());
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    @Test
    public void testBasicActionsFileStorage () {
        try {
            basicXMLFileReader = new XMLFileReader(Paths.get("config" + File.separator + "basic-actions.xml").toAbsolutePath().toFile());
        } catch (IOException | ParserConfigurationException | SAXException e){
            System.out.println(e.getMessage());
        }
        HashMap<String, HashSet<GameAction>> allGameActions = basicXMLFileReader.getAllGameActions();
        assertNotNull(allGameActions);
        assertEquals(9, allGameActions.size());

        assertTrue(allGameActions.containsKey("hit"));
        assertTrue(allGameActions.containsKey("attack"));
        assertTrue(allGameActions.containsKey("fight"));
        assertTrue(allGameActions.containsKey("cutdown"));
        assertTrue(allGameActions.containsKey("cut"));
        assertTrue(allGameActions.containsKey("open"));
        assertTrue(allGameActions.containsKey("unlock"));
        assertTrue(allGameActions.containsKey("chop"));
        assertTrue(allGameActions.containsKey("drink"));

        // Verify subjects, consumed, and produced entities for actions in the first XML file
        HashSet<GameAction> openActions = allGameActions.get("open");
        assertNotNull(openActions);
        for (GameAction action : openActions) {
            assertEquals(2, action.getActionSubjects().size());
            assertTrue(action.getActionSubjects().contains("trapdoor"));
            assertTrue(action.getActionSubjects().contains("key"));

            assertEquals(1, action.getConsumedEntities().size());
            assertTrue(action.getConsumedEntities().contains("key"));

            assertEquals(1, action.getProducedEntities().size());
            assertTrue(action.getProducedEntities().contains("cellar"));
        }
    }

    @Test
    public void testExtendedActionsFileStorage(){
        try {
            extendedXMLFileReader = new XMLFileReader(Paths.get("config" + File.separator + "extended-actions.xml").toAbsolutePath().toFile());
        } catch (IOException | ParserConfigurationException | SAXException e){
            System.out.println(e.getMessage());
        }
        HashMap<String, HashSet<GameAction>> allGameActions = extendedXMLFileReader.getAllGameActions();
        assertNotNull(allGameActions);
        assertEquals(13, allGameActions.size());

        assertTrue(allGameActions.containsKey("open"));
        assertTrue(allGameActions.containsKey("unlock"));
        assertTrue(allGameActions.containsKey("chop"));
        assertTrue(allGameActions.containsKey("drink"));
        assertTrue(allGameActions.containsKey("fight"));
        assertTrue(allGameActions.containsKey("pay"));
        assertTrue(allGameActions.containsKey("bridge"));

        // Check individual storage of a random action
        HashSet<GameAction> digActions = allGameActions.get("dig");
        assertNotNull(digActions);
        for (GameAction action : digActions) {
            assertEquals(2, action.getActionSubjects().size());
            assertTrue(action.getActionSubjects().contains("ground"));
            assertTrue(action.getActionSubjects().contains("shovel"));

            assertEquals(1, action.getConsumedEntities().size());
            assertTrue(action.getConsumedEntities().contains("ground"));

            assertEquals(2, action.getProducedEntities().size());
            assertTrue(action.getProducedEntities().contains("hole"));
            assertTrue(action.getProducedEntities().contains("gold"));
        }

        // Ensure equivalent triggers point to the same action object
        HashSet<GameAction> openActions = allGameActions.get("open");
        HashSet<GameAction> unlockActions = allGameActions.get("unlock");
        GameAction action1 = null;
        GameAction action2 = null;
        for (GameAction action : openActions) { action1 = action; }
        for (GameAction action : unlockActions) { action2 = action; }
        assertNotNull(action1);
        assertNotNull(action2);
        assertEquals(action1, action2);
    }

}
