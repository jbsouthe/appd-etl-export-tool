package com.cisco.josouthe.util;

import com.appdynamics.apm.appagent.api.DataScope;
import com.cisco.josouthe.database.TypeAndSize;
import net.sourceforge.argparse4j.inf.Namespace;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utility {
    private static final Logger logger = LogManager.getFormatterLogger();
    private static Pattern patternConnectionString = Pattern.compile("^(?<jdbc>[j|J][d|D][b|B][c|C]:)?(?<vendor>[^:]+):(?<driver>[^:]+):(?<path>.*);?");
    private static Pattern patternAnalyticsDateString = Pattern.compile("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}Z");
    private static SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
    private static Set<DataScope> snapshotDatascope;

    public static String encode( String original ){
        /* this, of course, got rediculous, replacing with a utility external to code base */
        try {
            return URLEncoder.encode(original, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            logger.warn("Unsupported Encoding Exception: %s", e.getMessage());
        }
        //falling back to the manual, because it is already written..
        return original.replace("%", "%25")
                .replace("|","%7C")
                .replace(" ", "%20")
                .replace(":","%3A")
                .replace(".", "%2E")
                .replace("-", "%2D")
                .replace("#", "%23")
                .replace("\"", "%22")
                .replace("/", "%2F")
                .replace("(", "%28")
                .replace(")", "%29")
                .replace("<", "%3C")
                .replace(">", "%3E")
                ;
    }

    public static String getDateString(long dateTime) { //ISO8601 Date (Extend) https://dencode.com/en/date/iso8601
        return ZonedDateTime.ofInstant(new Date(dateTime).toInstant(), ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX"));
    }
    public static String getEncodedDateString( long dateTime ) {
        return encode(getDateString(dateTime));
    }

    public static TypeAndSize parseTypeAndSizeString( String typeAndSize ) {
        Pattern pattern = Pattern.compile("(\\S)\\((\\d+).*\\)");
        Matcher matcher = pattern.matcher(typeAndSize);
        if(matcher.find()) {
            return new TypeAndSize(matcher.group(0), Integer.parseInt(matcher.group(1)) );
        }
        return null;
    }

    public static String escapeQuotes( String input ) {
        return input.replace("\"", "\\\"");
    }

    public static String parseDatabaseVendor( String connectionString) {
        Matcher matcher = patternConnectionString.matcher(connectionString);
        if(matcher.find()) return matcher.group("vendor");
        return "oracle";
    }

    public static String parseDatabasePath( String connectionString ) {
        Matcher matcher = patternConnectionString.matcher(connectionString);
        if(matcher.find()) return matcher.group("path");
        return null;
    }

    public static String cleanFileName( String in ) {
        return in.replace('.', '_').replace(' ', '_');
    }

    public static long now( long adj ) { return now()+adj; }
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

    public static Set<DataScope> getSnapshotDatascope() {
        if( snapshotDatascope == null ) {
            snapshotDatascope = new HashSet<>();
            snapshotDatascope.add(DataScope.SNAPSHOTS);
        }
        return snapshotDatascope;
    }

    public static boolean isAPIKeyValidFormat(String clientID, String hostname) {
        if( clientID == null || hostname == null ) return false;
        if( !clientID.contains("@") ) return false;
        String apikey = clientID.split("\\@")[0];
        if( hostname.contains(".") )
            hostname = hostname.split("\\.")[0];
        if( String.format("%s@%s",apikey,hostname).equals(clientID) ) return true;
        return false;
    }

    public static List<String> getCommands( Namespace namespace ) {
        List<String> commands = new ArrayList<>();
        try {
            commands = namespace.getList("command");
        } catch (java.lang.ClassCastException ignored) {
            commands.add(namespace.getString("command"));
        }
        return commands;
    }
}
