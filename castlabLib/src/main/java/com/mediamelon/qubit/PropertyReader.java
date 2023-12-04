package com.mediamelon.qubit;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;

/**
 * Created by shri on 20/10/15.
 */
//159268936
public class PropertyReader {
    static class PropertyHolder {
        public static PropertyReader propertyReader = new PropertyReader();
    }
    public static PropertyReader getInstance() {
        return PropertyHolder.propertyReader;
    }
    
    static private String KFallbackRegistrationServerURL = "https://register.mediamelon.com/mm-apis/register/";
    static private String KFallbackVersion = "JAVASDK3.10.1_CLPP_4.2.56_1.0.6";
    
    private HashMap<String,String> propertiesMap;
    private boolean isTestBuild = false;
    private PropertyReader() {
        propertiesMap = new HashMap<String, String>();
        InputStream inputStream = null;
        Properties properties = new Properties();
        if(isTestBuild == false) {
            MMLogger.v("EPIntegration","Not a test build ...");
            
            propertiesMap.put("RegisterURL", KFallbackRegistrationServerURL);
            propertiesMap.put("Version", KFallbackVersion);
	    }
	    else{
            propertiesMap.put("PlayerName", "TestPlayer");
            propertiesMap.put("CustomerId", "791434980");
            propertiesMap.put("RegisterURL", "http://register.mediamelon.com/mm-apis/register/");
            propertiesMap.put("Version", "Engineering Debug Build");
        }
    }

    public void setProperties(HashMap<String, String> map){ //for overriding existing kvp or adding new ones
        Set<String> keySet = map.keySet();
        Iterator<String> keySetIterator = keySet.iterator();
        while (keySetIterator.hasNext()) {
            String key = keySetIterator.next();
            setProperty(key, map.get(key));
        }
    }

    public void setProperty(String key, String value){ //for overriding existing kvp or adding new ones
        propertiesMap.put(key, value);
    }

    public String getProperty(String name) {
        boolean hasKvp = propertiesMap.containsKey(name);
        String retval = "";
        
        if(hasKvp){
           retval = propertiesMap.get(name);
        }
        MMLogger.v("EPIntegration", "Got candidate value " + retval + " for the key " + name);

        if(retval.length() > 0) {
            return retval;
        } else {
            return "Unknown";
        }
    }
}


