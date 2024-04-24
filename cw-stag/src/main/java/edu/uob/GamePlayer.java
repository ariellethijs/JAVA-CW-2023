package edu.uob;

import java.util.HashMap;

public class GamePlayer extends Character {

    HashMap<String, GameEntity> currentInventory;

    GamePlayer(String name, int index, Location location){
        super(name, "Player " + index, location);
        currentInventory = new HashMap<>();
    }

    void addToInventory(GameEntity entity){ currentInventory.put(entity.getName().toLowerCase(), entity); }

    void removeFromInventory(GameEntity entity){ currentInventory.remove(entity.getName().toLowerCase()); }

    void setLocation(Location newLocation){ currentLocation = newLocation; }

    HashMap<String, GameEntity> getInventory(){ return currentInventory; }
}
