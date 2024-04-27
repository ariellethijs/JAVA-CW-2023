package edu.uob;

import java.util.ArrayList;
import java.util.HashMap;

public class Location extends GameEntity {
    private final HashMap<String, GameEntity> locationArtefacts;
    private final HashMap<String, GameEntity> locationFurniture;
    private final HashMap<String, GameEntity> locationCharacters;
    private final ArrayList<String> pathsTo;

    public Location (String name, String description){
        super(name, description);
        pathsTo = new ArrayList<>();
        locationArtefacts = new HashMap<>();
        locationFurniture = new HashMap<>();
        locationCharacters = new HashMap<>();
    }
    public void addArtefactToLocation(Artefact a){ locationArtefacts.put(a.getName().toLowerCase(), a); }
    public void addFurnitureToLocation(Furniture f){ locationFurniture.put(f.getName().toLowerCase(), f); }
    public void addCharacterToLocation(Character c){ locationCharacters.put(c.getName().toLowerCase(), c); }
    public void addEntity(GameEntity entity){
        if (entity instanceof Artefact){
            addArtefactToLocation((Artefact) entity);
        } else if (entity instanceof Character){
            addCharacterToLocation((Character) entity);
        } else if (entity instanceof Furniture){
            addFurnitureToLocation((Furniture) entity);
        }
    }
    public void removeEntity(GameEntity entity, boolean fromAction){
        if (entity instanceof Artefact){
            locationArtefacts.remove(entity.getName().toLowerCase());
        } else if (entity instanceof Character){
            locationCharacters.remove(entity.getName().toLowerCase());
        } else if (entity instanceof Furniture && fromAction){
            locationFurniture.remove(entity.getName().toLowerCase());
        }
    }
    public HashMap<String,GameEntity> getLocationArtefacts(){ return locationArtefacts; }
    public HashMap<String, GameEntity> getLocationFurniture(){ return locationFurniture; }
    public HashMap<String, GameEntity> getLocationCharacters(){ return locationCharacters; }
    public void addPathDestination(String toLocation){ pathsTo.add(toLocation); }
    public void removePath(String toLocation){ pathsTo.remove(toLocation); }
    public ArrayList<String> getPathsTo(){ return pathsTo; }
    public boolean checkEntityPresent(String entityName){
        return (locationArtefacts.containsKey(entityName.toLowerCase())) ||
                (locationFurniture.containsKey(entityName.toLowerCase())) ||
                (locationCharacters.containsKey(entityName.toLowerCase()));
    }
    public GameEntity determineEntityFromName(String entityName){
        if (locationArtefacts.containsKey(entityName)){
            return locationArtefacts.get(entityName);
        } else if (locationCharacters.containsKey(entityName)){
            return locationCharacters.get(entityName);
        } else {
            return locationFurniture.get(entityName);
        }
    }
}
