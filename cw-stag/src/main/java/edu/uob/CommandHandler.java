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
        // Create a command parser for the game setup
        commandParser = new CommandParser(layout, actions);
        gameLayout = layout;
        possibleActions = actions;
        // Determine the start location from it's name
        startLocation = gameLayout.get(firstLocation);
        // Create storage for all players
        allPlayers = new HashMap<>();
        playerIndex = 0;
    }

    public String handleCommand(String incomingCommand) throws IOException {
        // Pass the command to the command parser
        if (commandParser.parseCommand(incomingCommand)){
            // Determine current player and their location based on the parser's determination of player name
            currentPlayer = determineCommandPlayer(commandParser.getPlayerName());
            currentLocation = currentPlayer.getCurrentLocation();

            // If the command parser found a single valid inbuilt command
            if (commandParser.getCommandKeyword() != null){
                // Return the inbuilt command handling
                return handleBuiltInCommand(commandParser.getCommandKeyword());
            // If the parser found some possible action triggers
            } else if (!commandParser.getCommandTriggers().isEmpty()){
                // Return the resulting action handling
                return processGameAction(commandParser.getCommandTriggers());
            } else {
            // Else a composite command was attempted
                throw new IOException(currentPlayer.getName() +" can't multi-task - enter one command at a time");
            }
        } else {
            // Else no valid command was attempted
            throw new IOException("Not sure what you mean - try a valid command next time");
        }
    }

    public String handleBuiltInCommand(String keyword) throws IOException {
        // Switch the inbuilt command for its relevant method
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
        // Check for inventory contents
        if (!currentPlayer.getInventory().isEmpty()){
            // Start building the response
            StringBuilder response = new StringBuilder("\n[" + currentPlayer.getName().toUpperCase() + "'S INVENTORY]\n");
            for (GameEntity entity : currentPlayer.getInventory().values()){
                // Add each entity
                response.append("      "); // Indentation formatting because I like how it looks
                response = appendEntityDescription(response, entity);
            }
            return String.valueOf(response);
        } else {
            // Else return an empty inventory message
            return currentPlayer.getName() + "'s inventory is empty";
        }
    }

    private String processLook(){
        // Start building the response with the locations name and description
        StringBuilder response = new StringBuilder("\n[" + currentLocation.getName().toUpperCase() + "] " + currentLocation.getDescription() + "\n");

        // Add all location artefacts, furniture, and characters to response
        response = iterateThroughLocationEntities(response, currentLocation.getLocationArtefacts());
        response = iterateThroughLocationEntities(response, currentLocation.getLocationFurniture());
        response = iterateThroughLocationEntities(response, currentLocation.getLocationCharacters());

        // If there are any paths out of current location
        if (!currentLocation.getPathsTo().isEmpty()){
            response.append("\n[PATHS]\n");
        }
        for (String path : currentLocation.getPathsTo()){
            // Append a description of each path
            response.append("     ").append("[").append(currentLocation.getName().toUpperCase()).append("] --> [").append(path.toUpperCase()).append("]\n");
        }
        return String.valueOf(response);
    }

    private String processGetOrDrop(String commandType) throws IOException {
        // Determine whether there is one corresponding entity for the command
        commandParser.storeEntityForInbuilt();
        // If so retrieve it
        String relevantEntityName = commandParser.getInbuiltCommandEntity();
        if (commandType.equals("get")){
            // If it's a get command, attempt item pick up
            return pickUpItem(relevantEntityName);
        } else if (currentPlayer.getInventory().containsKey(relevantEntityName)){
            // Else it's a drop command, so drop the item
            GameEntity artefactToMove = currentPlayer.getInventory().get(relevantEntityName);
            currentLocation.addArtefactToLocation((Artefact)artefactToMove);
            currentPlayer.removeFromInventory(artefactToMove.getName());
            return currentPlayer.getName() + " dropped the " +relevantEntityName;
        } else {
            // Else it's a drop command, but the player doesn't have the corresponding entity in their inv
            throw new IOException(currentPlayer.getName()+ " can't see a " +relevantEntityName +
                    " in their inventory - try running inventory or inv to see contents");
        }
    }

    private String pickUpItem(String itemKey) throws IOException {
        // Check not attempting a furniture pick up
        if (currentLocation.getLocationFurniture().containsKey(itemKey)){
            throw new IOException(currentPlayer.getName() + " tries to pick up the " +itemKey +
                    " but it seems to be fixed in place - players cannot pick up items of furniture");
        // Check not attempting a character pick up
        } else if (currentLocation.getLocationCharacters().containsKey(itemKey)){
            throw new IOException(currentPlayer.getName() + " tries to pick up the " +itemKey +
                    " but they don't seem too happy about it - players cannot pick other characters");
        // Check if item is an artefact present in current location
        } else if (currentLocation.getLocationArtefacts().containsKey(itemKey)){
            GameEntity artefactToMove = currentLocation.getLocationArtefacts().get(itemKey);
            currentLocation.removeEntity(artefactToMove, false);
            currentPlayer.addToInventory(artefactToMove);
            return currentPlayer.getName() + " picked up the " +itemKey;
        } else {
        // Else entity is in a different location, so cannot be picked up
            throw new IOException(currentPlayer.getName()+ " can't see a " +itemKey +
                    " anywhere - try running look to see objects in the area");
        }
    }

    private String processGoTo() throws IOException {
        // Determine whether there is one corresponding location for the goto
        commandParser.storeEntityForInbuilt();
        // If so retrieve it from the parser
        String potentialDestination = commandParser.getInbuiltCommandEntity();
        if (gameLayout.containsKey(potentialDestination)){
            // If the entity refers to a location
            if (currentLocation.getPathsTo().contains(potentialDestination)){
                // If there is a path to that location, move the player there
                currentLocation.removeEntity(currentPlayer, false);
                Location newLocation = gameLayout.get(potentialDestination);
                newLocation.addCharacterToLocation(currentPlayer);
                currentPlayer.setLocation(newLocation);
                return currentPlayer.getName() + " travels to the " +potentialDestination + " from "
                        +currentLocation.getName();
            } else {
                // Else there's no path there
                throw new IOException(currentPlayer.getName() + " can't see a way to " +potentialDestination +
                        " from here - try running look to see available paths");
            }
        } else {
            // Else the entity does not refer to a location
            throw new IOException(currentPlayer.getName() + " can't see a" +potentialDestination +
                    " nearby - try running look to see available paths");
        }
    }

    private String processGameAction(Set<String> potentialTriggers) throws IOException {
        // Check there is a single valid action, and retrieve it if so
        commandParser.setUpForActionParsing(currentLocation, currentPlayer);
        GameAction validAction = commandParser.determineValidAction(potentialTriggers);

        // Consume and produce entities to execute action
        executeConsumedEntities(validAction.getConsumedEntities());
        executeProducedEntities(validAction.getProducedEntities());

        // Start generating a response
        String response = validAction.getNarration();

        // Check if action resulted in player death
        if (currentPlayer.getHealth() <= 0){
            // Respawn player if necessary and append death message to response
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
            // If health is consumed, remove player health
            if (consumedEntity.equalsIgnoreCase("health")){
                currentPlayer.loseHealth();
            // Else if it is a placed entity, remove from its location and move it to store room
            } else if (checkEntityPlacedInGame(consumedEntity)){
                Location entityLocation = determineEntityLocation(consumedEntity);
                GameEntity entityToRemove = entityLocation.determineEntityFromName(consumedEntity);
                entityLocation.removeEntity(entityToRemove, true);
                storeRoom.addEntity(entityToRemove);
            // Else if its in current player's inv, remove and move it to store room
            } else if (currentPlayer.checkInventoryContains(consumedEntity)){
                GameEntity entityToRemove = currentPlayer.getItemFromPlayerInv(consumedEntity);
                currentPlayer.removeFromInventory(consumedEntity);
                storeRoom.addEntity(entityToRemove);
            // Else if it's a location name, remove the path to it from the current location
            } else if (gameLayout.containsKey(consumedEntity)){
                currentLocation.removePath(consumedEntity);
            }
        }
    }

    private void executeProducedEntities(Set<String> produced) throws IOException {
        for (String producedName : produced){
            // If health is produced, add player health
            if (producedName.equalsIgnoreCase("health")){
                currentPlayer.gainHealth();
            // Else if it is a placed entity, remove from its location and move it to current location
            } else if (checkEntityPlacedInGame(producedName)){
                Location entityLocation = determineEntityLocation(producedName);
                GameEntity producedEntity = entityLocation.determineEntityFromName(producedName);
                entityLocation.removeEntity(producedEntity, true);
                currentLocation.addEntity(producedEntity);
            // Else if it's a location name, add a path to it from the current location
            } else if (gameLayout.containsKey(producedName)){
                currentLocation.addPathDestination(producedName);
            }
        }
    }

    private void respawnPlayer(){
        // Drop all players items to current location
        for (GameEntity entity : currentPlayer.getInventory().values()){
            currentLocation.addEntity(entity);
        }
        currentPlayer.resetHealth();
        currentPlayer.clearInventory();
        // Move player to start location
        currentPlayer.setLocation(startLocation);
    }

                 /*  HELPER FUNCTIONS  */

    private GamePlayer determineCommandPlayer(String name){
        String nameAsKey = name.toLowerCase();
        if (allPlayers.containsKey(nameAsKey)){
            // If it is a pre-existing player, return that player
            return allPlayers.get(nameAsKey);
        } else {
            // Create a new player with that name and add them to the start location
            playerIndex++;
            GamePlayer newPlayer = new GamePlayer(name, playerIndex, startLocation);
            allPlayers.put(nameAsKey, newPlayer);
            startLocation.addCharacterToLocation(newPlayer);
            return newPlayer;
        }
    }

    private StringBuilder iterateThroughLocationEntities(StringBuilder response, HashMap<String, GameEntity> entityMap){
        // Iterate through all entities of a certain type for look command response generation
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
        // Add a single entity's name and description to the response
        return response.append("[").append(entity.getName().toUpperCase()).append("] ").append(entity.getDescription()).append("\n");
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
