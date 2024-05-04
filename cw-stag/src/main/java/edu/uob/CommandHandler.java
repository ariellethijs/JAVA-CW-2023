package edu.uob;

import java.io.IOException;
import java.util.*;

public class CommandHandler {
    CommandParser commandParser;
    private final HashMap<String, Location> gameLayout;
    private final HashMap<String, HashSet<GameAction>> possibleActions;
    private GamePlayer currentPlayer;
    private Location currentLocation;
    private final Location startLocation;
    private final HashMap<String, GamePlayer> allPlayers;
    private int playerIndex;

    public CommandHandler(HashMap<String, Location> layout, HashMap<String, HashSet<GameAction>> actions, String firstLocation){
        commandParser = new CommandParser(layout, actions);
        gameLayout = layout;
        possibleActions = actions;
        startLocation = gameLayout.get(firstLocation);
        allPlayers = new HashMap<>();
        playerIndex = 0;
    }

    public String handleCommand(String incomingCommand) throws IOException {
        if (commandParser.parseCommand(incomingCommand)) {
            currentPlayer = determineCommandPlayer(commandParser.getPlayerName());
            currentLocation = currentPlayer.getCurrentLocation();

            if (commandParser.getCommandKeyword() != null){
                return handleBuiltInCommand(commandParser.getCommandKeyword());
            } else if (!commandParser.getCommandTriggers().isEmpty()){
                return processGameAction(commandParser.getCommandTriggers());
            } else {
                throw new IOException(currentPlayer.getName() +" can't multi-task - enter one command at a time");
            }
        } else {
            throw new IOException("Not sure what you mean - try a valid command next time");
        }
    }

    public String handleBuiltInCommand(String keyword) throws IOException {
        return switch (keyword) {
            case "inventory", "inv" -> processInventory();
            case "look" -> processLook();
            case "get", "drop" -> processGetOrDrop(keyword);
            case "goto" -> processGoTo();
            case "health" -> currentPlayer.getName() + "'s health: " +currentPlayer.getHealth();
            default -> throw new IOException(currentPlayer.getName() + " isn't sure what you mean - try a valid command next time");
        };
    }

    private String processInventory(){
        if (!currentPlayer.getInventory().isEmpty()){
            StringBuilder response = new StringBuilder("\n[" + currentPlayer.getName().toUpperCase() + "'S INVENTORY]\n");
            for (GameEntity entity : currentPlayer.getInventory().values()){
                response.append("      ");
                response = appendEntityDescription(response, entity);
            }
            return String.valueOf(response);
        } else {
            return currentPlayer.getName() + "'s inventory is empty";
        }
    }

    private String processLook(){
        StringBuilder response = new StringBuilder("\n[" + currentLocation.getName().toUpperCase() + "] " + currentLocation.getDescription() + "\n");

        response = iterateThroughLocationEntities(response, currentLocation.getLocationArtefacts());
        response = iterateThroughLocationEntities(response, currentLocation.getLocationFurniture());
        response = iterateThroughLocationEntities(response, currentLocation.getLocationCharacters());

        if (!currentLocation.getPathsTo().isEmpty()){
            response.append("\n[PATHS]\n");
        }
        for (String path : currentLocation.getPathsTo()){
            response.append("     ").append("[").append(currentLocation.getName().toUpperCase()).append("] --> [").append(path.toUpperCase()).append("]\n");
        }
        return String.valueOf(response);
    }

    private String processGetOrDrop(String commandType) throws IOException {
        commandParser.storeEntityForInbuilt();
        String relevantEntityName = commandParser.getInbuiltCommandEntity();
        if (commandType.equals("get")){
            return pickUpItem(relevantEntityName);
        } else if (currentPlayer.getInventory().containsKey(relevantEntityName)){
            GameEntity artefactToMove = currentPlayer.getInventory().get(relevantEntityName);
            currentLocation.addArtefactToLocation((Artefact)artefactToMove);
            currentPlayer.removeFromInventory(artefactToMove.getName());
            return currentPlayer.getName() + " dropped the " +relevantEntityName;
        } else {
            throw new IOException(currentPlayer.getName()+ " can't see a " +relevantEntityName +
                    " in their inventory - try running inventory or inv to see contents");
        }
    }

