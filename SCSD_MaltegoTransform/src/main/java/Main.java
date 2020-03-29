import maltego.MaltegoEntity;
import maltego.MaltegoTransform;
import java.io.*;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import static utilities.DirectoryParser.getInfo;
import static utilities.DirectoryParser.getPageAsHtmlString;
import static utilities.EntityExtractor.extractEntities;

public class Main {
	private static final String URL_SEARCH="https://www.um.es/atica/directorio/?nivel=&lang=0&vista=unidades&search=";
	private static final String UNOSOLO="jesus.garcia15@um.es";
	private static final String CARGOS="skarmeta@um.es";
	private static final String VARIOS="arm@um.es";
	private static final String VALID_MAIL_REGEX="[a-zA-Z._0-9\\-]+@um.es";

	
	public static void main(String[] args) throws Exception {
        MaltegoTransform mt=new MaltegoTransform();
		if(args.length==0)
            mt.throwException("No input for the transform");
		else{
            String mail=args[0];
            if(!validMail(mail))
                mt.throwException("Invalid mail. The domain has to be um.es");
            else {
                URL myurl = new URL(URL_SEARCH + mail);
                String page = getPageAsHtmlString(myurl);
                //String page =readFile(mail);
                //System.err.println(page);
                Map<String, String> extractedData = getInfo(page, mail);
                //for(String key:extractedData.keySet())
                //System.err.println(key+" "+extractedData.get(key));
                if (extractedData.size() == 0) {
                    MaltegoEntity e = new MaltegoEntity("maltego.EmailAddress", mail);
                    e.setNote("Mail not in directory");
                    mt.addEntity(e);
                    mt.returnOutput();
                } else {
                    List<MaltegoEntity> entities = extractEntities(extractedData,mail);
                    for (MaltegoEntity e : entities)
                        mt.addEntity(e);
                    mt.returnOutput();
                }
            }
        }
	}

	private static boolean validMail(String mail) {
		Pattern pat=Pattern.compile(VALID_MAIL_REGEX);
		Matcher mat=pat.matcher(mail);
		return mat.matches();
	}


	private static String readFile(String mail) throws IOException {
		String filename="soloPage.txt";
		if(mail.equals(CARGOS))
			filename="CargoPage.txt";
		if(mail.equals(VARIOS))
			filename="ArmPage.txt";
		InputStream is = new FileInputStream("./src/main/resources/"+filename);
		BufferedReader buf = new BufferedReader(new InputStreamReader(is,"UTF-8"));

		String line = buf.readLine();
		StringBuilder sb = new StringBuilder();

		while(line != null){
			sb.append(line).append("\n");
			line = buf.readLine();
		}
		return sb.toString();
	}

}
