package edu.uob;

import java.util.ArrayList;
import java.util.HashSet;


public class GameAction
{
    ArrayList<String> actionTriggers;
    ArrayList<String> actionSubjects;
    ArrayList<String> consumedEntities;
    ArrayList<String> producedEntities;
    String narration;

    GameAction(ArrayList<String> triggers, ArrayList<String> subjects, ArrayList<String> consumed, ArrayList<String> produced, String narrationString){
        actionTriggers = triggers;
        actionSubjects = subjects;
        consumedEntities = consumed;
        producedEntities = produced;
        narration = narrationString;
    }

    ArrayList<String> getActionSubjects(){ return actionSubjects; }
    ArrayList<String> getConsumedEntities(){ return consumedEntities; }
    ArrayList<String> getProducedEntities(){ return producedEntities; }
    String getNarration(){ return narration; }
}
