package edu.uob;

import java.util.Arrays;

public class GameFileReader {
    private final String[] commandKeywords = { "inventory",
            "inv", "get", "drop", "goto",
            "look"
    };

    public boolean checkIfKeyword(String token){
        return Arrays.asList(commandKeywords).contains(token);
    }
}
