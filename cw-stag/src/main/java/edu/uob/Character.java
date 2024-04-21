package edu.uob;

import java.util.ArrayList;

public class Character extends GameEntity {
    Location currentLocation;
    ArrayList<Artefact> currentInventory;

    Character (String name, String description, Location location){
        super(name, description);
        currentLocation = location;
        currentInventory = new ArrayList<>();
    }

    Location getCurrentLocation(){ return currentLocation; }
}
