package edu.uob;

import java.util.HashMap;

public class GamePlayer extends Character {

    HashMap<String, Artefact> currentInventory;

    GamePlayer(String name, int index, Location location){
        super(name, "Player " + index, location);
        currentInventory = new HashMap<>();
    }

    void addToInventory(Artefact a){ currentInventory.put(a.getName().toLowerCase(), a); }

    HashMap<String, Artefact> getInventory(){ return currentInventory; }
}
