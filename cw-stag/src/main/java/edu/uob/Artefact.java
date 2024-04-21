package edu.uob;

public class Artefact extends GameEntity {
    Location currentLocation;

    Artefact (String name, String description, Location location){
        super(name, description);
        currentLocation = location;
    }
}
