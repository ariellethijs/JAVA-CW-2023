package edu.uob;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;

public class CommandHandler {
    HashMap<String, Location> gameLayout;
    HashMap<String, HashSet<GameAction>> possibleActions;
    GamePlayer currentPlayer;
    Location startLocation;
    HashMap<String, GamePlayer> allPlayers;
    int playerIndex;
    int tokenIndex;
    String[] command;

    CommandHandler(HashMap<String, Location> layout, HashMap<String, HashSet<GameAction>> actions, String firstLocation){
        gameLayout = layout;
        possibleActions = actions;
        startLocation = gameLayout.get(firstLocation);
        allPlayers = new HashMap<>();
        playerIndex = 0;
        tokenIndex = 0;
    }

    String handleBuiltInCommand(String incomingCommand) throws IOException {
        tokenIndex = 0;
        command = incomingCommand.trim().split("\\s+");
        currentPlayer = determineCommandPlayer(command[tokenIndex]);
        tokenIndex++;

        return switch (command[tokenIndex].toLowerCase()) {
            case "inventory", "inv" -> processInventory();
            case "look" -> processLook();
            case "get" -> processGetOrDrop("get");
            case "drop" -> processGetOrDrop("drop");
            case "goto" -> processGoTo();
            default -> "";
        };
    }

    GamePlayer determineCommandPlayer(String name) throws IOException {
        if (name.charAt(name.length()-1) == ':'){
            name = name.substring(0, (name.length() - 1));
            String nameAsKey = name.toLowerCase();
            if (allPlayers.containsKey(nameAsKey)){
                return allPlayers.get(nameAsKey);
            } else {
                playerIndex++;
                GamePlayer newPlayer = new GamePlayer(name, playerIndex, startLocation);
                allPlayers.put(nameAsKey, newPlayer);
                return newPlayer;
            }
        } else {
            throw new IOException("Expecting a player name at the start of command");
        }
    }

    String processInventory(){
        if (!currentPlayer.getInventory().isEmpty()){
            StringBuilder response = new StringBuilder(currentPlayer.getName() + "'s inventory: \n");
            for (Artefact a : currentPlayer.getInventory().values()){
                response.append(a.getName()).append(" ").append(a.getDescription()).append("\n");
            }
            return String.valueOf(response);
        } else {
            return currentPlayer.getName() + "'s inventory is empty";
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

    String processGetOrDrop(String commandType) throws IOException {
        if (tokenIndex < command.length){
            tokenIndex++;
            String potentialKey = command[tokenIndex].toLowerCase();
            Location currentLocation = currentPlayer.getCurrentLocation();

            if (commandType.equals("get")){
                return pickUpItem(potentialKey);
            } else if (currentPlayer.getInventory().containsKey(potentialKey)){
                Artefact artefactToMove = currentPlayer.getInventory().get(potentialKey);
                currentLocation.addArtefactToLocation(artefactToMove);
                currentPlayer.removeFromInventory(artefactToMove);
                return currentPlayer.getName() + " dropped the " +command[tokenIndex];
            } else {
                throw new IOException("No such artefact in " +currentPlayer.getName() + "'s inventory!");
            }
        } else {
            throw new IOException("Player must specify which artefact they are referring to");
        }
    }

    String pickUpItem(String itemKey) throws IOException {
        Location currentLocation = currentPlayer.getCurrentLocation();
        if (!currentLocation.getLocationArtefacts().containsKey(itemKey)){
            if (currentLocation.getLocationFurniture().containsKey(itemKey)){
                throw new IOException("Player cannot pick up items of furniture!");
            } else if (currentLocation.getLocationCharacters().containsKey(itemKey)){
                throw new IOException("Player cannot pick up game characters!");
            } else {
                throw new IOException("There is no " +itemKey + " in that player's current location");
            }
        } else {
            if (tokenIndex+1 < command.length){
                throw new IOException("Players cannot pick up multiple items at once!");
            } else {
                Artefact artefactToMove = currentLocation.getLocationArtefacts().get(itemKey);
                currentLocation.removeEntity(artefactToMove);
                currentPlayer.addToInventory(artefactToMove);
                return currentPlayer.getName() + " picked up the " +command[tokenIndex];
            }
        }
    }

    String processGoTo() throws IOException {
        Location currentLocation = currentPlayer.getCurrentLocation();
        if (tokenIndex < command.length) {
            tokenIndex++;
            String potentialDestination = command[tokenIndex].toLowerCase();

            if (gameLayout.containsKey(potentialDestination)){
                if (currentLocation.getPathsTo().contains(potentialDestination)){
                    currentPlayer.setLocation(gameLayout.get(potentialDestination));
                    return currentLocation.getName() + " moved to " +potentialDestination + " from " +currentLocation.getName();
                } else {
                    throw new IOException("There's no path to " +potentialDestination + " from " +currentLocation.getName());
                }
            } else {
                throw new IOException("There is no " +command[tokenIndex] + " in the game!");
            }
        } else {
            throw new IOException("Must specify the destination you wish to go to!");
        }
    }


}
