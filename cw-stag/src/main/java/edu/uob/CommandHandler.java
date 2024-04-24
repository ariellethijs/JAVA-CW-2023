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
    String[] commandKeywords = { "inventory",
            "get", "drop", "goto",
            "look"
    };

    CommandHandler(HashMap<String, Location> layout, HashMap<String, HashSet<GameAction>> actions, String firstLocation){
        gameLayout = layout;
        possibleActions = actions;
        startLocation = gameLayout.get(firstLocation);
        allPlayers = new HashMap<>();
        playerIndex = 0;
        tokenIndex = 0;
    }

    String handleBuiltInCommand(String incomingCommand) throws IOException {
        if (checkForMultipleKeywords(incomingCommand)){
            throw new IOException("Cannot process multiple commands at once");
        } else {
            tokenIndex = 0;
            command = incomingCommand.trim().split("\\s+");
            currentPlayer = determineCommandPlayer(command[tokenIndex]);
            tokenIndex++;

            return switch (command[tokenIndex].toLowerCase()) {
                case "inventory", "inv" -> processInventory();
                case "look" -> processLook();
                case "get", "drop" -> processGetOrDrop(command[tokenIndex].toLowerCase());
                case "goto" -> processGoTo();
                default -> processGameAction();
            };
        }
    }

    boolean checkForMultipleKeywords(String command){
        int keywordCount = 0;
        for (String keyword : commandKeywords){
            if (command.contains(keyword)){ keywordCount++; }
        }
        return (keywordCount > 1);
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
            StringBuilder response = new StringBuilder("[" + currentPlayer.getName().toUpperCase() + "'S INVENTORY]\n");
            for (GameEntity entity : currentPlayer.getInventory().values()){
                response.append("      ");
                response = appendEntityDescription(response, entity);
            }
            return String.valueOf(response);
        } else {
            return currentPlayer.getName() + "'s inventory is empty";
        }
    }

    String processLook(){
        Location currentLocation = currentPlayer.getCurrentLocation();

        StringBuilder response = new StringBuilder("[" + currentLocation.getName().toUpperCase() + "] " + currentLocation.getDescription() + "\n");

        response = iterateThroughLocationEntities(response, currentLocation.getLocationArtefacts());
        response = iterateThroughLocationEntities(response, currentLocation.getLocationFurniture());
        response = iterateThroughLocationEntities(response, currentLocation.getLocationCharacters());

        if (!currentLocation.getPathsTo().isEmpty()){
            response.append("[PATHS]\n");
        }
        for (String path : currentLocation.getPathsTo()){
            response.append("     ").append("[").append(currentLocation.getName().toUpperCase()).append("] --> [").append(path.toUpperCase()).append("]\n");
        }

        return String.valueOf(response);
    }

    StringBuilder iterateThroughLocationEntities(StringBuilder response, HashMap<String, GameEntity> entityMap){
        String indentation = "     ";
        for (GameEntity entity : entityMap.values()){
            response.append(indentation);
            response = appendEntityDescription(response, entity);
        }
        return response;
    }

    StringBuilder appendEntityDescription(StringBuilder response, GameEntity entity){
        return response.append("[").append(entity.getName().toUpperCase()).append("] ").append(entity.getDescription()).append("\n");
    }

    String processGetOrDrop(String commandType) throws IOException {
        if (tokenIndex < (command.length-1)){
            tokenIndex++;
            String potentialKey = command[tokenIndex].toLowerCase();
            Location currentLocation = currentPlayer.getCurrentLocation();

            if (commandType.equals("get")){
                return pickUpItem(potentialKey);
            } else if (currentPlayer.getInventory().containsKey(potentialKey)){
                GameEntity artefactToMove = currentPlayer.getInventory().get(potentialKey);
                currentLocation.addArtefactToLocation((Artefact)artefactToMove);
                currentPlayer.removeFromInventory(artefactToMove);
                return currentPlayer.getName() + " dropped the " +command[tokenIndex] + "\n";
            } else {
                throw new IOException("No " +potentialKey + " in " +currentPlayer.getName() + "'s inventory!");
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
                throw new IOException("There is no " +itemKey + " in " +currentPlayer.getName()+ "'s current location");
            }
        } else {
            if (tokenIndex+1 < command.length){
                throw new IOException("Players cannot pick up multiple items at once!");
            } else {
                GameEntity artefactToMove = currentLocation.getLocationArtefacts().get(itemKey);
                currentLocation.removeEntity(artefactToMove);
                currentPlayer.addToInventory(artefactToMove);
                return currentPlayer.getName() + " picked up the " +command[tokenIndex] + "\n";
            }
        }
    }

    String processGoTo() throws IOException {
        Location currentLocation = currentPlayer.getCurrentLocation();
        if (tokenIndex < (command.length-1)) {
            tokenIndex++;
            String potentialDestination = command[tokenIndex].toLowerCase();

            if (gameLayout.containsKey(potentialDestination)){
                if (currentLocation.getPathsTo().contains(potentialDestination)){
                    currentPlayer.setLocation(gameLayout.get(potentialDestination));
                    return currentPlayer.getName() + " moved to " +potentialDestination + " from " +currentLocation.getName() + "\n";
                } else {
                    throw new IOException("There's no path to " +potentialDestination + " from " +currentLocation.getName());
                }
            } else {
                throw new IOException("There is no " +command[tokenIndex] + " nearby!");
            }
        } else {
            throw new IOException("Player must specify the destination they wish to go to!");
        }
    }

    String processGameAction() throws IOException {
        if (possibleActions.containsKey(command[tokenIndex].toLowerCase())){
            return "";
        } else {
            throw new IOException("Try entering a valid command next time");
        }
    }


}
