package edu.uob;

import java.io.IOException;

public class DBInterpreter {
    private String[] commands;
    private int index;
    private DBSession currentSession;

    private Database currentDB;

    public DBInterpreter(String[] commandTokens, DBSession current) {
        this.currentSession = current;
        this.commands = commandTokens;
        this.index = 0;
    }

    public void interpretCommand(int commandStartIndex) throws IOException {
        this.index = commandStartIndex;

        switch (commands[this.index]) {
            case "USE" -> {
                executeUse();
            }
            case "CREATE" -> {
                executeCreate();
            }
            case "DROP" -> {
                executeDrop();
            }
            case "ALTER" -> {
                executeAlter();
            }
            case "INSERT" -> {
                executeInsert();
            }
            case "SELECT" -> {
                executeSelect();
            }
            case "UPDATE" -> {
                executeUpdate();
            }
            case "DELETE" -> {
                executeDelete();
            }
            case "JOIN" -> {
                executeJoin();
            }
            default -> {
                throw new IOException("Attempting to interpret unimplemented command");
            }
        }
    }

    public void executeUse() throws IOException {
        this.index++;
        if (currentSession.dbExists(commands[this.index])){
            currentSession.setCurrentDB(currentSession.getDatabaseByName(commands[this.index]));
        } else {
            throw new IOException("No Database by that name exists");
        }

    }
    public void executeCreate() throws IOException {

    }

    //
//        addAttribute("id", DataType.INTEGER);
//            for (String attributeName : attributeList){
//                 addAttribute(attributeName); //
//        }
//    }

    public void executeDrop(){

    }

    public void executeAlter(){

    }

    public void executeInsert(){

    }

    public void executeSelect(){

    }

    public void executeUpdate(){

    }


    public void executeDelete(){

    }
    public void executeJoin(){

    }

}
