package com.cisco.josouthe.database.oracle;

import com.cisco.josouthe.data.analytic.Result;
import com.cisco.josouthe.data.event.EventData;
import com.cisco.josouthe.data.metric.MetricData;
import com.cisco.josouthe.database.ColumnFeatures;
import com.cisco.josouthe.database.Database;
import com.cisco.josouthe.database.Table;
import com.cisco.josouthe.exceptions.InvalidConfigurationException;
import com.cisco.josouthe.util.Utility;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.*;

public class OracleDatabase extends Database {
    private static final Logger logger = LogManager.getFormatterLogger();

    public static final String STRING_TYPE = "varchar2";
    public static final int STRING_SIZE = 120;
    public static final String INTEGER_TYPE = "number";
    public static final int INTEGER_SIZE = 22;
    public static final String DATE_TYPE = "date";
    public static final int DATE_SIZE = -1;


    public OracleDatabase(String connectionString, String user, String password, String metricTable, String controlTable, String eventTable, Long firstRunHistoricNumberOfHours) throws InvalidConfigurationException {
        super( connectionString, user, password);
        if( ! "".equals(metricTable) && isValidDatabaseTableName(metricTable) ) logger.debug("Default Metric Table set to: %s", metricTable);
        if( ! "".equals(eventTable) && isValidDatabaseTableName(eventTable) ) logger.debug("Default Event Table set to: %s", eventTable);
        if( ! "".equals(controlTable) && isValidDatabaseTableName(controlTable) ) logger.debug("Run Control Table set to: %s", controlTable);
        this.defaultMetricTable = new MetricTable(metricTable, this);
        this.controlTable = new ControlTable(controlTable, this);
        if( firstRunHistoricNumberOfHours != null ) ((ControlTable)this.controlTable).setDefaultLoadNumberOfHoursIfControlRowMissing(firstRunHistoricNumberOfHours.intValue());
        this.defaulEventTable = new EventTable(eventTable, this);
        this.tablesMap = new HashMap<>();
        logger.info("Testing Database connection returned: "+ isDatabaseAvailable());
    }

    public ControlTable getControlTable() { return (ControlTable) controlTable; }

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

    protected com.cisco.josouthe.database.MetricTable getMetricTable(String name ) {
        if( name != null ) {
            if( ! this.tablesMap.containsKey(name) ) {
                Table table = new MetricTable(name, this);
                this.tablesMap.put(name,table);
            }
            return (com.cisco.josouthe.database.MetricTable) this.tablesMap.get(name);
        }
        return (com.cisco.josouthe.database.MetricTable) this.defaultMetricTable;
    }

    protected com.cisco.josouthe.database.EventTable getEventTable(String name ) {
        if( name != null  ) {
            if (!this.tablesMap.containsKey(name)) {
                Table table = new EventTable(name, this);
                this.tablesMap.put(name,table);
            }
            return (com.cisco.josouthe.database.EventTable) this.tablesMap.get(name);
        }
        return (com.cisco.josouthe.database.EventTable) this.defaulEventTable;
    }

