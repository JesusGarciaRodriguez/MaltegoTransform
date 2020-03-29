package utilities;

import exceptions.DifferentMailException;
import exceptions.ParsingException;
import org.apache.commons.lang3.StringUtils;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.URL;
import java.text.Normalizer;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DirectoryParser {

    private static final String URL="https://www.um.es/atica/directorio/";

    private static final String NUM_RESULT ="Encontradas (\\s*|<strong>)[0-9]+(\\s*|</strong>) entradas";
    private static final String TABLA_MULTIPLE_REGEX ="Personas.*?<table.*?>";
    private static final String HREF_REGEX="<\\s*a\\s+href=['\"]([?a-zA-Z0-9@:%._+~=&]+)['\"].*?>";

    private static final String TABLA_DATOS_NAME="infoElem";
    private static final String TABLA_DATOS_REGEX=TABLA_DATOS_NAME+"[^>]*>(.*?)</table>"; //TODO, Ojo, si hay table en medio no vale (parece que basta en single person)

    private static final String ENTRADA_TABLA="<tr>(.*?)</tr>"; //TODO, Ojo, si hay tr en medio no vale (en single person esto basta)

    private static final String TAG="<.*?>";
    private static final String DATA_REGEX="(.+?):\\s*(.+)";
    private static final String MAIL_FUNCTION_REGEX="correo\\(['\"](.*?)['\"],['\"](.*?)['\"],.*?\\)";
    private static final String TLFN_REGEX="(\\+34 )?\\d{9}";
    private static final String ACUTE_REGEX="&([a-zA-Z])acute;";
    //TODO tal vez todos los Pattern compile aqui?


    public static Map<String, String> getInfo(String page, String mail) {
        Map<String,String> information=new HashMap<>();
        Pattern pat = Pattern.compile(NUM_RESULT);
        Matcher mat = pat.matcher(page);
        try {
            if(mat.find())
                information=getInfoMultipleResults(page,mail);
            else
                information= getInfoSingleResult(page,mail);
        } catch (ParsingException | DifferentMailException e) {
            System.err.println("No info");
            //TODO Message?, Different treatment?
        }
        return information;
    }

    private static Map<String, String> getInfoMultipleResults(String page, String mail) throws ParsingException {
        Map<String,String> properties=new HashMap<>();
        Pattern pat = Pattern.compile(TABLA_MULTIPLE_REGEX);
        Matcher mat = pat.matcher(page);
        if(!mat.find())
            throw new ParsingException("Mail not found in directory");
        String table=getField("table",page.substring(mat.start()));
        //System.err.println(table);
        String tableEntry=getField("tr",table);
        Pattern auxPat=Pattern.compile("<table.*?>");
        Matcher auxMat=auxPat.matcher(table);
        auxMat.find();
        String parsingTable=table.substring(auxMat.end()); //All except <table> to correctly extract and remove fields
        //System.err.println(table);
        //System.err.println(parsingTable);
        boolean found=false; //Si una de las entradas es de la persona buscada, las demás no
        while (!tableEntry.equals("") && !found){
            try {
                properties=getInfoPerson(tableEntry,mail);
                found=true;
            } catch (DifferentMailException | ParsingException | IOException e) {
                //TODO Maybe do something
            }
            parsingTable=parsingTable.substring(tableEntry.length()); //Eliminate parsed entry
            //System.err.println(parsingTable);
            tableEntry=getField("tr",parsingTable);
        }
        return properties;
    }

    private static Map<String, String> getInfoPerson(String tableEntry,String mail) throws ParsingException, DifferentMailException, IOException {
        Map<String,String> properties=new HashMap<>();
        Pattern pat=Pattern.compile(HREF_REGEX);
        Matcher mat=pat.matcher(tableEntry);
        int count=0;
        while(mat.find()){
            //System.err.println(mat.group(1));
            Map<String,String> someProperties=getInfoSingleResult(getPageAsHtmlString(new URL(URL+mat.group(1))),mail);
            for(String key:someProperties.keySet())
                properties.put(key+count,someProperties.get(key));
            count++;
        }
        return properties;
    }

    private static Map<String, String> getInfoSingleResult(String page, String mail) throws ParsingException, DifferentMailException {
        Pattern pat = Pattern.compile(TABLA_DATOS_REGEX);
        Matcher mat = pat.matcher(page);
        if(!mat.find())
            throw new ParsingException("Table not found");
        String table=mat.group(1);
        Map<String,String> properties=new HashMap<>();
        Pattern pat2=Pattern.compile(ENTRADA_TABLA);
        Matcher mat2=pat2.matcher(table);
        while (mat2.find()){
            try {
                String[] prop=extractProperty(mat2.group(1));
                properties.put(prop[0],prop[1]);
            }catch (ParsingException e){
                //Ignore entries that do not contain data
            }
        }
        if(!properties.values().contains(mail))
            throw new DifferentMailException("Result corresponds to another mail");
        return properties;
    }

    private static String[] extractProperty(String tableEntry) throws ParsingException {
        String[] res=new String[2];
        String parsed=tableEntry.replaceAll(TAG," ");
        parsed=parsed.replaceAll("\\s\\s+"," ").trim();
        Pattern pat=Pattern.compile(DATA_REGEX);
        Matcher mat=pat.matcher(parsed);
        if(!mat.matches()){
            Pattern patTlfn=Pattern.compile(TLFN_REGEX);
            Matcher matTlfn=patTlfn.matcher(parsed);
            if(matTlfn.matches()){
                res[0]="Telefono"+parsed.hashCode(); //Por si hay mas de 2 telefonos, si no poner Telefono2 y ya
                res[1]=parsed;
                return res;
            }
            else
                throw new ParsingException("Not a data field");
        }
        res[0]=mat.group(1);
        Pattern patMail=Pattern.compile(MAIL_FUNCTION_REGEX);
        Matcher matMail=patMail.matcher(mat.group(2));
        if(matMail.matches()){
            res[1]=recoverEmail(matMail.group(1),matMail.group(2));
        }
        else {
            res[1] = mat.group(2).trim();
        }
        //System.err.println("r0 "+res[0]+" r1 "+res[1]);
        return res;
    }

    private static String getField(String fieldName, String htmlText){
        String openTag="<"+fieldName+".*?>";
        Pattern patOpen= Pattern.compile(openTag);
        String closeTag="</"+fieldName+">";
        Pattern patClose= Pattern.compile(closeTag);
        Pattern patOr= Pattern.compile(openTag+"|"+closeTag);
        Matcher matOr=patOr.matcher(htmlText);
        if(!matOr.find()) //Si no hay de ese field devuelves vacio, usado para iterar entradas de la tabla
            return "";
        if(!patOpen.matcher(matOr.group(0)).matches())
            throw new RuntimeException("The first tag found is not an opening tag for the field");
        int firstCharIndx=matOr.start();
        int lastCharIndx=firstCharIndx;
        int count=1;
        while(count!=0 && matOr.find()){
            if(patOpen.matcher(matOr.group(0)).matches())
                count+=1;
            else if(patClose.matcher(matOr.group(0)).matches())
                count-=1;
            else
                throw new RuntimeException("Matched something that is not an opening or closing tag for the field");
            lastCharIndx=matOr.end();
        }
        return htmlText.substring(firstCharIndx,lastCharIndx);
    }

    public static String recoverEmail(String dom,String usr){
        String separator="@";
        String mail=new String(Base64.getDecoder().decode(usr));
        String domain=new String(Base64.getDecoder().decode(dom));
        return mail+separator+domain;
    }

    public static String getPageAsHtmlString(URL myurl) throws IOException {
        HttpsURLConnection con = (HttpsURLConnection) myurl.openConnection();
        con.setRequestProperty ( "User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:63.0) Gecko/20100101 Firefox/63.0");
        InputStream ins = con.getInputStream();
        InputStreamReader isr = new InputStreamReader(ins,"ISO-8859-1"); //La página está en ese encoding
        BufferedReader in = new BufferedReader(isr);
        String inputLine;
        String page="";
        while ((inputLine = in.readLine()) != null) {
            page+=inputLine.trim();
        }
        in.close();
        page=StringUtils.stripAccents(page);
        Pattern pat=Pattern.compile(ACUTE_REGEX);
        Matcher mat=pat.matcher(page);
        page=mat.replaceAll("$1");
        return page;
    }

}
