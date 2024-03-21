package edu.uob;

import java.util.ArrayList;
import java.util.Arrays;

public class Tokeniser {
    String query;
    String[] specialCharacters = {"(",")",",",";",};
    // "==" | ">" | "<" | ">=" | "<=" | "!="
    String[] comparators = {">=","<=","!=", "=="};
    ArrayList<String> tokens = new ArrayList<>();

    ArrayList<String> tokeniseInput(String inputQuery)
    {
        tokens = new ArrayList<>();
        this.query = inputQuery;
        // Remove any whitespace at the beginning and end of the query
        this.query = this.query.trim();
        // Split the query on single quotes (to separate out query characters from string literals)
        String[] fragments = this.query.split("'");
        for (int i=0; i<fragments.length; i++) {
            // Every odd fragment is a string literal, so just append it without any alterations
            if (i%2 != 0) tokens.add("'" + fragments[i] + "'");
                // If it's not a string literal, it must be query characters (which need further processing)
            else {
                // Tokenise the fragments into an array of strings
                String[] nextBatchOfTokens = tokenise(fragments[i]);
                // Then add these to the "result" array list (needs a bit of conversion)
                tokens.addAll(Arrays.asList(nextBatchOfTokens));
            }
        }
        return tokens;
    }

    String[] tokenise(String input)
    {
        input = input.replaceAll("\\s+", " ");
        input = input.trim();
        // Add in some extra padding spaces around the "special characters"
        // so we can be sure that they are separated by AT LEAST one space (possibly more)
        for (String specialCharacter : specialCharacters) {
            input = input.replace(specialCharacter, " " + specialCharacter + " ");
        }

        for (String comparator : comparators){
            input = input.replace(comparator, " " + comparator + " ");
        }

        // Handle difference between tokenising "<" & ">" and "<=" & ">="
        input = input.replaceAll("(?<=\\S)([<>])(?=\\S)", " $1 ");
        // Remove all double spaces (the previous replacements may have added some)
        // This is "blind" replacement - replacing if they exist, doing nothing if they don't
        while (input.contains("  ")){
            input = input.replaceAll("  ", " ");
        }
        // Again, remove any whitespace from the beginning and end that might have been introduced
        input = input.trim();
        // Finally split on the space char (since there will now ALWAYS be a space between tokens)
        return input.split(" ");
    }
}
