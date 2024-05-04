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
        commandTriggers = new HashSet<>();
        commandKeywords = new HashSet<>();
        inbuiltCommandEntities = new HashSet<>();
    }

    boolean parseCommand(String unprocessedCommand) throws IOException {
        setUpForNewCommand(unprocessedCommand);
        determinePlayerName();
        tokenizedCommand = command.trim().split("\\s+");

        if (checkNoMultipleKeywords()){
            return true;
        } else {
            throw new IOException(playerName +" can't multi-task - enter one command at a time");
        }
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
        storeOccurrences(restrictedKeywords, "inbuilt");
        storeOccurrences(possibleActions.keySet(), "triggers");

        if  ((commandKeywords.size() == 1 && commandTriggers.isEmpty()) || (commandKeywords.isEmpty()) && !commandTriggers.isEmpty()){
            return true;
        } else if (commandKeywords.isEmpty()){
            throw new IOException(playerName + " isn't sure what you mean - try a valid command next time");
        } else {
            return false;
        }
    }

    private void storeOccurrences(Set<String> keywords, String type){
        for (String token : tokenizedCommand){
            if (keywords.contains(token)){
                if (type.equals("triggers")){ commandTriggers.add(token); }
                if (type.equals("inbuilt")){ commandKeywords.add(token); }
            }
        }
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

    public boolean checkNoExtraneousEntities(GameAction action) throws IOException {
        Set <String> actionSubjects = new HashSet<>(action.getActionSubjects());
        for (String commandToken : tokenizedCommand){
            if (allGameEntities.contains(commandToken) && !actionSubjects.contains(commandToken)){
                // If there is an entity in the command which exists in the game, but is not a subject
                //throw new IOException(playerName + " isn't sure what to do - don't include extraneous objects in action calls");
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
