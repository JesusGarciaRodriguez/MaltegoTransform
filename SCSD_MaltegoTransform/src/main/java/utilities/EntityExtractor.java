package utilities;

import maltego.MaltegoEntity;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class EntityExtractor {

    public static List<MaltegoEntity> extractEntities(Map<String,String> properties){
        List<MaltegoEntity> entityList=new LinkedList<>();
        entityList.add(new MaltegoEntity("maltego.Domain","um.es"));
        for(String key:properties.keySet()){
            entityList.add(new MaltegoEntity("maltego.Alias", properties.get(key)));
        }
        return entityList;
    }
}
