package com.pacuss.cashcard.utility;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Util {
    public static String loadDetailsFromPropertiesFile(String property) {
        try(InputStream propertiesStream = Util.class.getResourceAsStream("/application.properties")){
            Properties properties = new Properties();
            properties.load(propertiesStream);
            return properties.getProperty(property);
        }catch (IOException e){
            throw new IllegalArgumentException(e);
        }
    }
}
