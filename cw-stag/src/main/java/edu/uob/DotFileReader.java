package edu.uob;

import com.alexmerz.graphviz.ParseException;
import com.alexmerz.graphviz.Parser;
import com.alexmerz.graphviz.objects.Edge;
import com.alexmerz.graphviz.objects.Graph;
import com.alexmerz.graphviz.objects.Node;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class DotFileReader extends GameFileReader {
    private final HashMap<String, Location> gameLocations;
    private FileReader entityFileReader;
    private String startLocationName;

    public DotFileReader(){
        gameLocations = new HashMap<>();
        entityFileReader = null;
    }

    public void openAndReadEntityFile(File entitiesFile) throws IOException {
        try {
            // Parse the entity file
            Parser parser = new Parser();
            entityFileReader = new FileReader(entitiesFile);
            parser.parse(entityFileReader);

            // Extract All Entities as a separate graph
            Graph allEntities = parser.getGraphs().get(0);
            ArrayList<Graph> entitySections = allEntities.getSubgraphs();

            // Extract all locations as separate graph and store each individual location
            ArrayList<Graph> locations = entitySections.get(0).getSubgraphs();
            storeLocations(locations);

            // Extract all paths as a separate path and store each path
            ArrayList<Edge> paths = entitySections.get(1).getEdges();
            storePaths(paths);
        } catch (FileNotFoundException fileNotFoundException){
            System.out.println(fileNotFoundException.getMessage());
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }

        // Close the file reader after use
        if (entityFileReader != null){
            entityFileReader.close();
        }
    }

    private void storeLocations(ArrayList<Graph> locations){
        boolean firstItem = true;
        // Iterate all location in file
        for (Graph location : locations){
            // Retrieve the entity name
            String locationName = location.getNodes(false).get(0).getId().getId().toLowerCase();

            // Ensure name is not a restricted keyword
            if (!checkIfKeyword(locationName)) {
                // Store the name of the first location in the dot file
                if (firstItem) {
                    startLocationName = locationName;
                    firstItem = false;
                }

                // Extract the location description from the graph
                String locationDescription = location.getNodes(false).get(0).getAttribute("description");

                // Extract a list of the location's subgraph
                ArrayList<Graph> locationContents = location.getSubgraphs();

                // Create a location with the extracted info
                Location currentLocation = new Location(locationName, locationDescription);

                // Iterate each entity subgraph and store the contents to the currentLocation
                for (Graph content : locationContents) {
                    storeLocationContent(currentLocation, content, content.getId().getId());
                }
                gameLocations.put(locationName, currentLocation);
            }
        }
    }

    private void storeLocationContent(Location parentLocation, Graph contentSubgraph, String contentType){
        // Extract each individual entity node from the current subgraph
        ArrayList<Node> Node = contentSubgraph.getNodes(false);
        for (Node node : Node){
            // Extract the name and description of the entity
            String name = node.getId().getId().toLowerCase();
            String description = node.getAttribute("description");

            // Store the entity based on its type
            if (!checkIfKeyword(name)) {
                switch (contentType) {
                    case "artefacts" -> parentLocation.addArtefactToLocation(new Artefact(name, description));
                    case "furniture" -> parentLocation.addFurnitureToLocation(new Furniture(name, description));
                    case "characters" -> parentLocation.addCharacterToLocation(new Character(name, description));
                }
            }
        }
    }

    private void storePaths(ArrayList<Edge> paths){
        // Iterate each path in graph
        for (Edge path : paths){
            // Extract to and from location
            String fromLocation = path.getSource().getNode().getId().getId().toLowerCase();
            String toLocation = path.getTarget().getNode().getId().getId().toLowerCase();
            // Store the path in the fromLocation
            if (gameLocations.containsKey(fromLocation)){
                gameLocations.get(fromLocation).addPathDestination(toLocation);
            }
        }
    }

    public HashMap<String, Location> getGameLocations(){ return gameLocations; }
    public String getStartLocation(){ return startLocationName; }
}
