package edu.uob;

import java.io.IOException;
import java.util.*;
public class CommandParser {

    private String command;
    private String[] tokenizedCommand;
    private final HashMap<String, Location> gameLayout;
    private final HashMap<String, HashSet<GameAction>> possibleActions;
    private String playerName;
    private Set<String> allGameEntities;
    private Set<String> commandTriggers;
    private Set<String> commandKeywords;
    private Set<String> inbuiltCommandEntities;
    private Location currentLocation;
    private GamePlayer currentPlayer;
    private Set<GameAction> validActions;
    private final Set<String> restrictedKeywords = Set.of("inv", "inventory", "get", "drop",
            "goto", "look", "health");

    CommandParser(HashMap<String, Location> locations, HashMap<String, HashSet<GameAction>> actions){
        gameLayout = locations;
        possibleActions = actions;
        storeAllGameEntities();
    }

    void storeAllGameEntities(){
        // Store all game entities into a set, used for testing for extraneous entities
        // Add all location names
        allGameEntities = new HashSet<>(gameLayout.keySet());

        // Add all entities within locations
        for (Location location : gameLayout.values()){
            allGameEntities.addAll(location.getLocationArtefacts().keySet());
            allGameEntities.addAll(location.getLocationFurniture().keySet());
            allGameEntities.addAll(location.getLocationCharacters().keySet());
        }
    }

    void setUpForNewCommand(String unprocessedCommand){
        // Store the new command for parsing
        command = unprocessedCommand.toLowerCase();
        tokenizedCommand = command.trim().split("\\s+");
        // Set the storage for different elements of the command
        commandTriggers = new HashSet<>();
        commandKeywords = new HashSet<>();
        inbuiltCommandEntities = new HashSet<>();
    }

    boolean parseCommand(String unprocessedCommand) throws IOException {
        // Reset values for new command parsing
        setUpForNewCommand(unprocessedCommand);
        // Extract player name
        determinePlayerName();
        // Return the result of extracting possible keywords and triggers
        return checkNoMultipleKeywords();
    }

    void determinePlayerName() throws IOException {
        // If no : no player name in command
        if (command.contains(":")){
            // Store everything before the : as the current player name
            playerName = command.substring(0, command.indexOf(':'));
            // Check the player name only contains valid characters
            if (!checkPlayerNameValidity()){
                throw new IOException("Invalid player name - only include uppercase and lowercase letters, spaces, apostrophes and hyphens");
            }
            // Restore the command without the player name
            command = command.substring(command.indexOf(':')+1);
        } else {
            throw new IOException("Not sure whose playing - start with your name next time");
        }
    }

    private boolean checkPlayerNameValidity(){
        // Tokenise the player name into individual chars
        char[] tokenizedPlayerName = playerName.toCharArray();
        for (char c : tokenizedPlayerName){
            // Check each char is of valid type for the player name
            if (!java.lang.Character.isLetter(c) && c != ' ' && c != '\'' && c != '-'){
                return false;
            }
        }
        return true;
    }

    private boolean checkNoMultipleKeywords() throws IOException {
        // Attempt storage of any inbuilt command words
        storeCommandKeywords();
        // Attempt storage of any trigger phrases
        storeCommandTriggers();

        // Determine the response based on the quantity of inbuilt command words/trigger phrases found
        if  ((commandKeywords.size() == 1 && commandTriggers.isEmpty()) || (commandKeywords.isEmpty()) && !commandTriggers.isEmpty()){
            return true;
        } else if (commandKeywords.isEmpty()){
            throw new IOException(playerName + " isn't sure what you mean - try a valid command next time");
        } else {
            throw new IOException(playerName +" can't multi-task - enter one command at a time");
        }
    }

    private void storeCommandKeywords(){
        for (String token : tokenizedCommand){
            // Store any tokens within the command which are inbuilt commands
            if (restrictedKeywords.contains(token)){
                commandKeywords.add(token);
            }
        }
    }

    private void storeCommandTriggers(){
        for (String triggerPhrase : possibleActions.keySet()){
            // Store any tokens within the command which are trigger phrases
            if (triggerPhrase.contains(" ")){
                // If it's a multi-word trigger check for each part of trigger phrase
                if (checkForMultiWordTrigger(triggerPhrase)){
                    commandTriggers.add(triggerPhrase);
                }
            } else if (checkCommandForWord(triggerPhrase)){
                commandTriggers.add(triggerPhrase);
            }
        }
    }

    private boolean checkForMultiWordTrigger(String triggerPhrase){
        // Tokenize the trigger phrase
        String[] tokenizedTrigger = triggerPhrase.trim().split("\\s+");

        // Check if command contains the first word of trigger phrase
        if (checkCommandForWord(tokenizedTrigger[0])){
            // Search from the index of first words of trigger phrase, for all parts of trigger phrase in order
            for (int commandIndex = findCommandIndexOf(tokenizedTrigger[0]), triggerIndex = 0;
                 commandIndex < tokenizedTrigger.length; commandIndex++, triggerIndex++){
                // If any word in the sequence is incorrect, return false
                if (!tokenizedCommand[commandIndex].equalsIgnoreCase(tokenizedTrigger[triggerIndex])){
                    return false;
                }
            }
            // Return true if command contains the trigger phrase in the correct order
            return true;
        }
        return false;
    }

