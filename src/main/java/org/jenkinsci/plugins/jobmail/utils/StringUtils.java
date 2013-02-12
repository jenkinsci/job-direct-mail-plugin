package org.jenkinsci.plugins.jobmail.utils;

public final class StringUtils {
    private StringUtils() {
        
    }
    
    public static String noNull(String string) {
        return string == null ? "" : string;
    }
    
    
}
