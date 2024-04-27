package edu.uob;

import java.util.ArrayList;

public class GameAction
{
    private final ArrayList<String> actionSubjects;
    private final ArrayList<String> consumedEntities;
    private final ArrayList<String> producedEntities;
    private final String narration;

    public GameAction(ArrayList<String> subjects, ArrayList<String> consumed, ArrayList<String> produced, String narrationString){
        actionSubjects = subjects;
        consumedEntities = consumed;
        producedEntities = produced;
        narration = narrationString;
    }
    public ArrayList<String> getActionSubjects(){ return actionSubjects; }
    public ArrayList<String> getConsumedEntities(){ return consumedEntities; }
    public ArrayList<String> getProducedEntities(){ return producedEntities; }
    public String getNarration(){ return narration; }
}
