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

public class XMLFileReader {
    ArrayList<GameAction> allGameActions;

    XMLFileReader(File actionsFile) throws ParserConfigurationException, IOException, SAXException {
        allGameActions = new ArrayList<>();
        openAndReadActionsFile(actionsFile);
    }

    void openAndReadActionsFile(File actionsFile) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document document = builder.parse(actionsFile);
        document.getDocumentElement().normalize();

        NodeList actions = document.getElementsByTagName("action");

        for (int i = 0; i < actions.getLength(); i++){
            Element action = (Element)actions.item(i);
            allGameActions.add(storeActionContents(action));
        }
    }

    GameAction storeActionContents(Element action){
        ArrayList<String> triggers = storeSubElementValues(action,"triggers");
        ArrayList<String> subjects = storeSubElementValues(action, "subjects");
        String consumedEntityName = storeSubElementValues(action, "consumed").get(0);
        String producedEntityName = storeSubElementValues(action, "produced").get(0);
        String narration = storeSubElementValues(action,"narration").get(0);
        return new GameAction(triggers, subjects, consumedEntityName, producedEntityName, narration);
    }

    ArrayList<String> storeSubElementValues(Element action, String tagName){
        ArrayList<String> subElementKeyPhrases = new ArrayList<>();

        Node subElement = action.getElementsByTagName(tagName).item(0);
        NodeList childNodes = subElement.getChildNodes();

        for (int i = 0; i < childNodes.getLength(); i++){
            Node childNode = childNodes.item(i);
            String keyPhraseAsString = childNode.getTextContent().trim();
            if (!keyPhraseAsString.isEmpty()){
                subElementKeyPhrases.add(keyPhraseAsString);
            } else if (childNodes.getLength() == 1){
                subElementKeyPhrases.add("");
            }
        }
        return subElementKeyPhrases;
    }

    ArrayList<GameAction> getAllGameActions(){ return allGameActions; }

}
