package com.cisco.josouthe.util;

import jdk.internal.net.http.common.Pair;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utility {
    private static Pattern patternConnectionString = Pattern.compile("jdbc:(\\S):.*");
    private static Pattern patternAnalyticsDateString = Pattern.compile("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}Z");
    private static SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

    public static String encode( String original ){
        return original.replace("|","%7C").replace(" ", "%20").replace(":","%3A").replace(".", "%2E").replace("-", "%2D");
    }

    public static String getDateString(long dateTime) { //ISO8601 Date (Extend) https://dencode.com/en/date/iso8601
        return ZonedDateTime.ofInstant(new Date(dateTime).toInstant(), ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX"));
    }
    public static String getEncodedDateString( long dateTime ) {
        return encode(getDateString(dateTime));
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
        Matcher matcher = patternConnectionString.matcher(connectionString);
        if(matcher.find()) return matcher.group(0);
        return "oracle";
    }

    public static long now() { return new Date().getTime(); }

    public static boolean isThisStringADate(String data) {
        if( data == null ) return false;
        //"2021-10-14T18:00:51.435Z"
        Matcher matcher = patternAnalyticsDateString.matcher(data);
        if(matcher.matches()) return true;
        return false;
    }

    public static long parseDateString(String data) throws ParseException {
        //"2021-10-14T18:00:51.435Z"
        return simpleDateFormat.parse(data).getTime();
    }
}
