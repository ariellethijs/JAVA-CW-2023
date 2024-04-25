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

    void addEntity(GameEntity entity){
        if (entity instanceof Artefact){
            addArtefactToLocation((Artefact) entity);
        } else if (entity instanceof Character){
            addCharacterToLocation((Character) entity);
        } else if (entity instanceof Furniture){
            addFurnitureToLocation((Furniture) entity);
        }
    }

    void removeEntity(GameEntity entity, boolean fromAction){
        if (entity instanceof Artefact){
            locationArtefacts.remove(entity.getName().toLowerCase());
        } else if (entity instanceof Character){
            locationCharacters.remove(entity.getName().toLowerCase());
        } else if (entity instanceof Furniture && fromAction){
            locationFurniture.remove(entity.getName().toLowerCase());
        }
    }

    HashMap<String,GameEntity> getLocationArtefacts(){ return locationArtefacts; }

    HashMap<String, GameEntity> getLocationFurniture(){ return locationFurniture; }

    HashMap<String, GameEntity> getLocationCharacters(){ return locationCharacters; }

    ArrayList<String> getPathsTo(){ return pathsTo; }

    boolean checkEntityPresent(String entityName){
        return (locationArtefacts.containsKey(entityName.toLowerCase())) ||
                (locationFurniture.containsKey(entityName.toLowerCase())) ||
                (locationCharacters.containsKey(entityName.toLowerCase()));
    }

    GameEntity determineEntityFromName(String entityName){
        if (locationArtefacts.containsKey(entityName)){
            return locationArtefacts.get(entityName);
        } else if (locationCharacters.containsKey(entityName)){
            return locationCharacters.get(entityName);
        } else {
            return locationFurniture.get(entityName);
        }
    }
}
