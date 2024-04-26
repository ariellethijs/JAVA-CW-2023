package edu.uob;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class CommandHandler {
    HashMap<String, Location> gameLayout;
    HashMap<String, HashSet<GameAction>> possibleActions;
    GamePlayer currentPlayer;
    Location currentLocation;
    Location startLocation;
    HashMap<String, GamePlayer> allPlayers;
    int playerIndex;
    int tokenIndex;
    String[] command;
    String[] commandKeywords = {
            "inv", //"inventory",
            "get", "drop",
            "goto", "look", "health"
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
        command = incomingCommand.trim().split("\\s+");
        String commandKeyword = determineCommandKeyword(incomingCommand);
        currentPlayer = determineCommandPlayer(command[0]);
        currentLocation = currentPlayer.getCurrentLocation();

        return switch (commandKeyword) {
            case "inventory", "inv" -> processInventory();
            case "look" -> processLook();
            case "get", "drop" -> processGetOrDrop(commandKeyword);
            case "goto" -> processGoTo();
            case "health" -> currentPlayer.getName() + "'s health: " +currentPlayer.getHealth();
            default -> processGameAction(incomingCommand);
        };
    }

    String determineCommandKeyword(String incomingCommand) throws IOException {
        int keywordCount = 0;
        String commandPhrase = "";

        for (String keyword : commandKeywords){
            if (incomingCommand.contains(keyword)){
                commandPhrase = keyword;
                keywordCount++;
            }
        }

        int possibleActionCount = 0;
        for (String token : command){
            if (possibleActions.containsKey(token)){
                possibleActionCount++;
            }
        }

        if  (((keywordCount == 1) && (possibleActionCount == 0)) || ((keywordCount == 0) && (possibleActionCount == 1))){
            return commandPhrase;
        } else if (keywordCount == 0 && possibleActionCount == 0) {
cd             throw new IOException("\nNot sure what you mean - try a valid command next time");
        } else {
            throw new IOException("\nPlayer can't multi-task - enter one command at a time");
        }
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
                startLocation.addCharacterToLocation(newPlayer);
                return newPlayer;
            }
        } else {
            throw new IOException("Not sure whose playing - start with your name next time");
        }
    }

    String processInventory(){
        if (!currentPlayer.getInventory().isEmpty()){
            StringBuilder response = new StringBuilder("\n[" + currentPlayer.getName().toUpperCase() + "'S INVENTORY]\n");
            for (GameEntity entity : currentPlayer.getInventory().values()){
                response.append("      ");
                response = appendEntityDescription(response, entity);
            }
            return String.valueOf(response);
        } else {
            return "\n" + currentPlayer.getName() + "'s inventory is empty";
        }
    }

    String processLook(){
        Location currentLocation = currentPlayer.getCurrentLocation();

        StringBuilder response = new StringBuilder("\n[" + currentLocation.getName().toUpperCase() + "] " + currentLocation.getDescription() + "\n");

        response = iterateThroughLocationEntities(response, currentLocation.getLocationArtefacts());
        response = iterateThroughLocationEntities(response, currentLocation.getLocationFurniture());
        response = iterateThroughLocationEntities(response, currentLocation.getLocationCharacters());

        if (!currentLocation.getPathsTo().isEmpty()){
            response.append("\n[PATHS]");
        }
        for (String path : currentLocation.getPathsTo()){
            response.append("\n     ").append("[").append(currentLocation.getName().toUpperCase()).append("] --> [").append(path.toUpperCase()).append("]");
        }
        return String.valueOf(response);
    }

    StringBuilder iterateThroughLocationEntities(StringBuilder response, HashMap<String, GameEntity> entityMap){
        String indentation = "     ";
        for (GameEntity entity : entityMap.values()){
            if (!entity.getName().equals(currentPlayer.getName())){ // Skip over the current player when looking
                response.append(indentation);
                response = appendEntityDescription(response, entity);
            }
        }
        return response;
    }

    StringBuilder appendEntityDescription(StringBuilder response, GameEntity entity){
        return response.append("[").append(entity.getName().toUpperCase()).append("] ").append(entity.getDescription()).append("\n");
    }

    String processGetOrDrop(String commandType) throws IOException {
        String relevantEntityName = determineRelevantEntity();

        if (commandType.equals("get")){
            return pickUpItem(relevantEntityName);
        } else if (currentPlayer.getInventory().containsKey(relevantEntityName)){
            GameEntity artefactToMove = currentPlayer.getInventory().get(relevantEntityName);
            currentLocation.addArtefactToLocation((Artefact)artefactToMove);
            currentPlayer.removeFromInventory(artefactToMove.getName());
            return "\n" + currentPlayer.getName() + " dropped the " +relevantEntityName;
        } else {
            throw new IOException("\n" +currentPlayer.getName()+ " can''t see a " +relevantEntityName +
                    " in their inventory - try running inventory or inv to see contents");
        }
    }

    String determineRelevantEntity() throws IOException {
        String relevantEntity = "";
        int entityCount = 0;

        for (String token : command) {
            if (currentLocation.checkEntityPresent(token) || currentPlayer.checkInventoryContains(token)){
                relevantEntity = token;
                entityCount++;
            }
        }

        if (entityCount == 1){
            return relevantEntity;
        } else if (entityCount == 0){
            throw new IOException("\n" +currentPlayer.getName() + " isn't sure what you mean - be more specific");
        } else {
            throw new IOException("\n" +currentPlayer.getName() + " can't multi-task - only handle one object at a time");
        }
    }

    String pickUpItem(String itemKey) throws IOException {
        if (currentLocation.getLocationFurniture().containsKey(itemKey)){
            throw new IOException("\n" + currentPlayer.getName() + " tries to pick up the " +itemKey +
                    " but it seems to be fixed in place - players cannot pick up items of furniture");
        } else if (currentLocation.getLocationCharacters().containsKey(itemKey)){
            throw new IOException("\n" + currentPlayer.getName() + " tries to pick up the " +itemKey +
                    " but they don't seem too happy about it - players cannot pick other characters");
        } else if (currentLocation.getLocationArtefacts().containsKey(itemKey)){
            GameEntity artefactToMove = currentLocation.getLocationArtefacts().get(itemKey);
            currentLocation.removeEntity(artefactToMove, false);
            currentPlayer.addToInventory(artefactToMove);
            return "\n" + currentPlayer.getName() + " picked up the " +itemKey;
        } else {
            throw new IOException("\n" +currentPlayer.getName()+ " can''t see a " +itemKey +
                    " anywhere - try running look to see objects in the area");
        }
    }

    String processGoTo() throws IOException {
        String potentialDestination = determineDestination();
        if (gameLayout.containsKey(potentialDestination)){
            if (currentLocation.getPathsTo().contains(potentialDestination)){
                currentLocation.removeEntity(currentPlayer, false);
                Location newLocation = gameLayout.get(potentialDestination);
                newLocation.addCharacterToLocation(currentPlayer);
                currentPlayer.setLocation(newLocation);
                return "\n" + currentPlayer.getName() + " travels to the " +potentialDestination + " from "
                        +currentLocation.getName();
            } else {
                throw new IOException("\n" + currentPlayer.getName() + " can't see a way to " +potentialDestination +
                        " from here - try running look to see available paths");
            }
        } else {
            throw new IOException("\n" + currentPlayer.getName() + " can't see a" +potentialDestination +
                    " nearby - try running look to see available paths");
        }
    }

    String determineDestination() throws IOException {
        String destination = "";
        int destinationCount = 0;

        for (String token : command){
            if (gameLayout.containsKey(token)){
                destination = token;
                destinationCount++;
            }
        }

        if (destinationCount == 1){
            return destination;
        } else if (destinationCount == 0){
            throw new IOException("\n" + currentPlayer.getName() + " isn't sure where you want to go - be more specific");
        } else {
            throw new IOException("\n" + currentPlayer.getName() + " can't be in two places at once - " +
                    "you can only goto one location at a time");
        }
    }

    String processGameAction(String incomingCommand) throws IOException {
        String triggerPhrase = determineActionTrigger();

        if (!triggerPhrase.isEmpty()){
            GameAction validAction = determineAction(possibleActions.get(triggerPhrase), incomingCommand);
            executeConsumedEntities(validAction.getConsumedEntities());
            executeProducedEntities(validAction.getProducedEntities());
            String response = validAction.getNarration();
            if (currentPlayer.getHealth() <= 0){
                respawnPlayer();
                response = response + "\n\n" +currentPlayer.getName()+ " died and lost all of their items \n\n...\n\n...\n\n"
                        +currentPlayer.getName()+ " opens their eyes to find themselves back in the "
                        +startLocation.getName() + "\n ... feeling much lighter - player died and lost all their items";
            }
            return "\n" + response;
        } else {
            throw new IOException("\n" +currentPlayer.getName() + " isn't sure what you mean - try a valid command next time");
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
        if (triggerPhraseCount == 1){
            return triggerPhrase.toLowerCase();
        } else if (triggerPhraseCount == 0) {
            throw new IOException("\n" +currentPlayer.getName() + " isn't sure what you mean - try a valid command next time");
        } else {
            throw new IOException("\n" +currentPlayer.getName() + " can't multi-task - enter one command at a time ");
        }
    }

    GameAction determineAction(HashSet<GameAction> actions, String incomingCommand) throws IOException {
        GameAction validAction = null;
        int validActionCount = 0;

        for (GameAction action : actions){
            if (checkSubjectsInCommand(action, incomingCommand) && checkSubjectsInLocation(action)){
                validAction = action;
                validActionCount++;
            }
        }
        if (validActionCount == 1){
            return validAction;
        } else {
            throw new IOException("\n" + currentPlayer.getName() + " isn't sure what to do - be more specific");
        }
    }

    boolean checkSubjectsInCommand(GameAction action, String incomingCommand){
        int subjectInCommandCount = 0;
        for (String subject : action.getActionSubjects()){
            if (incomingCommand.contains(subject.toLowerCase())){
                subjectInCommandCount++;
            }
        }
        return (subjectInCommandCount >= 1);
    }

    boolean checkSubjectsInLocation(GameAction action){
        Location currentLocation = currentPlayer.getCurrentLocation();
        for (String subject : action.getActionSubjects()){
            if (!((currentLocation.checkEntityPresent(subject)) || (currentPlayer.checkInventoryContains(subject)))){
                return false;
            }
        }
        return true;
    }

    void executeConsumedEntities(ArrayList<String> consumed){
        Location storeRoom = gameLayout.get("storeroom");
        Location currentLocation = currentPlayer.getCurrentLocation();
        for (String consumedEntity : consumed) {
            if (consumedEntity.equalsIgnoreCase("health")){
                currentPlayer.loseHealth();
            } else if (currentLocation.checkEntityPresent(consumedEntity)){
                GameEntity entityToRemove = currentLocation.determineEntityFromName(consumedEntity);
                currentLocation.removeEntity(entityToRemove, true);
                storeRoom.addEntity(entityToRemove);
            } else {
                currentPlayer.removeFromInventory(consumedEntity);
            }
        }
    }

    void executeProducedEntities(ArrayList<String> produced) {
        Location storeRoom = gameLayout.get("storeroom");
        Location currentLocation = currentPlayer.getCurrentLocation();

        for (String producedName : produced){
            if (producedName.equalsIgnoreCase("health")){
                currentPlayer.gainHealth();
            } else if (storeRoom.checkEntityPresent(producedName)){
                GameEntity producedEntity = storeRoom.determineEntityFromName(producedName);
                storeRoom.removeEntity(producedEntity, true);
                currentLocation.addEntity(producedEntity);
            } else if (gameLayout.containsKey(producedName)){
                currentLocation.addPathDestination(producedName);
            }
        }

    }

    void respawnPlayer(){
        for (GameEntity entity : currentPlayer.getInventory().values()){
            currentLocation.addEntity(entity);
        }
        currentPlayer.resetHealth();
        currentPlayer.clearInventory();
        currentPlayer.setLocation(startLocation);
    }
}
