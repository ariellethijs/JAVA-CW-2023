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
    private final Set<String> restrictedKeywords = Set.of("inv", "inventory", "get", "drop",
            "goto", "look", "health");

    CommandParser(HashMap<String, Location> locations, HashMap<String, HashSet<GameAction>> actions){
        gameLayout = locations;
        possibleActions = actions;
        storeAllGameEntities();
    }

    void storeAllGameEntities(){
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
        command = unprocessedCommand.toLowerCase();
        tokenizedCommand = command.trim().split("\\s+");
        commandTriggers = new HashSet<>();
        commandKeywords = new HashSet<>();
        inbuiltCommandEntities = new HashSet<>();
    }

    boolean parseCommand(String unprocessedCommand) throws IOException {
        setUpForNewCommand(unprocessedCommand);
        determinePlayerName();
        return checkNoMultipleKeywords();
    }

    void determinePlayerName() throws IOException {
        if (command.contains(":")){
            playerName = command.substring(0, command.indexOf(':'));
            command = command.substring(command.indexOf(':')+1);
        } else {
            throw new IOException("Not sure whose playing - start with your name next time");
        }
    }

    private boolean checkNoMultipleKeywords() throws IOException {
        storeCommandKeywords();
        storeCommandTriggers();

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
            if (restrictedKeywords.contains(token)){
                commandKeywords.add(token);
            }
        }
    }

    private void storeCommandTriggers(){
        for (String triggerPhrase : possibleActions.keySet()){
            if (triggerPhrase.contains(" ")){
                if (checkForMultiWordTrigger(triggerPhrase)){
                    commandTriggers.add(triggerPhrase);
                }
            } else if (checkCommandForWord(triggerPhrase)){
                commandTriggers.add(triggerPhrase);
            }
        }
    }

    private boolean checkForMultiWordTrigger(String triggerPhrase){
        String[] tokenizedTrigger = triggerPhrase.trim().split("\\s+");
        if (checkCommandForWord(tokenizedTrigger[0])){
            for (int commandIndex = findCommandIndexOf(tokenizedTrigger[0]), triggerIndex = 0;
                 commandIndex < tokenizedTrigger.length; commandIndex++, triggerIndex++){
                if (!tokenizedCommand[commandIndex].equalsIgnoreCase(tokenizedTrigger[triggerIndex])){
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    private boolean checkCommandForWord(String searchToken){
        for (String commandToken : tokenizedCommand){
            if (commandToken.equalsIgnoreCase(searchToken)){
                return true;
            }
        }
        return false;
    }

    private int findCommandIndexOf(String searchToken){
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
            if (allGameEntities.contains(token)){ inbuiltCommandEntities.add(token); }
        }

        if (inbuiltCommandEntities.isEmpty()){
            throw new IOException(playerName + " isn't sure what you want to do - be more specific");
        } else if (inbuiltCommandEntities.size() > 1){
            throw new IOException(playerName + " can't multi-task - " +
                    "you can only goto one location at a time, or handle one item at a time");
        }
    }

    public int countSubjectsInCommand(GameAction action){
        int validSubjectsCount = 0;

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
