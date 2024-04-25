package edu.uob;

import java.io.IOException;
import java.util.ArrayList;
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
        incomingCommand = incomingCommand.toLowerCase();

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
                default -> processGameAction(incomingCommand);
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
                currentPlayer.removeFromInventory(artefactToMove.getName());
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
                currentLocation.removeEntity(artefactToMove, false);
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

    String processGameAction(String incomingCommand) throws IOException {
        String triggerPhrase = determineActionTrigger();
        if (!triggerPhrase.isEmpty()){
            GameAction validAction = determineAction(possibleActions.get(triggerPhrase), incomingCommand);
            executeConsumedEntities(validAction.getConsumedEntities());
            executeProducedEntities(validAction.getProducedEntities());
            return validAction.getNarration();
        } else {
            throw new IOException("Try entering a valid command next time");
        }
    }

    String determineActionTrigger() throws IOException {
        String triggerPhrase = "";
        int triggerPhraseCount = 0;
        for (String token : command){
            if (possibleActions.containsKey(token.toLowerCase())){
                triggerPhraseCount++;
                triggerPhrase = token;
            }
        }
        if (!(triggerPhraseCount > 1)){
            return triggerPhrase.toLowerCase();
        } else {
            throw new IOException("Cannot process multiple commands at once");
        }
    }

    GameAction determineAction(HashSet<GameAction> actions, String incomingCommand) throws IOException {
        for (GameAction action : actions){
            if (checkSubjectsInCommand(action, incomingCommand) && checkSubjectsInLocation(action)){
                return action;
            }
        }
        throw new IOException("Subjects for that action aren't present");
    }

    boolean checkSubjectsInCommand(GameAction action, String incomingCommand){
        for (String subject : action.getActionSubjects()){
            System.out.println("Checking if " +subject + " is present in the command");
            if (!(incomingCommand.contains(subject.toLowerCase()))){
                System.out.println(subject + " was not present in the command");
                return false;
            }
        }
        return true;
    }

    boolean checkSubjectsInLocation(GameAction action){
        Location currentLocation = currentPlayer.getCurrentLocation();
        for (String subject : action.getActionSubjects()){
            System.out.println("Checking if " +subject + " is present in the current location");
            if (!((currentLocation.checkEntityPresent(subject)) || (currentPlayer.checkInventoryContains(subject)))){
                System.out.println(subject + " was not present in the current location");
                return false;
            }
        }
        return true;
    }

    void executeConsumedEntities(ArrayList<String> consumed){
        Location currentLocation = currentPlayer.getCurrentLocation();
        for (String consumedEntity : consumed){
            if (currentLocation.checkEntityPresent(consumedEntity)){
                GameEntity entityToRemove = currentLocation.determineEntityFromName(consumedEntity);
                System.out.println("Removing " +entityToRemove.getName() + " from current location");
                currentLocation.removeEntity(entityToRemove, true);
            } else {
                currentPlayer.removeFromInventory(consumedEntity);
            }
        }
    }

    void executeProducedEntities(ArrayList<String> produced) throws IOException {
        Location storeRoom = gameLayout.get("storeroom");
        Location currentLocation = currentPlayer.getCurrentLocation();

        for (String producedName : produced){
            if (storeRoom.checkEntityPresent(producedName)){
                GameEntity producedEntity = storeRoom.determineEntityFromName(producedName);
                storeRoom.removeEntity(producedEntity, true);
                currentLocation.addEntity(producedEntity);
            } else if (gameLayout.containsKey(producedName)){
                currentLocation.addPathDestination(producedName);
            } else {
                throw new IOException ("Couldn't find the produced entity"); // FOR DEBUGGING
            }
        }

    }

}
