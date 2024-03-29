package com.cisco.josouthe.print;

import com.cisco.josouthe.util.Utility;

import java.util.Date;
import java.util.List;

public class Printer {
    public static String print( List<IPrintable> list ) {
        return print( list.toArray(new IPrintable[0]));
    }

    public static String print( IPrintable... printables ) {
        if( printables == null || printables.length == 0 ) return null;
        String[] headers = printables[0].getHeader();
        int columnSizes[] = new int[headers.length];
        Justification justifications[] = printables[0].getJustifications();
        for( int i = 0; i< headers.length; i++ )
            columnSizes[i] = headers[i].length()+1;
        for( IPrintable printable : printables ) {
            for (int i = 0; i < headers.length; i++)
                if (columnSizes[i] < formatAsString(printable.toArray()[i]).length() ) columnSizes[i] = formatAsString(printable.toArray()[i]).length()+1;
        }
        StringBuilder outputStringBuilder = new StringBuilder();
        for( int i=0; i< headers.length; i++ ) {
            outputStringBuilder.append("--").append( fillString(columnSizes[i], '-') );
        }
        outputStringBuilder.append("--\n");

        for( int i=0; i< headers.length; i++ ) {
            outputStringBuilder.append("| ").append( paddString(columnSizes[i], headers[i], justifications[i]) );
        }
        outputStringBuilder.append(" |\n");
        for( int i=0; i< headers.length; i++ ) {
            outputStringBuilder.append("|-").append( fillString(columnSizes[i], '-') );
        }
        outputStringBuilder.append("-|\n");

        for( IPrintable printable : printables ) {
            for (int i = 0; i < headers.length; i++)
                outputStringBuilder.append("| ").append(paddString(columnSizes[i], formatAsString(printable.toArray()[i]), justifications[i]));
            outputStringBuilder.append(" |\n");
        }

        for( int i=0; i< headers.length; i++ ) {
            outputStringBuilder.append("--").append( fillString(columnSizes[i], '-') );
        }
        outputStringBuilder.append("--\n").append("rows returned: ").append(printables.length);
        return outputStringBuilder.toString();
    }

    private static String formatAsString(Object object) {
        if( object == null ) return "null";
        if( object instanceof Date || object instanceof java.sql.Date ) {
            return Utility.formatControlDate( (Date)object );
        } else {
            return String.valueOf(object);
        }
    }

    public static String paddString( int size, String string, Justification justification ) {
        switch (justification) {
            case LEFT: {
                return string + fillString(size-string.length(), ' ');
            }
            case RIGHT: {
                return fillString(size-string.length(), ' ') + string;
            }
            default: { //CENTER
                boolean addOne = false;
                if( (size-string.length()) %2 == 1 ) addOne=true;
                return fillString((size-string.length())/2, ' ') + string +fillString((size-string.length())/2, ' ') + (addOne ? " ": "");
            }
        }
    }
    public static String fillString( int size, char c ) {
        StringBuilder sb = new StringBuilder();
        for( int i=0; i< size; i++) sb.append(c);
        return sb.toString();
    }
}
