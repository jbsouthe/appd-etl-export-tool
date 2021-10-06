package com.cisco.josouthe.util;

import jdk.internal.net.http.common.Pair;

import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utility {
    public static String encode( String original ){
        return original.replace("|","%7C").replace(" ", "%20");
    }

    public static Pair<String,Integer> parseTypeAndSizeString( String typeAndSize ) {
        Pattern pattern = Pattern.compile("(\\S)\\((\\d+).*\\)");
        Matcher matcher = pattern.matcher(typeAndSize);
        if(matcher.find()) {
            return new Pair<>(matcher.group(0), Integer.parseInt(matcher.group(1)) );
        }
        return null;
    }

    public static String parseDatabaseVendor( String connectionString) {
        Pattern pattern = Pattern.compile("jdbc:(\\S):.*");
        Matcher matcher = pattern.matcher(connectionString);
        if(matcher.find()) return matcher.group(0);
        return "oracle";
    }

    public static long now() { return new Date().getTime(); }
}
