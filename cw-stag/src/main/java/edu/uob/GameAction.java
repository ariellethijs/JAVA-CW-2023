package edu.uob;

import java.util.ArrayList;

public class GameAction
{
    ArrayList<String> actionTriggers;
    ArrayList<String> actionSubjects;

    String consumedEntity;
    String producedEntity;
    String narration;

    GameAction(ArrayList<String> triggers, ArrayList<String> subjects, String consumed, String produced, String narrationString){
        actionTriggers = triggers;
        actionSubjects = subjects;
        consumedEntity = consumed;
        producedEntity = produced;
        narration = narrationString;
    }

    void printContents(){
        System.out.println("Triggers: ");
        for (String t : actionTriggers){
            System.out.println(t);
        }

        System.out.println("Subjects: ");
        for (String s : actionSubjects){
            System.out.println(s);
        }

        System.out.println("Consumed = " +consumedEntity);
        System.out.println("Produced = " +producedEntity);
        System.out.println("Narration = " +narration);

    }
}
