package com.cisco.josouthe.print;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.LinkedList;

public class ResultSetPrinter implements IPrintable{
    private LinkedList<String> columnNames= new LinkedList<>();
    private LinkedList<Object> columnValues= new LinkedList<>();
    private LinkedList<Justification> columnJustifications = null;

    public ResultSetPrinter(ResultSet resultSet) throws SQLException {
        for (int i = 1; i <= resultSet.getMetaData().getColumnCount(); i++) {
            columnNames.add(resultSet.getMetaData().getColumnLabel(i));
            Object object = resultSet.getObject(i);
            if( object == null || object.toString().toLowerCase().equals("[null]") || object.toString().toLowerCase().equals("null")) {
                columnValues.add("null");
            } else {
                columnValues.add(resultSet.getObject(i));
            }
        }
    }

    @Override
    public Object[] toArray() {
        return columnValues.stream().toArray();
    }

    @Override
    public String[] getHeader() {
        return columnNames.toArray( new String[0] );
    }

    @Override
    public Justification[] getJustifications() {
        if( this.columnJustifications == null ) {
            this.columnJustifications = new LinkedList<>();
            for( Object object : toArray() ) {
                if( object instanceof Date || object instanceof java.sql.Date )  { this.columnJustifications.add(Justification.CENTER); }
                else if( object instanceof Number ) { this.columnJustifications.add(Justification.RIGHT); }
                else { this.columnJustifications.add(Justification.LEFT); }
            }
        }
        return this.columnJustifications.toArray( new Justification[0] );
    }
}
