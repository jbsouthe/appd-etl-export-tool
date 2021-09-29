package com.cisco.josouthe.util;

public class Encoder {
    public static String encode( String original ){
        return original.replace("|","%7C").replace(" ", "%20");
    }
}
