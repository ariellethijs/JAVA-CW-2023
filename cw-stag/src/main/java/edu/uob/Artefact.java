package edu.uob;

public class Artefact extends GameEntity {
    Location currentLocation;
    Character currentHolder;
    Artefact (String name, String description, Location location){
        super(name, description);
        currentLocation = location;
        currentHolder = null;
    }
}
