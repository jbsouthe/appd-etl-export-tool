package com.cisco.josouthe.database.csv;

import com.cisco.josouthe.Configuration;
import com.cisco.josouthe.data.analytic.Result;
import com.cisco.josouthe.database.ColumnFeatures;
import com.cisco.josouthe.database.Database;
import com.cisco.josouthe.database.Table;
import com.cisco.josouthe.exceptions.InvalidConfigurationException;
import com.cisco.josouthe.util.Utility;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;

public class CSVDatabase extends Database {
    private static final Logger logger = LogManager.getFormatterLogger();

    public static final String STRING_TYPE = "varchar2";
    public static final int STRING_SIZE = -1;
    public static final String INTEGER_TYPE = "number";
    public static final int INTEGER_SIZE = -1;
    public static final String FLOAT_TYPE = "number";
    public static final int FLOAT_SIZE = -1;
    public static final String DATE_TYPE = "date";
    public static final int DATE_SIZE = -1;
    public static final String BOOLEAN_TYPE = "varchar2";
    public static final int BOOLEAN_SIZE = 1;

    private File databaseDirectory;
    private char delimeter=',';

    public CSVDatabase(Configuration configuration, String connectionString, String metricTable, String controlTable, String eventTable, String baselineTable, Long firstRunHistoricNumberOfHours) throws InvalidConfigurationException {
        super( configuration, connectionString, "", "");
        if( ! "".equals(metricTable) && isValidDatabaseTableName(metricTable) ) logger.debug("Default Metric Table set to: %s", metricTable);
        if( ! "".equals(eventTable) && isValidDatabaseTableName(eventTable) ) logger.debug("Default Event Table set to: %s", eventTable);
        if( ! "".equals(baselineTable) && isValidDatabaseTableName(baselineTable) ) logger.debug("Default Baseline Table set to: %s", baselineTable);
        if( ! "".equals(controlTable) && isValidDatabaseTableName(controlTable) ) logger.debug("Run Control Table set to: %s", controlTable);
        this.databaseDirectory = new File(Utility.parseDatabasePath(connectionString));
        this.defaultMetricTable = new MetricTable(metricTable, this, this.databaseDirectory);
        this.controlTable = new ControlTable(controlTable, this, this.databaseDirectory);
        if( firstRunHistoricNumberOfHours != null ) ((ControlTable)this.controlTable).setDefaultLoadNumberOfHoursIfControlRowMissing(firstRunHistoricNumberOfHours.intValue());
        this.defaulEventTable = new EventTable(eventTable, this, this.databaseDirectory);
        this.defaultBaselineTable = new BaselineTable(baselineTable, this, this.databaseDirectory);
        logger.info("Testing Database connection returned: "+ isDatabaseAvailable());
    }

    @Override
    public Connection getConnection() throws SQLException {
        return null;
    }

    @Override
    public boolean isDatabaseAvailable() {
        if( (! this.databaseDirectory.exists() && ! this.databaseDirectory.mkdirs() ) || ! this.databaseDirectory.isDirectory() || ! this.databaseDirectory.canWrite() )
            return false;
        return true;
    }

    protected com.cisco.josouthe.database.MetricTable getMetricTable(String name ) {
        if( name != null ) {
            if( ! this.tablesMap.containsKey(name) ) {
                Table table = new com.cisco.josouthe.database.csv.MetricTable(name, this, this.databaseDirectory);
                this.tablesMap.put(name,table);
            }
            return (com.cisco.josouthe.database.MetricTable) this.tablesMap.get(name);
        }
        return (com.cisco.josouthe.database.MetricTable) this.defaultMetricTable;
    }

    protected com.cisco.josouthe.database.BaselineTable getBaselineTable(String name ) {
        if( name != null ) {
            if( ! this.tablesMap.containsKey(name) ) {
                Table table = new com.cisco.josouthe.database.csv.BaselineTable(name, this, this.databaseDirectory);
                this.tablesMap.put(name,table);
            }
            return (com.cisco.josouthe.database.BaselineTable) this.tablesMap.get(name);
        }
        return (com.cisco.josouthe.database.BaselineTable) this.defaultBaselineTable;
    }

    protected com.cisco.josouthe.database.EventTable getEventTable(String name ) {
        if( name != null  ) {
            if (!this.tablesMap.containsKey(name)) {
                Table table = new com.cisco.josouthe.database.csv.EventTable(name, this, this.databaseDirectory);
                this.tablesMap.put(name,table);
            }
            return (com.cisco.josouthe.database.EventTable) this.tablesMap.get(name);
        }
        return (com.cisco.josouthe.database.EventTable) this.defaulEventTable;
    }

    protected com.cisco.josouthe.database.AnalyticTable getAnalyticTable(Result result ) {
        if( !this.tablesMap.containsKey(result.targetTable) ) {
            com.cisco.josouthe.database.AnalyticTable analyticTable = new AnalyticTable( result, this, this.databaseDirectory);
            this.tablesMap.put(result.targetTable, (Table) analyticTable);
        }
        return (com.cisco.josouthe.database.AnalyticTable) this.tablesMap.get(result.targetTable);
    }


    @Override
    public String convertToAcceptableColumnName(String label, Collection<ColumnFeatures> existingColumns) {
        return label.replace(delimeter, '_');
    }

    @Override
    public boolean isValidDatabaseTableName(String tableName) throws InvalidConfigurationException {
        if( tableName.contains(".") || tableName.contains("/"))
            throw new InvalidConfigurationException(String.format("Database File Name '%s' invalid!", tableName) );
        return true;
    }
}
