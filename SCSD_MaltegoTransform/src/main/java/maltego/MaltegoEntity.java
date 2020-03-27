/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package maltego;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author miragshin
 */
public class MaltegoEntity {
    //TODO Codigo mas bonito?

    List<List<String>> additionalFields = new ArrayList<>();
    String weight = "100";
    String iconURL = "";
    String entityType;
    String entityValue;

        public MaltegoEntity(String entityType,String entityValue){
            this.entityType=entityType;
            this.entityValue=entityValue;
        }

        public void addProperty(String fieldName, String displayName, String matchingRule, String value) {

            List<String> list = new ArrayList<String>();
            list.add(fieldName);
            list.add(displayName);
            list.add(matchingRule);
            list.add(value);
            this.additionalFields.add(list);

        }


        public void setWeight(String weight) {
            this.weight = weight;
        }

        public void setLinkColor(String color) {
            this.addProperty("link#main.java.maltego.link.color", "LinkColor", "", color);
        }

        public void setLinkStyle(String style) {
            this.addProperty("link#main.java.maltego.link.color", "LinkColor", "", style);
        }

        public void setLinkThichkness(String thick) {
            this.addProperty("link#main.java.maltego.link.thickness", "Thickness", "", thick);

        }

        public void setLinkLabel(String Label) {
            this.addProperty("link#main.java.maltego.link.label", "Label", "", Label);
        }

        public void setBookmark(String bookmark) {
            this.addProperty("bookmark#", "Bookmark", "strict", bookmark);
        }

        public void setNote(String Note) {
            this.addProperty("notes#", "Note", "", Note);
        }

        public void setIconUrl(String url) {
            this.iconURL = url;
        }
    }