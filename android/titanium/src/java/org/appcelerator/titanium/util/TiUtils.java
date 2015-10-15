package org.appcelerator.titanium.util;

import java.util.Map;

public class TiUtils {
    public static String fastReplace(String source, String os, String ns) {
        if (source == null) {
            return null;
        }
        int i = 0;
        if ((i = source.indexOf(os, i)) >= 0) {
            char[] sourceArray = source.toCharArray();
            char[] nsArray = ns.toCharArray();
            int oLength = os.length();
            StringBuilder buf = new StringBuilder(sourceArray.length);
            buf.append(sourceArray, 0, i).append(nsArray);
            i += oLength;
            int j = i;
            // Replace all remaining instances of oldString with newString.
            while ((i = source.indexOf(os, i)) > 0) {
                buf.append(sourceArray, j, i - j).append(nsArray);
                i += oLength;
                j = i;
            }
            buf.append(sourceArray, j, sourceArray.length - j);
            source = buf.toString();
            buf.setLength(0);
        }
        return source;
    }
    
    public static String[] fastSplit(String s, char delimeter) {
        
        int count = 1;
 
        for (int i = 0; i < s.length(); i++)
            if (s.charAt(i) == delimeter)
                count++;
 
        String[] array = new String[count];
 
        int a = -1;
        int b = 0;
 
        for (int i = 0; i < count; i++) {
 
            while (b < s.length() && s.charAt(b) != delimeter)
                b++;
 
            array[i] = s.substring(a+1, b);
            a = b;
            b++;
            
        }
 
        return array;
 
    }
    
    
    public static <K, V> V mapGetOrDefault(Map<K,V> map, K key, V defaultValue) {
        return map.containsKey(key) ? map.get(key) : defaultValue;
    }
    
    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }
}
