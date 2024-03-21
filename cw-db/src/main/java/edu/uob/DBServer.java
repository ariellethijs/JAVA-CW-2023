package edu.uob;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.util.ArrayList;

public class DBServer {
    private static final char END_OF_TRANSMISSION = 4;
    private String storageFolderPath;

    private DBSession currentSession;


    public static void main(String[] args) throws IOException {
        DBServer server = new DBServer();
        server.blockingListenOn(8888);
    }

    /**
    * KEEP this signature otherwise we won't be able to mark your submission correctly.
    */
    public DBServer() {
        storageFolderPath = Paths.get("databases").toAbsolutePath().toString();
        try {
            // Create the database storage folder if it doesn't already exist !
            Files.createDirectories(Paths.get(storageFolderPath));
        } catch(IOException ioe) {
            System.out.println("Can't seem to create database storage folder " + storageFolderPath);
        }

        try {
            this.currentSession = new DBSession(getStorageFolderPath());
        } catch (IOException fileException){
            System.out.println("[ERROR] " + fileException.getMessage());
        }
    }

    /**
    * KEEP this signature (i.e. {@code edu.uob.DBServer.handleCommand(String)}) otherwise we won't be
    * able to mark your submission correctly.
    *
    * <p>This method handles all incoming DB commands and carries out the required actions.
    */
    public String handleCommand(String command) {

        DBTokeniser tokeniser = new DBTokeniser();
        ArrayList<String> tokens = tokeniser.tokeniseInput(command);
        String[] commands = tokens.toArray(new String[0]);

        DBParser parser = new DBParser(commands);
        DBInterpreter interpreter = new DBInterpreter(commands, this.currentSession);

        try {
            parser.parseAllTokens();
            ArrayList<Integer> validCommandsStartIndexes = parser.getValidCommandStartingIndexes();

            for (Integer startIndex : validCommandsStartIndexes){
                 interpreter.interpretCommand(startIndex);
            }
        } catch (IOException e) {
            return ("[ERROR] " + e.getMessage());
        }

        if (interpreter.responseRequired){
            String responseTableAsString = convertResponseTableToString(interpreter.responseTable);
            return "[OK]" + "\n" + responseTableAsString;
        } else {
            return "[OK]";
        }
    }

    public String convertResponseTableToString(ArrayList<ArrayList<String>> responseTable){
        StringBuilder stringBuilder = new StringBuilder();

        for (ArrayList<String> row : responseTable){
            for (String value : row){
                stringBuilder.append(value).append("\t");
            }
            stringBuilder.append("\n");
        }
        return stringBuilder.toString();
    }

    //  === Methods below handle networking aspects of the project - you will not need to change these ! ===

    public void blockingListenOn(int portNumber) throws IOException {
        try (ServerSocket s = new ServerSocket(portNumber)) {
            System.out.println("Server listening on port " + portNumber);
            while (!Thread.interrupted()) {
                try {
                    blockingHandleConnection(s);
                } catch (IOException e) {
                    System.err.println("Server encountered a non-fatal IO error:");
                    e.printStackTrace();
                    System.err.println("Continuing...");
                }
            }
        }
    }

    private void blockingHandleConnection(ServerSocket serverSocket) throws IOException {
        try (Socket s = serverSocket.accept();
        BufferedReader reader = new BufferedReader(new InputStreamReader(s.getInputStream()));
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()))) {

            System.out.println("Connection established: " + serverSocket.getInetAddress());
            while (!Thread.interrupted()) {
                String incomingCommand = reader.readLine();
                System.out.println("Received message: " + incomingCommand);
                String result = handleCommand(incomingCommand);
                writer.write(result);
                writer.write("\n" + END_OF_TRANSMISSION + "\n");
                writer.flush();
            }
        }
    }

    public String getStorageFolderPath(){
        return storageFolderPath;
    }

}
