/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package maltego;

import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 *
 * @author miragshin
 */
public class MaltegoTransform {
    
    public final  String BOOKMARK_COLOR_NONE = "-1";
    public final  String BOOKMARK_COLOR_BLUE = "0";
    public final  String BOOKMARK_COLOR_GREEN = "1";
    public final  String BOOKMARK_COLOR_YELLOW = "2";
    public final  String BOOKMARK_COLOR_ORANGE = "3";
    public final  String BOOKMARK_COLOR_RED = "4";

    
    private List<MaltegoEntity> listEntities = new ArrayList<>();
    private List<MaltegoUiMessage> listUImsgs = new ArrayList<>();


        public void addEntity(MaltegoEntity entity){
            this.listEntities.add(entity);
        }

    public void addUiMessage(MaltegoUiMessage uiMsg){
        this.listUImsgs.add(uiMsg);
    }

    public void addUiMessage(String type, String msg){
        this.listUImsgs.add(new MaltegoUiMessage(type,msg));
    }
    
        public void addEntity(String type, String value) {
            MaltegoEntity Entity = new MaltegoEntity(type,value);
            this.listEntities.add(Entity);
        }

        public void throwException(String message){
            try {
                DocumentBuilderFactory documentFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder documentBuilder = documentFactory.newDocumentBuilder();
                Document xmlDocument = documentBuilder.newDocument();
                Element maltegoMessage = xmlDocument.createElement("MaltegoMessage");
                xmlDocument.appendChild(maltegoMessage);
                Element maltegoExceptionMessage = xmlDocument.createElement("MaltegoTransformExceptionMessage");
                maltegoMessage.appendChild(maltegoExceptionMessage);
                Element maltegoExceptions = xmlDocument.createElement("Exceptions");
                maltegoExceptionMessage.appendChild(maltegoExceptions);
                Element maltegoException=xmlDocument.createElement("Exception");
                maltegoException.appendChild(xmlDocument.createTextNode(message));
                maltegoExceptions.appendChild(maltegoException);

                TransformerFactory transformerFactory = TransformerFactory.newInstance();
                Transformer transformer = transformerFactory.newTransformer();

                DOMSource source = new DOMSource(xmlDocument);
                StreamResult result = new StreamResult(System.out);

                transformer.transform(source, result);
            }catch (Exception e){
                e.printStackTrace();
            }
        }

        public void returnOutput() {
            try {

                DocumentBuilderFactory documentFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder documentBuilder = documentFactory.newDocumentBuilder();

                Document xmlDocument = documentBuilder.newDocument();
                Element maltegoMessage = xmlDocument.createElement("MaltegoMessage");
                xmlDocument.appendChild(maltegoMessage);

                Element maltegoResponse = xmlDocument.createElement("MaltegoTransformResponseMessage");
                maltegoMessage.appendChild(maltegoResponse);

                Element uiMessages = xmlDocument.createElement("UIMessages");
                maltegoResponse.appendChild(uiMessages);
                for(int i = 0; i < listUImsgs.size(); i++){
                    Element uiMsg=xmlDocument.createElement("UIMessage");
                    uiMsg.setAttribute("MessageType",listUImsgs.get(i).getType());
                    uiMsg.appendChild(xmlDocument.createTextNode(listUImsgs.get(i).getMessage()));
                    uiMessages.appendChild(uiMsg);
                }

                Element Entities = xmlDocument.createElement("Entities");
                maltegoResponse.appendChild(Entities);

                for (int i = 0; i < listEntities.size(); i++) {

                    Element entity = xmlDocument.createElement("Entity");
                    entity.setAttribute("Type", listEntities.get(i).entityType);
                    Entities.appendChild(entity);

                    Element value = xmlDocument.createElement("Value");
                    value.appendChild(xmlDocument.createTextNode(listEntities.get(i).entityValue));
                    entity.appendChild(value);

                    Element weight = xmlDocument.createElement("Weight");
                    weight.appendChild(xmlDocument.createTextNode(listEntities.get(i).weight));
                    entity.appendChild(weight);

                    if (listEntities.get(i).additionalFields.size() > 0) {
                        Element additionalFields = xmlDocument.createElement("AdditionalFields");
                        for (int z = 0; z < listEntities.get(i).additionalFields.size(); z++) {
                            Element field = xmlDocument.createElement("Field");
                            field.setAttribute("Name", listEntities.get(i).additionalFields.get(z).get(0));
                            field.setAttribute("DisplayName", listEntities.get(i).additionalFields.get(z).get(1));
                            field.setAttribute("MatchingRule", listEntities.get(i).additionalFields.get(z).get(2));
                            field.appendChild(xmlDocument.createTextNode(listEntities.get(i).additionalFields.get(z).get(3)));
                            additionalFields.appendChild(field);
                        }

                        entity.appendChild(additionalFields);

                    }
                    if (listEntities.get(i).iconURL.length() > 0) {
                        Element iconUrl = xmlDocument.createElement("IconURL");
                        iconUrl.appendChild(xmlDocument.createTextNode(listEntities.get(i).iconURL));
                        Entities.appendChild(iconUrl);
                    }
                }

                TransformerFactory transformerFactory = TransformerFactory.newInstance();
                Transformer transformer = transformerFactory.newTransformer();

                DOMSource source = new DOMSource(xmlDocument);
                StreamResult result = new StreamResult(System.out);

                transformer.transform(source, result);
            } catch (ParserConfigurationException pce) {
                pce.printStackTrace();
            } catch (TransformerException tfe) {
                tfe.printStackTrace();
            }

        }
}
