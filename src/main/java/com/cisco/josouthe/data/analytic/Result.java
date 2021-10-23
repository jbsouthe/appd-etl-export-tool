package com.cisco.josouthe.data.analytic;

public class Result {
    public String query;
    public String schema;
    public boolean moreData;
    public Field[] fields;
    public Object[][] results;
    public String name, targetTable;
    public long startTimestamp, endTimestamp;

    public String toString() {
        int rows =0, columns=0;
        if( fields != null ) columns=fields.length;
        if( results != null ) rows= results.length;
        StringBuffer sb = new StringBuffer(String.format("Analytics Query %s '%s' Columns Returned: %d Rows Returned: %d\n",name, query, columns, rows));
        if( rows == 0 ) return sb.toString();
        Integer[] maxSizes = new Integer[columns];
        for( int i=0; i< fields.length; i++ )
            maxSizes[i] = fields[i].label.length();
        for( int j=0; j< results.length; j++)
            for( int k=0; k< results[j].length; k++)
                maxSizes[k] = Integer.max(String.valueOf(results[j][k]).length(), maxSizes[k]);
        sb.append("\t| ");
        for( Field field : fields ) {
            sb.append(field.label +" |");
        }
        sb.append("\n");
        sb.append("\t|-");
        for( int i=0; i< fields.length; i++ ) {
            sb.append(printFill("-",maxSizes[i]) +"-|");
        }
        sb.append("\n");
        for( int i = 0; i < results.length; i++) {
            sb.append("\t| ");
            for( int j=0; j < results[i].length ; j++)
                sb.append(printWithPadding(String.valueOf(results[i][j]), maxSizes[j])+" |");
            sb.append("\n");
        }
        return sb.toString();
    }

    private String printWithPadding( String theString, int maxSize ) {
        StringBuffer sb = new StringBuffer(theString);
        for( int i=theString.length(); i< maxSize; i++)
            sb.append(" ");
        return sb.toString();
    }

    private String printFill( String character, int padSize) {
        StringBuffer sb = new StringBuffer();
        for(int i=0; i< padSize; i++) sb.append(character);
        return sb.toString();
    }
}
