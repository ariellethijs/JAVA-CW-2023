package edu.uob;

import java.util.HashMap;

public class GamePlayer extends Character {

    HashMap<String, Artefact> currentInventory;

    GamePlayer(String name, int index, Location location){
        super(name, "Player " + index, location);
        currentInventory = new HashMap<>();
    }

    void addToInventory(Artefact a){ currentInventory.put(a.getName().toLowerCase(), a); }

    void removeFromInventory(Artefact a){ currentInventory.remove(a.getName().toLowerCase()); }

    void setLocation(Location newLocation){ currentLocation = newLocation; }

    HashMap<String, Artefact> getInventory(){ return currentInventory; }
}
