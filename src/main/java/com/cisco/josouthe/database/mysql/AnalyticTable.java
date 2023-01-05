package com.cisco.josouthe.database.mysql;

import com.cisco.josouthe.data.analytic.Field;
import com.cisco.josouthe.data.analytic.Result;
import com.cisco.josouthe.database.ColumnFeatures;
import com.cisco.josouthe.database.Database;
import com.cisco.josouthe.database.IAnalyticTable;
import com.cisco.josouthe.util.Utility;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.ParseException;

public class AnalyticTable extends MySQLTable implements IAnalyticTable {
    protected static final Logger logger = LogManager.getFormatterLogger();

    public AnalyticTable(Result result, Database database ) {
        super(result.targetTable, "Analytic Table", database);
        columns.put("starttimestamp", new ColumnFeatures("starttimestamp", MySQLDatabase.DATE_TYPE, -1, false));
        columns.put("endtimestamp", new ColumnFeatures("endtimestamp", MySQLDatabase.DATE_TYPE, -1, false));
        for(Field field: result.fields) {
            columns.put( field.label,
                    new ColumnFeatures(database.convertToAcceptableColumnName(field.label, columns.values()),
                            getColumnTypeForName(field.type),
                            getColumnSizeForName(field.type),
                            true)
            );
            logger.debug("Added column for field %s column definition: %s", field.label, columns.get(field.label));
        }
        this.initTable();
    }

    private int getColumnSizeForName(String type) {
        switch(type) {
            case "string": return MySQLDatabase.STRING_SIZE;
            case "integer": return MySQLDatabase.INTEGER_SIZE;
            case "float": return MySQLDatabase.FLOAT_SIZE;
            case "boolean": return MySQLDatabase.BOOLEAN_SIZE;
            case "date": return MySQLDatabase.DATE_SIZE;
            default: {
                logger.debug("Unknown data type: %s setting table column size to %s", type, MySQLDatabase.STRING_SIZE);
            }
        }
        return MySQLDatabase.STRING_SIZE;
    }

    private String getColumnTypeForName(String type) {
        switch(type) {
            case "string": return MySQLDatabase.STRING_TYPE;
            case "integer": return MySQLDatabase.INTEGER_TYPE;
            case "float": return MySQLDatabase.FLOAT_TYPE;
            case "boolean": return MySQLDatabase.BOOLEAN_TYPE;
            case "date": return MySQLDatabase.DATE_TYPE;
            default: {
                logger.debug("Unknown data type: %s setting table column type to %s", type, MySQLDatabase.STRING_TYPE);
            }
        }
        return MySQLDatabase.STRING_TYPE;
    }

    @Override
    public int insert(Object object) {
        Result result = (Result) object;
        int counter=0;
        StringBuilder sqlBeginning = new StringBuilder(String.format("insert into %s (",name));
        StringBuilder sqlEnding = new StringBuilder(") VALUES (");
        for( String key : columns.keySet()) {
            sqlBeginning.append(" ").append( getColumns().get(key).name.toLowerCase() ).append(",");
            sqlEnding.append("?,");
        }
        StringBuilder insertSQL = new StringBuilder(sqlBeginning.substring(0, sqlBeginning.length()-1));
        insertSQL.append( sqlEnding.substring(0, sqlEnding.length()-1) );
        insertSQL.append(")");
        logger.trace("Data to insert: %s", result.toString());
        logger.trace("insertAnalytics SQL: %s", insertSQL);
        this.initTable();
        try ( Connection conn = database.getConnection(); PreparedStatement preparedStatement = conn.prepareStatement(insertSQL.toString());){
            for( int dataIndex=0; dataIndex<result.results.length; dataIndex++) {
                int parameterIndex = 1;
                for (String key : columns.keySet()) {
                    switch (key) {
                        case "starttimestamp": { preparedStatement.setTimestamp(parameterIndex++, new Timestamp(result.startTimestamp)); break; }
                        case "endtimestamp": { preparedStatement.setTimestamp(parameterIndex++, new Timestamp(result.endTimestamp)); break; }
                        default: {
                            int fieldIndex=0;
                            for (Field field : result.fields) {
                                if( field.label.equals(key)) {
                                    Object data = result.results[dataIndex][fieldIndex];
                                    if( data == null ) {
                                        preparedStatement.setObject(parameterIndex++, data);
                                    } else {
                                        if( data instanceof Integer ) {
                                            preparedStatement.setInt(parameterIndex++, (Integer) data);
                                        } else if( data instanceof Long ) {
                                            preparedStatement.setLong(parameterIndex++, (Long) data);
                                        } else if( data instanceof Double ) {
                                            preparedStatement.setDouble(parameterIndex++, (Double) data);
                                        } else if( data instanceof  Float ) {
                                            preparedStatement.setFloat(parameterIndex++, (Float) data);
                                        } else if( data instanceof Boolean ) {
                                            preparedStatement.setInt(parameterIndex++, ((Boolean)data? 1 : 0 ));
                                        } else if( data instanceof String && Utility.isThisStringADate((String)data) ) {
                                            //logger.trace("This String is a date: %s",data);
                                            try {
                                                preparedStatement.setTimestamp(parameterIndex++, new Timestamp(Utility.parseDateString((String) data)));
                                            } catch (SQLException exception) {
                                                throw exception;
                                            } catch (ParseException e) {
                                                logger.warn("Parse exception for date time conversion: %s Exception: %s",data, e.getMessage());
                                            }
                                        } else {
                                            //logger.trace("This String/Thing is not a date: %s",data);
                                            preparedStatement.setString(
                                                    parameterIndex++,
                                                    fitToSize(String.valueOf(data), getColumn(key).size)
                                                    /* this is a bit obtuse, and better as a method for other uses
                                                    String.valueOf(data).substring(
                                                            0,
                                                            Math.min(getColumns().get(key).size, String.valueOf(data).length())
                                                    )*/
                                            );
                                        }
                                    }
                                }
                                fieldIndex++;
                            }
                        }
                    }
                }
                counter += preparedStatement.executeUpdate();
            }
        } catch (Exception exception) {
            logger.error("Error inserting analytics data into %s, Exception: %s", name, exception.toString(), exception);
            logger.warn("Bad SQL: %s",insertSQL.toString());
        }
        return counter;
    }
}
