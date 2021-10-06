package com.cisco.josouthe.database;

import com.cisco.josouthe.data.EventData;
import com.cisco.josouthe.data.MetricData;
import com.cisco.josouthe.util.Utility;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;
import java.util.*;

public class Database {
    private static final Logger logger = LogManager.getFormatterLogger();

    String connectionString, user, password;
    Table defaultMetricTable, controlTable, defaulEventTable;
    Map<String,Table> tablesMap;

    public Database( String connectionString, String user, String password, String metricTable, String controlTable, String eventTable) {
        this.connectionString = connectionString;
        this.user = user;
        this.password = password;
        this.defaultMetricTable = new MetricTable(metricTable, this);
        this.controlTable = new ControlTable(controlTable, this);
        this.defaulEventTable = new EventTable(eventTable, this);
        this.tablesMap = new HashMap<>();
        logger.info("Testing Database connection returned: "+ isDatabaseAvailable());
    }

    private MetricTable getMetricTable( String name ) {
        if( name != null ) {
            if( ! this.tablesMap.containsKey(name) ) {
                Table table = new MetricTable(name, this);
                this.tablesMap.put(name,table);
            }
            return (MetricTable) this.tablesMap.get(name);
        }
        return (MetricTable) this.defaultMetricTable;
    }

    private EventTable getEventTable( String name ) {
        if( name != null  ) {
            if (!this.tablesMap.containsKey(name)) {
                Table table = new EventTable(name, this);
                this.tablesMap.put(name,table);
            }
            return (EventTable) this.tablesMap.get(name);
        }
        return (EventTable) this.defaulEventTable;
    }

    public boolean isDatabaseAvailable() {
        Connection conn = null;
        try{
            conn = DriverManager.getConnection( this.connectionString, this.user, this.password);
            if( conn != null ) return true;
        } catch (Exception exception) {
            logger.error("Error testing database connection settings, Exception: %s", exception.toString());
        } finally {
            if( conn != null ) {
                try {
                    conn.close();
                } catch (SQLException ignored) {}
            }
        }
        return false;
    }

    public void importMetricData(MetricData[] metricData) {
        logger.trace("Beginning of import metric data method");
        if( metricData == null || metricData.length == 0 ) {
            logger.debug("Nothing to import, leaving quickly");
            return;
        }
        int cntStarted = 0;
        int cntFinished = 0;
        int cntAverageCalc = 0;
        long startTimeOverall = Utility.now();
        long maxDurationTime = -1;
        long minDurationTime = Long.MAX_VALUE;
        for( MetricData metric : metricData ) {
            if( "METRIC DATA NOT FOUND".equals(metric.metricName) ) continue;
            cntStarted+=metric.metricValues.size();
            MetricTable table = getMetricTable(metric.targetTable);
            long startTimeTransaction = Utility.now();
            cntFinished += table.insert(metric);
            cntAverageCalc++;
            long durationTimeTransaction = Utility.now() - startTimeTransaction;
            if( durationTimeTransaction > maxDurationTime ) maxDurationTime = durationTimeTransaction;
            if( durationTimeTransaction < minDurationTime ) minDurationTime = durationTimeTransaction;
        }
        long durationTimeOverallMS = Utility.now() - startTimeOverall;
        logger.info("Attempted to load %d metrics, succeeded in loading %d metrics. Total Time %d(ms), Max Time %d(ms), Min Time %d(ms), Avg Time %d(ms)",cntStarted,cntFinished,durationTimeOverallMS, maxDurationTime, minDurationTime, durationTimeOverallMS/cntAverageCalc);

    }

    public void importEventData(EventData[] events) {
        logger.trace("Beginning of import event data method");
        if( events == null || events.length == 0 ) {
            logger.debug("Nothing to import, leaving quickly");
            return;
        }
        int cntStarted = 0;
        int cntFinished = 0;
        long startTimeOverall = Utility.now();
        long maxDurationTime = -1;
        long minDurationTime = Long.MAX_VALUE;
        for( EventData event : events ) {
            cntStarted++;
            long startTimeTransaction = Utility.now();
            EventTable table = getEventTable(event.targetTable);
            cntFinished += table.insert(event);
            long durationTimeTransaction = Utility.now() - startTimeTransaction;
            if( durationTimeTransaction > maxDurationTime ) maxDurationTime = durationTimeTransaction;
            if( durationTimeTransaction < minDurationTime ) minDurationTime = durationTimeTransaction;
        }
        long durationTimeOverallMS = Utility.now() - startTimeOverall;
        logger.info("Attempted to load %d events, succeeded in loading %d events. Total Time %d(ms), Max Time %d(ms), Min Time %d(ms), Avg Time %d(ms)",cntStarted,cntFinished,durationTimeOverallMS, maxDurationTime, minDurationTime, durationTimeOverallMS/cntStarted);
    }

    public Connection getConnection() throws SQLException { return DriverManager.getConnection( this.connectionString, this.user, this.password); }

}
