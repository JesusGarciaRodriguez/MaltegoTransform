package utilities;

import maltego.MaltegoEntity;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class EntityExtractor {

    private static final String CENTRO_REGEX="([A-ZÑ\\s]+) ([\\w\\s/,\\d]+?)(,([\\w\\s/,\\d]+?))? (\\d{5})";
    private static final String NAME_REGEX="([A-ZÑ]+) ([A-ZÑ\\-]+ [A-ZÑ\\-]+)\\s*([A-ZÑ\\-\\s]*)?";
    private static final String MATCHING_RULE="loose";

    public static List<MaltegoEntity> extractEntities(Map<String,String> properties, String mail){
        List<MaltegoEntity> entityList=new LinkedList<>();
        entityList.add(new MaltegoEntity("maltego.Domain","um.es"));
        entityList.add(new MaltegoEntity("jesus.University","Universidad de Murcia"));
        entityList.addAll(extractName(properties));
        entityList.addAll(extractMails(properties,mail));
        entityList.addAll(extractPhoneNumbers(properties));
        entityList.addAll(extractOfficialPost(properties));
        entityList.addAll(extractWebs(properties));
        entityList.addAll(extractWorkplace(properties));
        entityList.addAll(extractExpertiseArea(properties));
        entityList.addAll(extractFiliations(properties));
        entityList.addAll(extractPostalAddress(properties));
        return entityList;
    }

    private static Collection<MaltegoEntity> extractName(Map<String, String> properties) {
        List<MaltegoEntity> entities=new LinkedList<>();
        Optional<String> name=properties.entrySet().stream()
                .filter(e->e.getKey().contains("Nombre"))
                .map(e->e.getValue())
                .findFirst();           //Nombre is always the same, we just take one (could be done in other ways)
        if(name.isPresent()){
            Pattern pat=Pattern.compile(NAME_REGEX);
            Matcher mat=pat.matcher(name.get());
            MaltegoEntity entity=new MaltegoEntity("maltego.Person",capitalizeFully(name.get()));
            if(mat.matches() && mat.group(3).equals("")){
                entity.addProperty("person.firstnames","First names",MATCHING_RULE,capitalize(mat.group(1)));
                entity.addProperty("person.lastname","Surname",MATCHING_RULE,capitalizeFully(mat.group(2)));
            }
            entities.add(entity);
        }
        return entities;
    }


    private static Collection<MaltegoEntity> extractMails(Map<String, String> properties, String mail) {
        List<MaltegoEntity> entities=properties.entrySet().stream()
                .filter(e-> e.getKey().contains("Correo") || e.getKey().contains("Direcciones alternativas"))
                .map(e->e.getValue())
                .filter(e->!e.equals(mail))
                .distinct()
                .map(web-> new MaltegoEntity("maltego.EmailAddress", web))
                .collect(Collectors.toList());
        return entities;
    }

    private static Collection<MaltegoEntity> extractFiliations(Map<String, String> properties) {
        List<MaltegoEntity> entities=new LinkedList<>();
        List<String> filiations=properties.entrySet().stream()
                .filter(e-> e.getKey().contains("Filiacion"))
                .sorted(Comparator.comparing(Map.Entry::getKey)) //Sort by key (equal except number at the end that identifies them)
                .map(e->e.getValue())
                .collect(Collectors.toList());
        List<String> units=properties.entrySet().stream()
                .filter(e-> e.getKey().contains("Unidad Organizativa"))
                .sorted(Comparator.comparing(Map.Entry::getKey)) //Sort by key (equal except number at the end that identifies them)
                .map(e->e.getValue())
                .collect(Collectors.toList());
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

    private static Collection<MaltegoEntity> extractPostalAddress(Map<String, String> properties) {
        List<MaltegoEntity> entities=new LinkedList<>();
        Optional<String> address=properties.entrySet().stream()
                .filter(e->e.getKey().contains("Centro"))
                .map(e->e.getValue())
                .findFirst();           //Direccion postal is always the same, we just take one (could be done in other ways)
        if(address.isPresent())
            entities.add(new MaltegoEntity("jesus.PostalAddress",capitalize(address.get())));
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
                .findFirst();           //Centro is always the same, we just take one (could be done in other ways)
        if(centro.isPresent()){ //It should always be present
            Pattern pat=Pattern.compile(CENTRO_REGEX);
            Matcher mat=pat.matcher(centro.get());
            if(mat.matches()) { //If no match, it is not a faculty (see atica and other PAS personnel, no really relevant info)
                MaltegoEntity entity = new MaltegoEntity("jesus.Faculty", capitalize(mat.group(1)));
                entity.addProperty("faculty","Faculty",MATCHING_RULE,capitalize(mat.group(1)));
                entity.addProperty("country","Country",MATCHING_RULE,"Spain");
                entity.addProperty("city","City",MATCHING_RULE,"Murcia");
                entity.addProperty("location.area","Area",MATCHING_RULE,mat.group(2));
                if(mat.group(3)!=null)
                    entity.addProperty("streetaddress","Street Address",MATCHING_RULE,mat.group(4));
                entity.addProperty("location.areacode","Area Code",MATCHING_RULE,mat.group(5));
                Optional<String> despacho=properties.entrySet().stream()
                        .filter(e->e.getKey().contains("Despacho"))
                        .map(e->e.getValue())
                        .findFirst();            //Despacho is always the same, we just take one (could be done in other ways)
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


    private static String capitalize(String line) {
        return Character.toUpperCase(line.charAt(0)) + line.toLowerCase().substring(1);
    }

    private static String capitalizeFully(String line) {
        StringBuffer output = new StringBuffer();
        Pattern pat=Pattern.compile("\\b[A-Za-zñÑ]+\\b");
        Matcher mat=pat.matcher(line);
        while(mat.find()){
            mat.appendReplacement(output,capitalize(mat.group(0)));
        }
        mat.appendTail(output);
        return output.toString();
    }

}
