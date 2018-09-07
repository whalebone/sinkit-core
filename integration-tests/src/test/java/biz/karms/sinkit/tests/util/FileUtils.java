package biz.karms.sinkit.tests.util;

import biz.karms.sinkit.ioc.IoCRecord;
import com.google.gson.GsonBuilder;
import org.apache.commons.io.IOUtils;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;

/**
 * @Author Krystof Kolar
 */
public class FileUtils {
    /**
     * Reads file on classpath(in resources folder)
     */
    public static String readFileIntoString(String filename) throws java.io.IOException{
        InputStream jsonFileInputStream = FileUtils.class.getClassLoader()
                .getResourceAsStream(filename);
        StringWriter writer = new StringWriter();
        IOUtils.copy(jsonFileInputStream, writer, "utf8");
        return writer.toString();

    }


}
