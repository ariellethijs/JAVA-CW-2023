package edu.uob;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class FileDataReader {

    public void accessFile(String filename, String storageFolderPath){
        String filepath = storageFolderPath + File.separator + filename;

        try (BufferedReader reader = new BufferedReader(new FileReader(filepath))){
            String currentLine;
            while ((currentLine = reader.readLine()) != null) {
                System.out.println(currentLine);
            }
        } catch (IOException e){
            System.err.println("ERROR while reading file: " + e.getMessage());
        }
    }
}
