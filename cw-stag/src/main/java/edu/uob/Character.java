package edu.uob;

public class Character extends GameEntity {
    Location currentLocation;

    Character (String name, String description, Location location){
        super(name, description);
        currentLocation = location;

    }
}
