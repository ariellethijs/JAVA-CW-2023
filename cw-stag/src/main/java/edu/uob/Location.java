package edu.uob;

import java.util.ArrayList;
import java.util.HashMap;

public class Location extends GameEntity {

    HashMap<String, Artefact> locationArtefacts;
    HashMap<String, Furniture> locationFurniture;
    HashMap<String, Character> locationCharacters;
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

    HashMap<String,Artefact> getLocationArtefacts(){ return locationArtefacts; }

    HashMap<String, Furniture> getLocationFurniture(){ return locationFurniture; }

    HashMap<String, Character> getLocationCharacters(){ return locationCharacters; }

    ArrayList<String> getPathsTo(){ return pathsTo; }

    void printContents(){
        System.out.println("Location name = " +getName());
        System.out.println("Description = " +getDescription());

        System.out.println();

        System.out.println("Location Artefacts:");
        int cnt = 0;
        for (Artefact a : locationArtefacts.values()){
            System.out.println("["+cnt+"] = " +a.getName() + " " +a.getDescription());
            cnt++;
        }

        System.out.println();

        System.out.println("Location Furniture:");
        cnt = 0;
        for (Furniture f : locationFurniture.values()){
            System.out.println("["+cnt+"] = " +f.getName() + " " +f.getDescription());
            cnt++;
        }

        System.out.println();

        System.out.println("Location Characters:");
        cnt = 0;
        for (Character c : locationCharacters.values()){
            System.out.println("["+cnt+"] = " +c.getName() + " " +c.getDescription());
            cnt++;
        }

        System.out.println();

        System.out.println("Paths to:");
        for (String path : pathsTo){
            System.out.println(path);
        }

        System.out.println("\n");


    }
}
