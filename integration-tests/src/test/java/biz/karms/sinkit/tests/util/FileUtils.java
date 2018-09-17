package biz.karms.sinkit.tests.util;

import com.google.common.io.Resources;
import org.apache.commons.io.Charsets;

import java.net.URL;

/**
 * @Author Krystof Kolar
 */
public class FileUtils {
    /**
     * Reads file on classpath(in resources folder)
     */
    public static String readFileIntoString(String filename) throws java.io.IOException{
       // InputStream jsonFileInputStream = FileUtils.class.getClassLoader()
         //       .getResourceAsStream(filename);
        //#StringWriter writer = new StringWriter();
        //#IOUtils.copy(jsonFileInputStream, writer, "utf8");
        //#jsonFileInputStream.close();
        URL url = Resources.getResource(filename);
        return Resources.toString(url, Charsets.UTF_8);
        //return writer.toString();

    }


}
