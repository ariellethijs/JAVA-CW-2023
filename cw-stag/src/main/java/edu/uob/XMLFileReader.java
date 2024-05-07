package edu.uob;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class XMLFileReader extends GameFileReader {
    private final HashMap<String, HashSet<GameAction>> allGameActions;

    public XMLFileReader(File actionsFile) throws ParserConfigurationException, IOException, SAXException {
        allGameActions = new HashMap<>();
        openAndReadActionsFile(actionsFile);
    }

    public void openAndReadActionsFile(File actionsFile) throws ParserConfigurationException, IOException, SAXException {
        // Parse the action file
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document document = builder.parse(actionsFile);
        // Remove all empty nodes
        document.getDocumentElement().normalize();

        // Retrieve all actions nodes
        NodeList actions = document.getElementsByTagName("action");

        // Iterate each action node
        for (int i = 0; i < actions.getLength(); i++){
            Element action = (Element)actions.item(i);
            // Extract the action triggers
            ArrayList<String> triggers = storeSubElementValues(action,"triggers", false);
            // Store all elements of the action in a GameAction object
            GameAction currentAction = storeActionContents(action);
            for (String trigger : triggers){
                // Iterate each trigger word and add the same Action object to each of its triggers
                trigger = trigger.toLowerCase();
                if (allGameActions.containsKey(trigger)){
                    // If the trigger already has some associated actions, add the current action to its set
                    HashSet<GameAction> actionSet = allGameActions.get(trigger);
                    actionSet.add(currentAction);
                } else if (!checkIfKeyword(trigger)){
                    // Else generate a new corresponding hashset for the trigger word and add the current action there
                    HashSet<GameAction> newActionSet = new HashSet<>();
                    newActionSet.add(currentAction);
                    allGameActions.put(trigger, newActionSet);
                }
            }
        }
    }

    private GameAction storeActionContents(Element action){
        // Store each sub element of the action
        ArrayList<String> subjects = storeSubElementValues(action, "subjects", false);
        ArrayList<String> consumedEntities = storeSubElementValues(action, "consumed", true);
        ArrayList<String> producedEntities = storeSubElementValues(action, "produced", true);
        String narration = storeSubElementValues(action,"narration", false).get(0);
        // Create GameAction object with the stored parameters
        return new GameAction(subjects, consumedEntities, producedEntities, narration);
    }

    private ArrayList<String> storeSubElementValues(Element action, String tagName, boolean optional) {
        // Create storage for extracted elements
        ArrayList<String> subElementKeyPhrases = new ArrayList<>();

        // Extract the subElement nodes
        Node subElement = action.getElementsByTagName(tagName).item(0);
        NodeList childNodes = subElement.getChildNodes();

        // Iterate each node
        for (int i = 0; i < childNodes.getLength(); i++){
            Node childNode = childNodes.item(i);
            String nodeText;
            if (!tagName.equals("narration")){
                // Store the text as a lowercase entity key
                nodeText = childNode.getTextContent().trim().toLowerCase();
            } else {
                // Store the narration text as is
                nodeText = childNode.getTextContent().trim();
            }
            if (!nodeText.isEmpty() && !checkIfKeyword(nodeText)){
                // If node is not empty and does not correspond to a inbuilt command word, store it
                subElementKeyPhrases.add(nodeText);
            } else if (childNodes.getLength() == 1 && nodeText.isEmpty() && optional){
                // If the node is empty, and it's an optional value add a placeholder
                subElementKeyPhrases.add("");
            }
        }
        return subElementKeyPhrases;
    }
    public HashMap<String, HashSet<GameAction>> getAllGameActions(){ return allGameActions; }
}