    private String pickUpItem(String itemKey) throws IOException {
        if (currentLocation.getLocationFurniture().containsKey(itemKey)){
            throw new IOException(currentPlayer.getName() + " tries to pick up the " +itemKey +
                    " but it seems to be fixed in place - players cannot pick up items of furniture");
        } else if (currentLocation.getLocationCharacters().containsKey(itemKey)){
            throw new IOException(currentPlayer.getName() + " tries to pick up the " +itemKey +
                    " but they don't seem too happy about it - players cannot pick other characters");
        } else if (currentLocation.getLocationArtefacts().containsKey(itemKey)){
            GameEntity artefactToMove = currentLocation.getLocationArtefacts().get(itemKey);
            currentLocation.removeEntity(artefactToMove, false);
            currentPlayer.addToInventory(artefactToMove);
            return currentPlayer.getName() + " picked up the " +itemKey;
        } else {
            throw new IOException(currentPlayer.getName()+ " can't see a " +itemKey +
                    " anywhere - try running look to see objects in the area");
        }
    }

    private String processGoTo() throws IOException {
        commandParser.storeEntityForInbuilt();
        String potentialDestination = commandParser.getInbuiltCommandEntity();
        if (gameLayout.containsKey(potentialDestination)){
            if (currentLocation.getPathsTo().contains(potentialDestination)){
                currentLocation.removeEntity(currentPlayer, false);
                Location newLocation = gameLayout.get(potentialDestination);
                newLocation.addCharacterToLocation(currentPlayer);
                currentPlayer.setLocation(newLocation);
                return currentPlayer.getName() + " travels to the " +potentialDestination + " from "
                        +currentLocation.getName();
            } else {
                throw new IOException(currentPlayer.getName() + " can't see a way to " +potentialDestination +
                        " from here - try running look to see available paths");
            }
        } else {
            throw new IOException(currentPlayer.getName() + " can't see a" +potentialDestination +
                    " nearby - try running look to see available paths");
        }
    }

    private String processGameAction(Set<String> potentialTriggers) throws IOException {
        GameAction validAction = determineAction(potentialTriggers);
        executeConsumedEntities(validAction.getConsumedEntities());
        executeProducedEntities(validAction.getProducedEntities());
        String response = validAction.getNarration();

        if (currentPlayer.getHealth() <= 0){
            respawnPlayer();
            response = response + "\n" +currentPlayer.getName()+ " died and lost all of their items \n\n...\n\n"
                        +currentPlayer.getName()+ " opens their eyes to find themselves back in the "
                        +startLocation.getName() + "\n ... feeling much lighter - player died and lost all their items";
        }
        return response;
    }

    private void executeConsumedEntities(Set<String> consumed) throws IOException {
        Location storeRoom = gameLayout.get("storeroom");
        for (String consumedEntity : consumed) {
            if (consumedEntity.equalsIgnoreCase("health")){
                currentPlayer.loseHealth();
            } else if (checkEntityPlacedInGame(consumedEntity)){
                Location entityLocation = determineEntityLocation(consumedEntity);
                GameEntity entityToRemove = entityLocation.determineEntityFromName(consumedEntity);
                entityLocation.removeEntity(entityToRemove, true);
                storeRoom.addEntity(entityToRemove);
            } else if (currentPlayer.checkInventoryContains(consumedEntity)){
                currentPlayer.removeFromInventory(consumedEntity);
            } else if (gameLayout.containsKey(consumedEntity)){
                currentLocation.removePath(consumedEntity);
            }
        }
    }

    private void executeProducedEntities(Set<String> produced) throws IOException {
        for (String producedName : produced){
            if (producedName.equalsIgnoreCase("health")){
                currentPlayer.gainHealth();
            } else if (checkEntityPlacedInGame(producedName)){
                Location entityLocation = determineEntityLocation(producedName);
                GameEntity producedEntity = entityLocation.determineEntityFromName(producedName);
                entityLocation.removeEntity(producedEntity, true);
                currentLocation.addEntity(producedEntity);
            } else if (gameLayout.containsKey(producedName)){
                currentLocation.addPathDestination(producedName);
            }
        }
    }