    protected com.cisco.josouthe.database.AnalyticTable getAnalyticTable(Result result ) {
        if( !this.tablesMap.containsKey(result.targetTable) ) {
            com.cisco.josouthe.database.AnalyticTable analyticTable = new AnalyticTable( result, this);
            this.tablesMap.put(result.targetTable, (Table) analyticTable);
        }
        return (com.cisco.josouthe.database.AnalyticTable) this.tablesMap.get(result.targetTable);
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
            MetricTable table = (MetricTable) getMetricTable(metric.targetTable);
            long startTimeTransaction = Utility.now();
            cntFinished += table.insert(metric);
            cntAverageCalc++;
            long durationTimeTransaction = Utility.now() - startTimeTransaction;
            if( durationTimeTransaction > maxDurationTime ) maxDurationTime = durationTimeTransaction;
            if( durationTimeTransaction < minDurationTime ) minDurationTime = durationTimeTransaction;
            logger.debug("Loaded %d metrics into the database in time %d(ms)", metric.metricValues.size(), durationTimeTransaction);
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
            EventTable table = (EventTable) getEventTable(event.targetTable);
            cntFinished += table.insert(event);
            long durationTimeTransaction = Utility.now() - startTimeTransaction;
            if( durationTimeTransaction > maxDurationTime ) maxDurationTime = durationTimeTransaction;
            if( durationTimeTransaction < minDurationTime ) minDurationTime = durationTimeTransaction;
            logger.debug("Loaded %d event into the database in time %d(ms)", 1, durationTimeTransaction);
        }
        long durationTimeOverallMS = Utility.now() - startTimeOverall;
        logger.info("Attempted to load %d events, succeeded in loading %d events. Total Time %d(ms), Max Time %d(ms), Min Time %d(ms), Avg Time %d(ms)",cntStarted,cntFinished,durationTimeOverallMS, maxDurationTime, minDurationTime, durationTimeOverallMS/cntStarted);
    }

    public Connection getConnection() throws SQLException { return DriverManager.getConnection( this.connectionString, this.user, this.password); }

    public void importAnalyticData(Result[] results) {
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
            cntStarted+=result.results.length;
            long startTimeTransaction = Utility.now();
            AnalyticTable table = (AnalyticTable) getAnalyticTable(result);
            cntFinished += table.insert(result);
            long durationTimeTransaction = Utility.now() - startTimeTransaction;
            if( durationTimeTransaction > maxDurationTime ) maxDurationTime = durationTimeTransaction;
            if( durationTimeTransaction < minDurationTime ) minDurationTime = durationTimeTransaction;
            logger.debug("Loaded %d analytic search results into the database in time %d(ms)", result.results.length, durationTimeTransaction);
        }
        long durationTimeOverallMS = Utility.now() - startTimeOverall;
        logger.info("Attempted to load %d analytic search results, succeeded in loading %d rows. Total Time %d(ms), Max Time %d(ms), Min Time %d(ms), Avg Time %d(ms)",cntStarted,cntFinished,durationTimeOverallMS, maxDurationTime, minDurationTime, durationTimeOverallMS/cntStarted);
    }

    @Override
    public String convertToAcceptableColumnName(String label, Collection<ColumnFeatures> existingColumns) {
        if( label == null ) return null;
        if(_tableNameForbiddenWords == null) {
            try {
                loadTableNameInvalidWords();
            } catch (InvalidConfigurationException e) {
                logger.warn("Error Loading reserved word list for this %s database: %s", this.vendorName, e.getMessage());
                return label;
            }
        }
        label = label.replace('.', '_');
        if(! Character.isAlphabetic(label.charAt(0))) label = "a"+label;
        StringBuilder result = new StringBuilder();
        label.chars().filter( ch -> ch == '_' || Character.isAlphabetic(ch) || Character.isDigit(ch)).forEach( ch -> result.append((char) ch));
        if( _tableNameForbiddenWords.contains(result.toString().toLowerCase()) ) result.append("_c");
        if( containsColumnAlready(result.toString(), existingColumns) ) result.append("_2");
        if( result.length() > 30 ) {
            String shorterLabel = result.substring(0,30);
            if( _tableNameForbiddenWords.contains(shorterLabel.toLowerCase()) ) {
                shorterLabel = shorterLabel.substring(0,28)+"_c"; //this could only happen if a reserved word is 30 characters long!
            }
            logger.trace("Setting database column name to short version: %s",shorterLabel);
            if( containsColumnAlready(shorterLabel, existingColumns) ) shorterLabel = shorterLabel.substring(0,28) + "_2";
            return shorterLabel;
        }
        logger.trace("Returning database column name: %s for field label %s", result, label);
        return result.toString();
    }

    private boolean containsColumnAlready(String name, Collection<ColumnFeatures> existingColumns) {
        for( ColumnFeatures columnFeatures : existingColumns)
            if( columnFeatures.name.toLowerCase().equals(name.toLowerCase()) ) return true;
        return false;
    }

    private List<String> _tableNameForbiddenWords = null;
    public boolean isValidDatabaseTableName( String tableName ) throws InvalidConfigurationException {
        if( tableName == null ) return false;
        if(_tableNameForbiddenWords == null) loadTableNameInvalidWords();
        if( tableName.length() > 30 ) throw new InvalidConfigurationException(String.format("Database table name longer than max 30 characters, by like %d characters. change this name: %s",(tableName.length()-30),tableName));
        if(! Character.isAlphabetic(tableName.charAt(0))) throw new InvalidConfigurationException(String.format("Database table name must begin with an alphabetic character, not this thing '%s' change this table: %s",tableName.charAt(0),tableName));
        //long countSpecialCharacters = tableName.chars().filter(ch -> ch == '#' || ch == '$' || ch == '_').count();
        //long countInvalidCharacters = tableName.chars().filter(ch -> ch != '#' && ch != '$' && ch != '_' && ! Character.isAlphabetic(ch) && ! Character.isDigit(ch)).count();
        long countInvalidCharacters = tableName.chars().filter(ch -> ch != '_' && ! Character.isAlphabetic(ch) && ! Character.isDigit(ch)).count();
        //if( countSpecialCharacters > 1 || countInvalidCharacters > 0 ) throw new InvalidConfigurationException(String.format("Only one of the special characters ($, #, or _) is allowed and the rest must be alphanumeric only, not my rules, (shrug), change this table name: %s",tableName));
        if( countInvalidCharacters > 0 ) throw new InvalidConfigurationException(String.format("Only one of the special characters ('_') is allowed and the rest must be alphanumeric only, change this table name: %s",tableName));
        if( _tableNameForbiddenWords.contains(tableName.toLowerCase()) ) throw new InvalidConfigurationException(String.format("The table name is a reserved word for this database vendor %s, please picking something different than: %s",this.vendorName, tableName));
        return true; //if we made it this far without an exception, we are good to go
    }

    private void loadTableNameInvalidWords() throws InvalidConfigurationException {
        String filename = this.vendorName.toLowerCase() + "-forbidden-words.txt";
        logger.debug("Loading file: %s",filename);
        try (BufferedReader reader = new BufferedReader( new InputStreamReader(getClass().getClassLoader().getResourceAsStream(filename)))) {
            _tableNameForbiddenWords = new ArrayList<>();
            String word;
            while ( (word = reader.readLine()) != null )
                _tableNameForbiddenWords.add(word.toLowerCase());
        } catch (IOException e) {
            logger.warn("Error reading list of forbidden table names from internal file %s, this database may not be supported: %s Exception: %s", filename, this.vendorName, e.getMessage());
            throw new InvalidConfigurationException(String.format("Error reading list of forbidden table names from internal file %s, this database may not be supported: %s", filename, this.vendorName));
        }
    }
}
