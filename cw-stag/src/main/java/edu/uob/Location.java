package edu.uob;

import java.util.ArrayList;
import java.util.HashMap;

public class Location extends GameEntity {

    HashMap<String, GameEntity> locationArtefacts;
    HashMap<String, GameEntity> locationFurniture;
    HashMap<String, GameEntity> locationCharacters;
    ArrayList<String> pathsTo;
    Location (String name, String description){
        super(name, description);
        pathsTo = new ArrayList<>();
        locationArtefacts = new HashMap<>();
        locationFurniture = new HashMap<>();
        locationCharacters = new HashMap<>();
    }

    void addArtefactToLocation(Artefact a){ locationArtefacts.put(a.getName().toLowerCase(), a); }

    void addFurnitureToLocation(Furniture f){ locationFurniture.put(f.getName().toLowerCase(), f); }

    void addCharacterToLocation(Character c){ locationCharacters.put(c.getName().toLowerCase(), c); }

    void addPathDestination(String toLocation){ pathsTo.add(toLocation); }

    void removeEntity(GameEntity entity){
        if (entity instanceof Artefact){
            locationArtefacts.remove(entity.getName().toLowerCase());
        } else if (entity instanceof Character){
            locationCharacters.remove(entity.getName().toLowerCase());
        }
    }

    HashMap<String,GameEntity> getLocationArtefacts(){ return locationArtefacts; }

    HashMap<String, GameEntity> getLocationFurniture(){ return locationFurniture; }

    HashMap<String, GameEntity> getLocationCharacters(){ return locationCharacters; }

    ArrayList<String> getPathsTo(){ return pathsTo; }
}
