package com.cisco.josouthe.database.microsoft;

import com.cisco.josouthe.data.metric.BaselineData;
import com.cisco.josouthe.data.metric.BaselineTimeslice;
import com.cisco.josouthe.data.metric.MetricValue;
import com.cisco.josouthe.database.ColumnFeatures;
import com.cisco.josouthe.database.Database;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;


public class BaselineTable extends MicrosoftTable implements com.cisco.josouthe.database.BaselineTable {
    protected static final Logger logger = LogManager.getFormatterLogger();

    public BaselineTable(String tableName, Database database ) {
        super(tableName,"Metric Table",database);
        columns.put("controller", new ColumnFeatures("controller", "varchar", 50, false));
        columns.put("application", new ColumnFeatures("application", "varchar", 50, false));
        columns.put("metricname", new ColumnFeatures("metricName", "varchar", 200, false));
        columns.put("baseline", new ColumnFeatures("baseline", "varchar", 200, false));
        columns.put("frequency", new ColumnFeatures("frequency", "varchar", 50, false));
        columns.put("userange", new ColumnFeatures("userange", "numeric", -1, false));
        for( String columnName : new String[] { "metricid","startTimeInMillis", "occurrences", "currentValue", "min", "max", "count", "sum", "value", "standardDeviation"})
            columns.put(columnName.toLowerCase(), new ColumnFeatures(columnName, "numeric", -1, false));
        columns.put("startTimestamp", new ColumnFeatures("startTimestamp", "date", -1, false));
        this.initTable();
    }

    public int insert(Object object) {
        if( object == null ) {
            logger.warn("Can not insert a null Baseline Data Object!");
            return 0;
        }
        BaselineData baselineData = (BaselineData) object;
        int counter=0;
        StringBuilder insertSQL = new StringBuilder(String.format("insert into %s (",name));
        insertSQL.append("controller, application, metricname, baseline, frequency, metricid, userange, ");
        insertSQL.append("startTimeInMillis, occurrences, currentvalue, min, max, count, sum, value, standardDeviation, startTimestamp");
        insertSQL.append(") VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,TO_DATE('19700101','yyyymmdd') + ((?/1000)/24/60/60))");
        logger.trace("insertMetric SQL: %s",insertSQL);
        try ( Connection conn = database.getConnection(); PreparedStatement preparedStatement = conn.prepareStatement(insertSQL.toString());){
            //for(BaselineTimeslice baselineTimeslice : baselineData.dataTimeslices ) {
            while( !baselineData.dataTimeslices.isEmpty() ) {
                BaselineTimeslice baselineTimeslice = baselineData.dataTimeslices.remove(0);
                MetricValue metricValue = baselineTimeslice.metricValue;
                if( metricValue == null ) {
                    logger.warn("Metric Value in Baseline Timeslice is null! for metric %s timeslice %d", baselineData.metricName, baselineTimeslice.startTime);
                    continue;
                }
                int parameterIndex=1;
                preparedStatement.setString(parameterIndex++, fitToSize(baselineData.controllerHostname, "controller"));
                preparedStatement.setString(parameterIndex++, fitToSize(baselineData.applicationName, "application"));
                preparedStatement.setString(parameterIndex++, fitToSize(baselineData.metricName, "metricname"));
                preparedStatement.setString(parameterIndex++, fitToSize(baselineData.baseline.name, "baseline"));
                preparedStatement.setString(parameterIndex++, fitToSize(baselineData.frequency, "frequency"));
                preparedStatement.setLong(parameterIndex++, baselineData.metricId);
                preparedStatement.setInt(parameterIndex++, (metricValue.useRange ? 1: 0 ));
                preparedStatement.setLong(parameterIndex++, baselineTimeslice.startTime);
                preparedStatement.setLong(parameterIndex++, metricValue.occurrences);
                preparedStatement.setLong(parameterIndex++, metricValue.current);
                preparedStatement.setLong(parameterIndex++, metricValue.min);
                preparedStatement.setLong(parameterIndex++, metricValue.max);
                preparedStatement.setLong(parameterIndex++, metricValue.count);
                preparedStatement.setLong(parameterIndex++, metricValue.sum);
                preparedStatement.setLong(parameterIndex++, metricValue.value);
                preparedStatement.setDouble(parameterIndex++, metricValue.standardDeviation);
                preparedStatement.setLong(parameterIndex++, baselineTimeslice.startTime);
                preparedStatement.addBatch();
                preparedStatement.clearParameters();
            }
            counter += preparedStatement.executeBatch().length;
        } catch (Exception exception) {
            logger.error("Error inserting baseline into %s, Exception: %s", name, exception.toString());
        }
        return counter;
    }
}
