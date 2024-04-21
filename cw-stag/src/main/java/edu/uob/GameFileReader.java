package edu.uob;

import java.util.Arrays;

public class GameFileReader {

    String[] commandKeywords = { "inventory",
            "inv", "get", "drop", "goto",
            "look"
    };

    boolean checkIfKeyword(String token){
        return Arrays.asList(commandKeywords).contains(token);
    }
}
