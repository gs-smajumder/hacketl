package util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Created by samujjal on 08/09/16.
 */
public class AppConfig {
    static Logger LOGGER = LoggerFactory.getLogger(AppConfig.class);
    protected static Properties properties = new Properties();

    static {
        String propertyFileName = "appconfig.properties";
        InputStream inputStream = AppConfig.class.getClassLoader().getResourceAsStream(propertyFileName);
        if(inputStream != null){
            try {
                properties.load(inputStream);
                inputStream.close();
                LOGGER.info("Succesfully loaded properties");
            } catch (IOException e) {
                LOGGER.error("Failed to load properties file", e);
            }
        }
    }

    public static String getProperty(String property){
        return properties.getProperty(property);
    }
}
