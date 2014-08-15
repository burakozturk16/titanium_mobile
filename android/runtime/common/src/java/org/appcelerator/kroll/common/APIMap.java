/** This is generated, do not edit by hand. **/
package org.appcelerator.kroll.common;

import java.util.HashMap;
import java.util.Map;

public class APIMap {

    private static Map<String, String> PROXY_MAP = createProxyMap();

    private static Map<String, String> createProxyMap() {
        return new HashMap<String, String>();
    }
    
    public static String getProxyClass(String apiName_) {
        apiName_ = apiName_.replaceFirst("^(Titanium|Ti)\\.", "");
    	return PROXY_MAP.get(apiName_); 
    }
    
    public static void addMapping(Map<String, String> map) {
        PROXY_MAP.putAll(map);
    }
}