    private void respawnPlayer(){
        for (GameEntity entity : currentPlayer.getInventory().values()){
            currentLocation.addEntity(entity);
        }
        currentPlayer.resetHealth();
        currentPlayer.clearInventory();
        currentPlayer.setLocation(startLocation);
    }

                 /*  HELPER FUNCTIONS  */

    private GamePlayer determineCommandPlayer(String name){
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
    }

    private StringBuilder iterateThroughLocationEntities(StringBuilder response, HashMap<String, GameEntity> entityMap){
        String indentation = "     ";
        for (GameEntity entity : entityMap.values()){
            if (!entity.getName().equals(currentPlayer.getName())){ // Skip over the current player when looking
                response.append(indentation);
                response = appendEntityDescription(response, entity);
            }
        }
        return response;
    }

    private StringBuilder appendEntityDescription(StringBuilder response, GameEntity entity){
        return response.append("[").append(entity.getName().toUpperCase()).append("] ").append(entity.getDescription()).append("\n");
    }

    private GameAction determineAction(Set<String> potentialTriggers) throws IOException {
        GameAction validAction = null;
        int validActionCount = 0;
        int highestValidSubjectsCount = 0;

        for (String potentialTrigger : potentialTriggers) {
            for (GameAction action : possibleActions.get(potentialTrigger)) {
                int currentActionsValidSubjects = commandParser.countSubjectsInCommand(action);
                if (currentActionsValidSubjects >= highestValidSubjectsCount && currentActionsValidSubjects != 0){
                    highestValidSubjectsCount = currentActionsValidSubjects;
                    if (validAction != action && checkActionValidity(action)){
                        validAction = action;
                        validActionCount++;
                    }
                }
            }
        }

        if (validActionCount == 1){
            return validAction;
        } else if (validActionCount == 0){
            throw new IOException(currentPlayer.getName() + " isn't sure what to do - try entering a valid command next time");
        } else {
            throw new IOException(currentPlayer.getName() + " isn't sure what to do - which open action do you want to perform?");
        }
    }

    boolean checkActionValidity(GameAction action) throws IOException {
        return checkActionSubjectsAvailable(action.getActionSubjects())
                && commandParser.checkNoExtraneousEntities(action) && checkConsumedEntitiesAvailable(action.getConsumedEntities());
    }

    private boolean checkActionSubjectsAvailable(Set<String> actionSubjects){
        for (String subject : actionSubjects){
            if (!subject.isEmpty()){
                if (!(currentLocation.checkEntityPresent(subject) || currentPlayer.checkInventoryContains(subject) ||
                        subject.equalsIgnoreCase(currentLocation.getName()))){
                    return false;
                }
            }
        }
        return true;
    }

    private boolean checkConsumedEntitiesAvailable(Set<String> consumedEntities) throws IOException {
        for (String consumedEntity : consumedEntities){
            if (!consumedEntity.isEmpty()){
                if (!(currentLocation.checkEntityPresent(consumedEntity) || currentPlayer.checkInventoryContains(consumedEntity)
                        || currentLocation.checkIfPathTo(consumedEntity) || consumedEntity.equalsIgnoreCase("health"))){
                    throw new IOException(currentPlayer.getName() + " cannot repeat actions which have already had permanent consequences");
                }
            }
        }
        return true;

    }

    private boolean checkEntityPlacedInGame(String entityName){
        for (Location location: gameLayout.values()){
            if (location.checkEntityPresent(entityName)){
                return true;
            }
        }
        return false;
    }

    private Location determineEntityLocation(String entityName) throws IOException {
        for (Location location : gameLayout.values()){
            if (location.checkEntityPresent(entityName)){
                return location;
            }
        }
        throw new IOException("Couldn't determine entity from location although already check for existence?"); // For debug !!
    }
}
