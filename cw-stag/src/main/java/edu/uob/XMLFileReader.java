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
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document document = builder.parse(actionsFile);
        document.getDocumentElement().normalize();

        NodeList actions = document.getElementsByTagName("action");

        for (int i = 0; i < actions.getLength(); i++){
            Element action = (Element)actions.item(i);
            ArrayList<String> triggers = storeSubElementValues(action,"triggers", false);
            GameAction currentAction = storeActionContents(action);
            for (String trigger : triggers){
                trigger = trigger.toLowerCase();
                if (allGameActions.containsKey(trigger)){
                    HashSet<GameAction> actionSet = allGameActions.get(trigger);
                    actionSet.add(currentAction);
                } else if (!checkIfKeyword(trigger)){
                    HashSet<GameAction> newActionSet = new HashSet<>();
                    newActionSet.add(currentAction);
                    allGameActions.put(trigger, newActionSet);
                }
            }
        }
    }
    private GameAction storeActionContents(Element action) throws IOException {
        ArrayList<String> subjects = storeSubElementValues(action, "subjects", false);
        ArrayList<String> consumedEntities = storeSubElementValues(action, "consumed", true);
        ArrayList<String> producedEntities = storeSubElementValues(action, "produced", true);
        String narration = storeSubElementValues(action,"narration", false).get(0);
        return new GameAction(subjects, consumedEntities, producedEntities, narration);
    }
    private ArrayList<String> storeSubElementValues(Element action, String tagName, boolean optional) {
        ArrayList<String> subElementKeyPhrases = new ArrayList<>();

        Node subElement = action.getElementsByTagName(tagName).item(0);
        NodeList childNodes = subElement.getChildNodes();

        for (int i = 0; i < childNodes.getLength(); i++){
            Node childNode = childNodes.item(i);
            String nodeText = childNode.getTextContent().trim().toLowerCase();
            if (!nodeText.isEmpty() && !checkIfKeyword(nodeText)){
                subElementKeyPhrases.add(nodeText);
            } else if (childNodes.getLength() == 1 && nodeText.isEmpty() && optional){
                subElementKeyPhrases.add(""); // If the node is empty and it's an optional value add a placeholder
            }
        }
        return subElementKeyPhrases;
    }
    public HashMap<String, HashSet<GameAction>> getAllGameActions(){ return allGameActions; }

}
