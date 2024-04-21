package edu.uob;

import java.util.ArrayList;
import java.util.HashMap;

public class CommandHandler {

    HashMap<String, Location> gameLayout;
    ArrayList<GameAction> possibleActions;
    GamePlayer currentPlayer;

    Location startLocation;

    HashMap<String, GamePlayer> allPlayers;
    int playerIndex;


    String[] command;

    CommandHandler(HashMap<String, Location> layout, ArrayList<GameAction> actions, String firstLocation){
        gameLayout = layout;
        possibleActions = actions;
        startLocation = gameLayout.get(firstLocation);
        allPlayers = new HashMap<>();
        playerIndex = 0;
    }

    String handleBuiltInCommand(String incomingCommand){
        command = incomingCommand.trim().split("\\s+");
        currentPlayer = determineCommandPlayer(command[0]);

        switch (command[1].toLowerCase()){
            case "look":
                return processLook();
        }
        return "";

    }

    GamePlayer determineCommandPlayer(String name){
        if (allPlayers.containsKey(name)){
            return allPlayers.get(name);
        } else {
            playerIndex++;
            return new GamePlayer(name, playerIndex, startLocation);
        }
    }

    String processLook(){
        Location currentLocation = currentPlayer.getCurrentLocation();

        StringBuilder response = new StringBuilder(currentLocation.getName() + " " + currentLocation.getDescription() + "\n");

        for (Artefact a : currentLocation.getLocationArtefacts().values()) {
            response.append(a.getName()).append(" ").append(a.getDescription()).append("\n");
        }

        for (Furniture f : currentLocation.getLocationFurniture().values()){
            response.append(f.getName()).append(" ").append(f.getDescription()).append("\n");
        }

        for (Character c : currentLocation.getLocationCharacters().values()) {
            response.append(c.getName()).append(" ").append(c.getDescription()).append("\n");
        }

        for (String path : currentLocation.getPathsTo()){
            response.append(currentLocation.getName()).append(" --> ").append(path).append("\n");
        }

        return String.valueOf(response);
    }

}
