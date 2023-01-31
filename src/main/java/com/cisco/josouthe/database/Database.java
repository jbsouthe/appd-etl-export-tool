package com.cisco.josouthe.database;

import com.cisco.josouthe.config.Configuration;
import com.cisco.josouthe.data.analytic.Result;
import com.cisco.josouthe.data.event.EventData;
import com.cisco.josouthe.data.metric.BaselineData;
import com.cisco.josouthe.data.metric.MetricData;
import com.cisco.josouthe.exceptions.FailedDataLoadException;
import com.cisco.josouthe.exceptions.InvalidConfigurationException;
import com.cisco.josouthe.util.Utility;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public abstract class Database {
    private static final Logger logger = LogManager.getFormatterLogger();

    public static final String STRING_TYPE = "varchar2";
    public static final int STRING_SIZE = 120;
    public static final String INTEGER_TYPE = "number";
    public static final int INTEGER_SIZE = 22;
    public static final String FLOAT_TYPE = "number";
    public static final int FLOAT_SIZE = 22;
    public static final String DATE_TYPE = "date";
    public static final int DATE_SIZE = -1;
    public static final String BOOLEAN_TYPE = "number";
    public static final int BOOLEAN_SIZE = 1;
    public static final int MAX_CONNECTION_RETRY = 5;

    protected Configuration configuration;
    protected String connectionString, user, password, vendorName;
    protected Table defaultMetricTable, controlTable, defaulEventTable, defaultBaselineTable;
    protected Map<String,Table> tablesMap = new HashMap<>();

    public Database( Configuration configuration, String connectionString, String user, String password ) {
        this.configuration = configuration;
        this.connectionString = connectionString;
        this.user = user;
        this.password = password;
        this.vendorName = Utility.parseDatabaseVendor(connectionString);
    }


    public IControlTable getControlTable() { return (IControlTable) controlTable; }
    protected abstract IAnalyticTable getAnalyticTable(Result result );
    protected abstract IEventTable getEventTable(String name );
    protected abstract IMetricTable getMetricTable(String name );
    protected abstract IBaselineTable getBaselineTable(String name );

    public abstract boolean isDatabaseAvailable();

    public void importData( Object[] someData ) throws FailedDataLoadException {
        if( someData == null || someData.length == 0 ) return;
        if( someData instanceof MetricData[] ) {
            importMetricData((MetricData[]) someData);
        } else if( someData instanceof EventData[] ) {
            importEventData((EventData[]) someData);
        } else if( someData instanceof Result[] ) {
            importAnalyticData((Result[]) someData);
        } else if( someData instanceof BaselineData[] ) {
            importBaselineData((BaselineData[]) someData);
        } else {
            logger.warn("Could not determine the datatype for %s %d records",someData,someData.length);
        }
    }

    public void importMetricData(MetricData[] metricData) throws FailedDataLoadException {
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
        for( MetricData metric : metricData ) { //TODO pop a data element off the array on success, otherwise leave the data in the array, in case of exception
            if( "METRIC DATA NOT FOUND".equals(metric.metricName) ) continue;
            cntStarted+=metric.metricValues.size();
            IMetricTable table = (IMetricTable) getMetricTable(metric.targetTable);
            long startTimeTransaction = Utility.now();
            cntFinished += table.insert(metric);
            cntAverageCalc++;
            long durationTimeTransaction = Utility.now() - startTimeTransaction;
            if( durationTimeTransaction > maxDurationTime ) maxDurationTime = durationTimeTransaction;
            if( durationTimeTransaction < minDurationTime ) minDurationTime = durationTimeTransaction;
            logger.debug("Loaded %d metrics into the database in time %d(ms)", metric.metricValues.size(), durationTimeTransaction);
        }
        long durationTimeOverallMS = Utility.now() - startTimeOverall;
        if( cntStarted > 0 )
            logger.info("Attempted to load %d metrics, succeeded in loading %d metrics. Total Time %d(ms), Max Time %d(ms), Min Time %d(ms), Avg Time %d(ms)",cntStarted,cntFinished,durationTimeOverallMS, maxDurationTime, minDurationTime, (cntStarted>0 ?durationTimeOverallMS/cntStarted : -1));

    }

    public void importBaselineData(BaselineData[] baselineData) throws FailedDataLoadException {
        logger.trace("Beginning of import metric data method");
        if( baselineData == null || baselineData.length == 0 ) {
            logger.debug("Nothing to import, leaving quickly");
            return;
        }
        int cntStarted = 0;
        int cntFinished = 0;
        int cntAverageCalc = 0;
        long startTimeOverall = Utility.now();
        long maxDurationTime = -1;
        long minDurationTime = Long.MAX_VALUE;
        for( BaselineData baseline : baselineData ) {
            cntStarted+=baseline.dataTimeslices.size();
            IBaselineTable table = (IBaselineTable) getBaselineTable(baseline.targetTable);
            long startTimeTransaction = Utility.now();
            cntFinished += table.insert(baseline);
            cntAverageCalc++;
            long durationTimeTransaction = Utility.now() - startTimeTransaction;
            if( durationTimeTransaction > maxDurationTime ) maxDurationTime = durationTimeTransaction;
            if( durationTimeTransaction < minDurationTime ) minDurationTime = durationTimeTransaction;
            logger.debug("Loaded %d baseline metrics into the database in time %d(ms)", baseline.dataTimeslices.size(), durationTimeTransaction);
        }
        long durationTimeOverallMS = Utility.now() - startTimeOverall;
        if( cntStarted > 0 )
            logger.info("Attempted to load %d baseline metrics, succeeded in loading %d baseline metrics. Total Time %d(ms), Max Time %d(ms), Min Time %d(ms), Avg Time %d(ms)",cntStarted,cntFinished,durationTimeOverallMS, maxDurationTime, minDurationTime, (cntStarted>0 ?durationTimeOverallMS/cntStarted : -1));

    }

    public void importEventData(EventData[] events) throws FailedDataLoadException{
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
            IEventTable table = (IEventTable) getEventTable(event.targetTable);
            cntFinished += table.insert(event);
            long durationTimeTransaction = Utility.now() - startTimeTransaction;
            if( durationTimeTransaction > maxDurationTime ) maxDurationTime = durationTimeTransaction;
            if( durationTimeTransaction < minDurationTime ) minDurationTime = durationTimeTransaction;
            logger.debug("Loaded %d event into the database in time %d(ms)", 1, durationTimeTransaction);
        }
        long durationTimeOverallMS = Utility.now() - startTimeOverall;
        logger.info("Attempted to load %d events, succeeded in loading %d events. Total Time %d(ms), Max Time %d(ms), Min Time %d(ms), Avg Time %d(ms)",cntStarted,cntFinished,durationTimeOverallMS, maxDurationTime, minDurationTime,  (cntStarted>0 ?durationTimeOverallMS/cntStarted : -1));
    }

    public void importAnalyticData(Result[] results) throws FailedDataLoadException{
        logger.trace("Begining of import analytics search results method");
        if( results == null || results.length == 0 ) {
            logger.debug("Nothing to import, leaving quickly");
            return;
        }
        int cntStarted = 0;
        int cntFinished = 0;
        long startTimeOverall = Utility.now();
        long maxDurationTime = -1;
        long minDurationTime = Long.MAX_VALUE;
        for( Result result : results ) {
            if( result.results == null ) continue;
            cntStarted+=result.results.length;
            long startTimeTransaction = Utility.now();
            IAnalyticTable table = (IAnalyticTable) getAnalyticTable(result);
            cntFinished += table.insert(result);
            long durationTimeTransaction = Utility.now() - startTimeTransaction;
            if( durationTimeTransaction > maxDurationTime ) maxDurationTime = durationTimeTransaction;
            if( durationTimeTransaction < minDurationTime ) minDurationTime = durationTimeTransaction;
            logger.debug("Loaded %d analytic search results into the database in time %d(ms)", result.results.length, durationTimeTransaction);
        }
        long durationTimeOverallMS = Utility.now() - startTimeOverall;
        if( maxDurationTime != minDurationTime ) {
            logger.info(
                    "Attempted to load %d analytic search results, succeeded in loading %d rows. Total Time %d(ms), Max Time %d(ms), Min Time %d(ms), Avg Time %d(ms)",
                    cntStarted, cntFinished, durationTimeOverallMS, maxDurationTime, minDurationTime,
                    (cntStarted > 0 ? durationTimeOverallMS / cntStarted : -1));
        } else {
            logger.info(
                    "Attempted to load %d analytic search results, succeeded in loading %d rows. Total Time %d(ms)",
                    cntStarted, cntFinished, durationTimeOverallMS);
        }
    }

    public abstract Connection getConnection() throws SQLException;

    public abstract String convertToAcceptableColumnName(String label, Collection<ColumnFeatures> existingColumns);

    public abstract boolean isValidDatabaseTableName( String tableName ) throws InvalidConfigurationException;

    public abstract String convertToAcceptableTableName(String tableName );
}