    private boolean checkCommandForWord(String searchToken){
        // My version of .contains() for a String[]
        for (String commandToken : tokenizedCommand){
            if (commandToken.equalsIgnoreCase(searchToken)){
                return true;
            }
        }
        return false;
    }

    private int findCommandIndexOf(String searchToken){
        // My version of .indexOf() for a String[]
        for (int i = 0; i < tokenizedCommand.length; i++){
            if (tokenizedCommand[i].equalsIgnoreCase(searchToken)){
                return i;
            }
        }
        // Should never return this as always checks it contains this first
        return -1;
    }

    public void storeEntityForInbuilt() throws IOException {
        for (String token : tokenizedCommand){
            // If words in command match entities in the game, store them
            if (allGameEntities.contains(token)){ inbuiltCommandEntities.add(token); }
        }

        // Generate error response if the quantity of entities is incorrect
        if (inbuiltCommandEntities.isEmpty()){
            throw new IOException(playerName + " isn't sure what you want to do - be more specific");
        } else if (inbuiltCommandEntities.size() > 1){
            throw new IOException(playerName + " can't multi-task - " +
                    "you can only goto one location at a time, or handle one item at a time");
        }
    }

    public GameAction determineValidAction(Set<String> potentialTriggers) throws IOException {
        // Track the highest number of subjects of an action present in the command
        int highestValidSubjectsCount = 0;

        // Iterate all potential triggers found in command
        for (String potentialTrigger : potentialTriggers) {
            // Iterate all GameActions related to current trigger
            for (GameAction action : possibleActions.get(potentialTrigger)) {
                // Count the number of action subjects in the current command
                int currentActionsValidSubjects = countSubjectsInCommand(action);
                // Compare the number of subjects to the highest recorded number so far
                if (currentActionsValidSubjects >= highestValidSubjectsCount && currentActionsValidSubjects != 0){
                    highestValidSubjectsCount = currentActionsValidSubjects;
                    if (!validActions.contains(action) && checkActionValidity(action)){
                        // If action has not been stored prior and is a valid action, add it to the set of valid actions
                        validActions.add(action);
                    }
                }
            }
        }

        // Generate response based on the number of valid actions found
        if (validActions.size() == 1){
            return validActions.stream().findFirst().orElse(null);
        } else if (validActions.isEmpty()){
            throw new IOException(currentPlayer.getName() + " isn't sure what to do - try entering a valid command next time");
        } else {
            throw new IOException(currentPlayer.getName() + " isn't sure what to do - which open action do you want to perform?");
        }
    }

    public void setUpForActionParsing(Location location, GamePlayer player){
        // Set location and player for testing valid actions
        currentLocation = location;
        currentPlayer = player;
        // Reset storage for valid actions
        validActions = new HashSet<>();
    }

    private boolean checkActionValidity(GameAction action) throws IOException {
        return checkActionSubjectsAvailable(action.getActionSubjects()) && checkNoExtraneousEntities(action)
                && checkConsumedEntitiesAvailable(action.getConsumedEntities());
    }

    private boolean checkActionSubjectsAvailable(Set<String> actionSubjects){
        // Check each subjects availability to the current player, i.e. is the currentLocation,
        // exists in the currentLocation, or is in the player's inv
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
        // Check the availability of consumed entities in currentLocation, inv, as a path, or as health
        // Ensures that non-repeatable actions are not executed twice
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

    public int countSubjectsInCommand(GameAction action){
        int validSubjectsCount = 0;

        // Count the number of action subjects present in the command
        for (String subject : action.getActionSubjects()){
            for (String token : tokenizedCommand) {
                if (token.equalsIgnoreCase(subject)) {
                    validSubjectsCount++;
                }
            }
        }
        return validSubjectsCount;
    }

    public boolean checkNoExtraneousEntities(GameAction action){
        // Compare entities named in command to the set of all game entities, excluding action subjects
        // To determine if any extraneous entities included
        Set <String> actionSubjects = new HashSet<>(action.getActionSubjects());
        for (String commandToken : tokenizedCommand){
            if (allGameEntities.contains(commandToken) && !actionSubjects.contains(commandToken)){
                return false;
            }
        }
        return true;
    }

    public String getPlayerName(){ return playerName; }
    public String getCommandKeyword(){ return commandKeywords.stream().findFirst().orElse(null); }
    public Set<String> getCommandTriggers(){ return commandTriggers; }
    public String getInbuiltCommandEntity(){ return inbuiltCommandEntities.stream().findFirst().orElse(null); }
}
