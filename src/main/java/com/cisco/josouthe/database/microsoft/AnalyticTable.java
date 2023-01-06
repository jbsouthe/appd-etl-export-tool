package com.cisco.josouthe.database.microsoft;

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
import java.text.ParseException;

public class AnalyticTable extends MicrosoftTable implements IAnalyticTable {
    protected static final Logger logger = LogManager.getFormatterLogger();

    public AnalyticTable(Result result, Database database ) {
        super(result.targetTable, "Analytic Table", database);
        columns.put("startTimestamp", new ColumnFeatures("startTimestamp", MicrosoftDatabase.DATE_TYPE, -1, false));
        columns.put("endTimestamp", new ColumnFeatures("endTimestamp", MicrosoftDatabase.DATE_TYPE, -1, false));
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



    @Override
    public int insert(Object object) {
        Result result = (Result) object;
        int counter=0;
        StringBuilder sqlBeginning = new StringBuilder(String.format("insert into %s (",name));
        StringBuilder sqlEnding = new StringBuilder(") VALUES (");
        for( String key : columns.keySet()) {
            sqlBeginning.append(" ").append( getColumns().get(key).name ).append(",");
            if( columns.get(key).type.equals(MicrosoftDatabase.DATE_TYPE) ) {
                sqlEnding.append("dateadd(s, ?/1000, '1970-01-01'),");
            } else {
                sqlEnding.append("?,");
            }
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
                        case "startTimestamp": { preparedStatement.setLong(parameterIndex++, result.startTimestamp); break; }
                        case "endTimestamp": { preparedStatement.setLong(parameterIndex++, result.endTimestamp); break; }
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
                                                preparedStatement.setLong(parameterIndex++, Utility.parseDateString((String) data));
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
        }
        return counter;
    }
}
