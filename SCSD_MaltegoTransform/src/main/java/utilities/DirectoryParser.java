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
    private static final String TAG = "<.*?>";

    private static final Pattern NUM_RESULT = Pattern.compile("Encontradas (\\s*|<strong>)[0-9]+(\\s*|</strong>) entradas");
    private static final Pattern TABLA_MULTIPLE_REGEX = Pattern.compile("Personas.*?<table.*?>");
    private static final Pattern HREF_REGEX = Pattern.compile("<\\s*a\\s+href=['\"]([?a-zA-Z0-9@:%._+~=&]+)['\"].*?>");

    private static final Pattern TABLA_DATOS_REGEX = Pattern.compile("infoElem[^>]*>(.*?)</table>"); //Enough for single result
    private static final Pattern ENTRADA_TABLA = Pattern.compile("<tr>(.*?)</tr>"); //Enough for single result

    private static final Pattern DATA_REGEX = Pattern.compile("(.+?):\\s*(.+)");
    private static final Pattern MAIL_FUNCTION_REGEX = Pattern.compile("correo\\(['\"](.*?)['\"],['\"](.*?)['\"],.*?\\)");
    private static final Pattern TLFN_REGEX = Pattern.compile("(\\+34 )?\\d{9}");
    private static final Pattern ACUTE_REGEX = Pattern.compile("&([a-zA-Z])acute;");

    public static Map<String, String> getInfo(String page, String mail) {
        Map<String,String> information=new HashMap<>();
        Matcher mat = NUM_RESULT.matcher(page);
        try {
            if(mat.find())
                information=getInfoMultipleResults(page,mail);
            else
                information= getInfoSingleResult(page,mail);
        } catch (ParsingException | DifferentMailException e) {
            System.err.println("No info");
        }
        return information;
    }

    private static Map<String, String> getInfoMultipleResults(String page, String mail) throws ParsingException {
        Map<String,String> properties=new HashMap<>();
        Matcher mat = TABLA_MULTIPLE_REGEX.matcher(page);
        if(!mat.find())
            throw new ParsingException("Mail not found in directory");
        String table=getField("table",page.substring(mat.start()));
        String tableEntry=getField("tr",table);
        Pattern auxPat=Pattern.compile("<table.*?>");
        Matcher auxMat=auxPat.matcher(table);
        auxMat.find();
        String parsingTable=table.substring(auxMat.end()); //All except <table...> to correctly extract and remove fields
        boolean found=false; //Once we find a correct entry, the others are not needed
        while (!tableEntry.equals("") && !found){
            try {
                properties=getInfoPerson(tableEntry,mail);
                found=true;
            } catch (DifferentMailException | ParsingException | IOException e) {
                //Ignore invalid entries
            }
            parsingTable=parsingTable.substring(tableEntry.length()); //Delete parsed entry from string
            tableEntry=getField("tr",parsingTable);
        }
        return properties;
    }

    private static Map<String, String> getInfoPerson(String tableEntry,String mail) throws ParsingException, DifferentMailException, IOException {
        Map<String,String> properties=new HashMap<>();
        Matcher mat=HREF_REGEX.matcher(tableEntry);
        int count=0;
        while(mat.find()){
            Map<String,String> someProperties=getInfoSingleResult(getPageAsHtmlString(new URL(URL+mat.group(1))),mail);
            for(String key:someProperties.keySet())
                properties.put(key+count,someProperties.get(key));
            count++;
        }
        return properties;
    }

    private static Map<String, String> getInfoSingleResult(String page, String mail) throws ParsingException, DifferentMailException {
        Matcher mat = TABLA_DATOS_REGEX.matcher(page);
        if(!mat.find())
            throw new ParsingException("Table not found");
        String table=mat.group(1);
        Map<String,String> properties=new HashMap<>();
        Matcher mat2=ENTRADA_TABLA.matcher(table);
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
        Matcher mat=DATA_REGEX.matcher(parsed);
        if(!mat.matches()){
            Matcher matTlfn=TLFN_REGEX.matcher(parsed);
            if(matTlfn.matches()){
                res[0]="Telefono"+parsed.hashCode();
                res[1]=parsed;
                return res;
            }
            else
                throw new ParsingException("Not a data field");
        }
        res[0]=mat.group(1);
        Matcher matMail=MAIL_FUNCTION_REGEX.matcher(mat.group(2));
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
        if(!matOr.find()) //If no field present return empty string, useful for parsing successive entries
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
        InputStreamReader isr = new InputStreamReader(ins,"ISO-8859-1"); //Page uses this encoding
        BufferedReader in = new BufferedReader(isr);
        String inputLine;
        String page="";
        while ((inputLine = in.readLine()) != null) {
            page+=inputLine.trim();
        }
        in.close();
        page=StringUtils.stripAccents(page);    // Delete accents except for the field Curriculum that works differently for some reason
        Matcher mat=ACUTE_REGEX.matcher(page);
        page=mat.replaceAll("$1"); // Correct Curriculum field
        return page;
    }

}
