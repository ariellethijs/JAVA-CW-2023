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
            Parser parser = new Parser();
            entityFileReader = new FileReader(entitiesFile);
            parser.parse(entityFileReader);
            Graph allEntities = parser.getGraphs().get(0);
            ArrayList<Graph> entitySections = allEntities.getSubgraphs();
            ArrayList<Graph> locations = entitySections.get(0).getSubgraphs();
            storeLocations(locations);
            ArrayList<Edge> paths = entitySections.get(1).getEdges();
            storePaths(paths);
        } catch (FileNotFoundException fileNotFoundException){
            System.out.println(fileNotFoundException.getMessage());
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }

        if (entityFileReader != null){
            entityFileReader.close();
        }
    }
    private void storeLocations(ArrayList<Graph> locations){
        boolean firstItem = true;

        for (Graph location : locations){
            String locationName = location.getNodes(false).get(0).getId().getId();

            if (!checkIfKeyword(locationName)) {
                if (firstItem) { // Store the name of the first location in the dot file
                    startLocationName = locationName;
                    firstItem = false;
                }

                String locationDescription = location.getNodes(false).get(0).getAttribute("description");
                ArrayList<Graph> locationContents = location.getSubgraphs();

                Location currentLocation = new Location(locationName, locationDescription);

                for (Graph content : locationContents) {
                    storeLocationContent(currentLocation, content, content.getId().getId());
                }
                gameLocations.put(locationName, currentLocation);
            }
        }
    }
    private void storeLocationContent(Location parentLocation, Graph contentSubgraph, String contentType){
        ArrayList<Node> Node = contentSubgraph.getNodes(false);
        for (Node node : Node){
            String name = node.getId().getId();
            String description = node.getAttribute("description");

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
        for (Edge path : paths){
            String fromLocation = path.getSource().getNode().getId().getId();
            String toLocation = path.getTarget().getNode().getId().getId();
            if (gameLocations.containsKey(fromLocation)){
                gameLocations.get(fromLocation).addPathDestination(toLocation);
            }
        }
    }
    public HashMap<String, Location> getGameLocations(){ return gameLocations; }
    public String getStartLocation(){ return startLocationName; }
}
