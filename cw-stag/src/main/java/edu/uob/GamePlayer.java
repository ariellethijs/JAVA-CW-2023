package edu.uob;

import java.util.HashMap;

public class GamePlayer extends Character {

    HashMap<String, GameEntity> currentInventory;

    int startHealth = 3;
    int health;

    GamePlayer(String name, int index, Location location){
        super(name, "Player " + index, location);
        currentInventory = new HashMap<>();
        health = startHealth;
    }

    void addToInventory(GameEntity entity){ currentInventory.put(entity.getName().toLowerCase(), entity); }

    void removeFromInventory(String entityName){ currentInventory.remove(entityName); }

    void setLocation(Location newLocation){ currentLocation = newLocation; }

    HashMap<String, GameEntity> getInventory(){ return currentInventory; }

    boolean checkInventoryContains(String artefactName){ return (currentInventory.containsKey(artefactName.toLowerCase())); }

    int getHealth(){ return health; }

    void resetHealth(){ health = startHealth; }

    void loseHealth(){
        if (health >= 1){
            health--;
        }
    }

    void gainHealth(){
        if (health <= (startHealth-1)){
            health++; }
    }

    void clearInventory(){ currentInventory.clear(); }

}
