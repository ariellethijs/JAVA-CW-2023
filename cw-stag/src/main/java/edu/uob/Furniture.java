package edu.uob;

public class Furniture extends GameEntity {
    final Location location;
    Furniture (String name, String description, Location fixedLocation){
        super(name, description);
        location = fixedLocation;
    }
}
