package utilities;

import maltego.MaltegoEntity;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class EntityExtractor {

    private static final String CENTRO_REGEX="([A-Z\\s]+) ([\\w\\s/,\\d]+?) (\\d{5})";
    private static final String MATCHING_RULE="loose";

    public static List<MaltegoEntity> extractEntities(Map<String,String> properties, String mail){
        List<MaltegoEntity> entityList=new LinkedList<>();
        entityList.add(new MaltegoEntity("maltego.Domain","um.es"));
        entityList.add(new MaltegoEntity("jesus.University","Universidad de Murcia"));
        //for(String key:properties.keySet()){
          //  entityList.add(new MaltegoEntity("maltego.Alias", properties.get(key)));
       // }
        entityList.addAll(extractMails(properties,mail));
        entityList.addAll(extractPhoneNumbers(properties));
        entityList.addAll(extractOfficialPost(properties));
        entityList.addAll(extractWebs(properties));
        entityList.addAll(extractWorkplace(properties));
        entityList.addAll(extractExpertiseArea(properties));
        entityList.addAll(extractFiliations(properties));
        return entityList;
    }

    private static Collection<MaltegoEntity> extractMails(Map<String, String> properties, String mail) {
        List<MaltegoEntity> entities=properties.entrySet().stream()
                .filter(e-> e.getKey().contains("Correo") || e.getKey().contains("Direcciones alternativas"))
                .map(e->e.getValue())
                .filter(e->!e.equals(mail))
                .distinct()
                .map(web-> new MaltegoEntity("maltego.Website", web))
                .collect(Collectors.toList());
        return entities;
    }

    private static Collection<MaltegoEntity> extractFiliations(Map<String, String> properties) {
        List<MaltegoEntity> entities=new LinkedList<>();
        List<String> filiations=properties.entrySet().stream()
                .filter(e-> e.getKey().contains("Filiacion"))
                .sorted(Comparator.comparing(Map.Entry::getKey)) //Sort by key (que son iguales excepto un numero al final en orden)
                .map(e->e.getValue())
                .collect(Collectors.toList());
        List<String> units=properties.entrySet().stream()
                .filter(e-> e.getKey().contains("Unidad Organizativa"))
                .sorted(Comparator.comparing(Map.Entry::getKey)) //Sort by key (que son iguales excepto un numero al final en orden)
                .map(e->e.getValue())
                .collect(Collectors.toList());
        //System.err.println(filiations.size());
        //System.err.println(units.size());
        if(filiations.size()==units.size()){ //If they are not equal something wrong happened
            for(int i=0;i<filiations.size();i++){
                MaltegoEntity entity=new MaltegoEntity("jesus.Filiation",capitalize(filiations.get(i)));
                entity.addProperty("unit","Organizational unit",MATCHING_RULE,capitalize(units.get(i)));
                entity.addProperty("organization","Organization",MATCHING_RULE,"Universidad de Murcia");
                entities.add(entity);
            }
        }
        return entities;
    }

    private static Collection<MaltegoEntity> extractExpertiseArea(Map<String, String> properties) {
        List<MaltegoEntity> entities=properties.entrySet().stream()
                .filter(e-> e.getKey().contains("Area de Conocimiento"))
                .map(e->e.getValue())
                .distinct()
                .map(area-> new MaltegoEntity("jesus.ExpertiseArea", capitalize(area)))
                .collect(Collectors.toList());
        return entities;
    }

    private static Collection<MaltegoEntity> extractWorkplace(Map<String, String> properties) {
        List<MaltegoEntity> entities=new LinkedList<>();
        Optional<String> centro=properties.entrySet().stream()
                .filter(e->e.getKey().contains("Centro"))
                .map(e->e.getValue())
                .findFirst();           //Centro es siempre el mismo, por eso simplemente el primero (podría hacerse de otra forma)
        if(centro.isPresent()){
            Pattern pat=Pattern.compile(CENTRO_REGEX);
            Matcher mat=pat.matcher(centro.get());
            if(mat.matches()) { //Si no matchea no ha sabido extraer centro y no devuelve nada
                MaltegoEntity entity = new MaltegoEntity("jesus.Faculty", capitalize(mat.group(1)));
                entity.addProperty("faculty","Faculty",MATCHING_RULE,capitalize(mat.group(1)));
                entity.addProperty("country","Country",MATCHING_RULE,"Spain");
                entity.addProperty("city","City",MATCHING_RULE,"Murcia");
                entity.addProperty("location.area","Area",MATCHING_RULE,mat.group(2));
                entity.addProperty("location.areacode","Area Code",MATCHING_RULE,mat.group(3));
                Optional<String> despacho=properties.entrySet().stream()
                        .filter(e->e.getKey().contains("Despacho"))
                        .map(e->e.getValue())
                        .findFirst();            //Despacho es siempre el mismo, por eso simplemente el primero (podría hacerse de otra forma)
                if(despacho.isPresent()){
                    MaltegoEntity entityOffice=new MaltegoEntity("jesus.Office","pavo");
                    entityOffice.addProperty("officecode","OfficeCode",MATCHING_RULE,despacho.get());
                    entityOffice.addProperty("faculty","OfficeCode",MATCHING_RULE,capitalize(mat.group(1)));
                    entities.add(entityOffice);
                }
                entities.add(entity);
            }
        }
        return entities;
    }

    private static Collection<MaltegoEntity> extractWebs(Map<String, String> properties) {
        List<MaltegoEntity> entities=properties.entrySet().stream()
                .filter(e-> e.getKey().contains("Web personal") || e.getKey().contains("Curriculum academico"))
                .map(e->e.getValue())
                .distinct()
                .map(web-> new MaltegoEntity("maltego.Website", web))
                .collect(Collectors.toList());
        return entities;
    }

    private static Collection<MaltegoEntity> extractOfficialPost(Map<String, String> properties) {
        List<MaltegoEntity> entities=properties.entrySet().stream()
                .filter(e-> e.getKey().contains("Puesto"))
                .map(e->e.getValue())
                .distinct()
                .map(post-> new MaltegoEntity("jesus.OfficialPost", capitalize(post)))
                .collect(Collectors.toList());
        return entities;
    }

    private static Collection<MaltegoEntity> extractPhoneNumbers(Map<String, String> properties) {
        List<MaltegoEntity> entities=properties.entrySet().stream()
                .filter(e-> e.getKey().contains("Telefono"))
                .map(e->e.getValue())
                .distinct()
                .map(tlfn-> new MaltegoEntity("maltego.PhoneNumber",tlfn))
                .collect(Collectors.toList());
        return entities;
    }


    private static String capitalize(String post) {
        return Character.toUpperCase(post.charAt(0)) + post.toLowerCase().substring(1);
    }
}
