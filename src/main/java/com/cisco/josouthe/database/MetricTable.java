package com.cisco.josouthe.database;

import com.cisco.josouthe.data.MetricData;
import com.cisco.josouthe.data.MetricValue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;


public class MetricTable extends Table {

    public MetricTable( String tableName, Database database ) {
        super(tableName,database);
        columns.put("controller", new ColumnFeatures("controller", "varchar2", 50, false));
        columns.put("application", new ColumnFeatures("application", "varchar2", 50, false));
        columns.put("metricname", new ColumnFeatures("metricName", "varchar2", 200, false));
        columns.put("metricpath", new ColumnFeatures("metricPath", "varchar2", 200, false));
        columns.put("frequency", new ColumnFeatures("frequency", "varchar2", 50, false));
        columns.put("userange", new ColumnFeatures("userange", "number", 22, false));
        for( String columnName : new String[] { "metricid","startTimeInMillis", "occurrences", "currentValue", "min", "max", "count", "sum", "value", "standardDeviation"})
            columns.put(columnName.toLowerCase(), new ColumnFeatures(columnName, "number", 22, false));
        columns.put("startTimestamp", new ColumnFeatures("startTimestamp", "date", 7, false));
    }

    public int insert(Object object) {
        MetricData metric = (MetricData) object;
        int counter=0;
        StringBuilder insertSQL = new StringBuilder(String.format("insert into %s (",name));
        insertSQL.append("controller, application, metricname, metricpath, frequency, metricid, userange, ");
        insertSQL.append("startTimeInMillis, occurrences, currentvalue, min, max, count, sum, value, standardDeviation, startTimestamp");
        insertSQL.append(") VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,TO_DATE('19700101','yyyymmdd') + ((?/1000)/24/60/60))");
        logger.debug("insertMetric SQL: %s",insertSQL);
        Connection conn = null;
        try{
            conn = DriverManager.getConnection( this.database.connectionString, this.database.user, this.database.password);
            PreparedStatement preparedStatement = conn.prepareStatement(insertSQL.toString());
            for(MetricValue metricValue : metric.metricValues ) {
                preparedStatement.setString(1, metric.controllerHostname);
                preparedStatement.setString(2, metric.applicationName);
                preparedStatement.setString(3, metric.metricName);
                preparedStatement.setString(4, metric.metricPath);
                preparedStatement.setString(5, metric.frequency);
                preparedStatement.setLong(6, metric.metricId);
                preparedStatement.setInt(7, (metricValue.useRange ? 1: 0 ));
                preparedStatement.setLong(8, metricValue.startTimeInMillis);
                preparedStatement.setLong(9, metricValue.occurrences);
                preparedStatement.setLong(10, metricValue.current);
                preparedStatement.setLong(11, metricValue.min);
                preparedStatement.setLong(12, metricValue.max);
                preparedStatement.setLong(13, metricValue.count);
                preparedStatement.setLong(14, metricValue.sum);
                preparedStatement.setLong(15, metricValue.value);
                preparedStatement.setDouble(16, metricValue.standardDeviation);
                preparedStatement.setLong(17, metricValue.startTimeInMillis);
                counter += preparedStatement.executeUpdate();
            }
        } catch (Exception exception) {
            logger.error("Error inserting metrics into %s, Exception: %s", name, exception.toString());
        } finally {
            if( conn != null ) {
                try {
                    conn.close();
                } catch (SQLException ignored) {}
            }
        }
        return counter;
    }
}
