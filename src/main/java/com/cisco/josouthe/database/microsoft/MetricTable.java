package com.cisco.josouthe.database.microsoft;

import com.cisco.josouthe.data.metric.MetricData;
import com.cisco.josouthe.data.metric.MetricValue;
import com.cisco.josouthe.database.ColumnFeatures;
import com.cisco.josouthe.database.Database;
import com.cisco.josouthe.database.IMetricTable;
import com.cisco.josouthe.exceptions.FailedDataLoadException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;


public class MetricTable extends MicrosoftTable implements IMetricTable {
    protected static final Logger logger = LogManager.getFormatterLogger();

    public MetricTable(String tableName, Database database ) {
        super(tableName,"Metric Table",database);
        columns.put("controller", new ColumnFeatures("controller", MicrosoftDatabase.STRING_TYPE, 50, false));
        columns.put("application", new ColumnFeatures("application", MicrosoftDatabase.STRING_TYPE, 50, false));
        columns.put("metricname", new ColumnFeatures("metricName", MicrosoftDatabase.STRING_TYPE, 200, false));
        columns.put("metricpath", new ColumnFeatures("metricPath", MicrosoftDatabase.STRING_TYPE, 200, false));
        columns.put("frequency", new ColumnFeatures("frequency", MicrosoftDatabase.STRING_TYPE, 50, false));
        columns.put("userange", new ColumnFeatures("userange", MicrosoftDatabase.INTEGER_TYPE, -1, false));
        for( String columnName : new String[] { "metricid","startTimeInMillis", "occurrences", "currentValue", "min", "max", "count", "sum", "value", "standardDeviation"})
            columns.put(columnName.toLowerCase(), new ColumnFeatures(columnName, MicrosoftDatabase.INTEGER_TYPE, -1, false));
        columns.put("startTimestamp", new ColumnFeatures("startTimestamp", MicrosoftDatabase.DATE_TYPE, -1, false));
        this.initTable();
    }

    public int insert(Object object) throws FailedDataLoadException {
        MetricData metric = (MetricData) object;
        int counter=0;
        StringBuilder insertSQL = new StringBuilder(String.format("insert into %s (",name));
        insertSQL.append("controller, application, metricname, metricpath, frequency, metricid, userange, ");
        insertSQL.append("startTimeInMillis, occurrences, currentvalue, min, max, count, sum, value, standardDeviation, startTimestamp");
        insertSQL.append(") VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,dateadd(s, ?/1000, '1970-01-01'))");
        logger.trace("insertMetric SQL: %s",insertSQL);
        try ( Connection conn = database.getConnection(); PreparedStatement preparedStatement = conn.prepareStatement(insertSQL.toString());){
            //for(MetricValue metricValue : metric.metricValues ) {
            while( !metric.metricValues.isEmpty() ) {
                MetricValue metricValue = metric.metricValues.remove(0);
                int parameterIndex=1;
                preparedStatement.setString(parameterIndex++, metric.controllerHostname);
                preparedStatement.setString(parameterIndex++, metric.applicationName);
                preparedStatement.setString(parameterIndex++, fitToSize(metric.metricName, "metricname"));
                preparedStatement.setString(parameterIndex++, fitToSize(metric.metricPath, "metricpath"));
                preparedStatement.setString(parameterIndex++, metric.frequency);
                preparedStatement.setLong(parameterIndex++, metric.metricId);
                preparedStatement.setInt(parameterIndex++, (metricValue.useRange ? 1: 0 ));
                preparedStatement.setLong(parameterIndex++, metricValue.startTimeInMillis);
                preparedStatement.setLong(parameterIndex++, metricValue.occurrences);
                preparedStatement.setLong(parameterIndex++, metricValue.current);
                preparedStatement.setLong(parameterIndex++, metricValue.min);
                preparedStatement.setLong(parameterIndex++, metricValue.max);
                preparedStatement.setLong(parameterIndex++, metricValue.count);
                preparedStatement.setLong(parameterIndex++, metricValue.sum);
                preparedStatement.setLong(parameterIndex++, metricValue.value);
                preparedStatement.setDouble(parameterIndex++, metricValue.standardDeviation);
                preparedStatement.setLong(parameterIndex++, metricValue.startTimeInMillis);
                preparedStatement.addBatch();
                preparedStatement.clearParameters();
            }
            counter += preparedStatement.executeBatch().length;
        } catch (Exception exception) {
            logger.error("Error inserting metrics into %s, Exception: %s", name, exception.toString());
            throw new FailedDataLoadException( String.format("Error inserting metrics into %s, Exception: %s", name, exception.toString()), object);
        }
        return counter;
    }
}
