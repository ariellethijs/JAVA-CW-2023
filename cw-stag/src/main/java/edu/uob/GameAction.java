package edu.uob;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class GameAction
{
    private final Set<String> actionSubjects;
    private final Set<String> consumedEntities;
    private final Set<String> producedEntities;
    private final String narration;

    public GameAction(ArrayList<String> subjects, ArrayList<String> consumed, ArrayList<String> produced, String narrationString){
        actionSubjects = new HashSet<>(subjects);
        consumedEntities = new HashSet<>(consumed);
        producedEntities = new HashSet<>(produced);
        narration = narrationString;
    }
    public Set<String> getActionSubjects(){ return actionSubjects; }
    public Set<String> getConsumedEntities(){ return consumedEntities; }
    public Set<String> getProducedEntities(){ return producedEntities; }
    public String getNarration(){ return narration; }
}
