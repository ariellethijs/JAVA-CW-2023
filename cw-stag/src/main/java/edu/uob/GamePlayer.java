package edu.uob;

import java.util.HashMap;

public class GamePlayer extends Character {

    private Location currentLocation;
    private final HashMap<String, GameEntity> currentInventory;
    private final int startHealth;
    private int health;

    public GamePlayer(String name, int index, Location location){
        super(name, "Player " + index);
        currentInventory = new HashMap<>();
        startHealth = 3;
        health = startHealth;
        currentLocation = location;
    }
    public Location getCurrentLocation(){ return currentLocation; }
    public void setLocation(Location newLocation){ currentLocation = newLocation; }
    public HashMap<String, GameEntity> getInventory(){ return currentInventory; }
    public void addToInventory(GameEntity entity){ currentInventory.put(entity.getName().toLowerCase(), entity); }
    public void removeFromInventory(String entityName){ currentInventory.remove(entityName); }
    public boolean checkInventoryContains(String artefactName){ return (currentInventory.containsKey(artefactName.toLowerCase())); }
    public int getHealth(){ return health; }
    public void resetHealth(){ health = startHealth; }
    public void loseHealth(){
        if (health >= 1){
            health--;
        }
    }
    public void gainHealth(){
        if (health <= (startHealth-1)){
            health++; }
    }
    public void clearInventory(){ currentInventory.clear(); }
}
