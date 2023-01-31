package com.cisco.josouthe.database.mysql;

import com.cisco.josouthe.config.Configuration;
import com.cisco.josouthe.data.analytic.Result;
import com.cisco.josouthe.database.*;
import com.cisco.josouthe.exceptions.InvalidConfigurationException;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public class MySQLDatabase extends Database {
    private static final Logger logger = LogManager.getFormatterLogger();

    public static final String STRING_TYPE = "varchar";
    public static final int STRING_SIZE = 120;
    public static final String INTEGER_TYPE = "bigint";
    public static final int INTEGER_SIZE = 20;
    public static final String DATE_TYPE = "timestamp";
    public static final int DATE_SIZE = -1;
    public static final String FLOAT_TYPE = "decimal";
    public static final int FLOAT_SIZE = 20;
    public static final String BOOLEAN_TYPE = "tinyint";
    public static final int BOOLEAN_SIZE = 1;

    private HikariConfig hikariConfig;
    private HikariDataSource dataSource;


    public MySQLDatabase(Configuration configuration, String connectionString, String user, String password, String metricTable, String controlTable, String eventTable, String baselineTable, Long firstRunHistoricNumberOfHours) throws InvalidConfigurationException {
        super( configuration, connectionString, user, password);
        this.hikariConfig = new HikariConfig();
        this.hikariConfig.setJdbcUrl(connectionString);
        this.hikariConfig.setUsername(user);
        this.hikariConfig.setPassword(password);
        this.hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
        this.hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
        this.hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        this.hikariConfig.addDataSourceProperty("maximumPoolSize", this.configuration.getProperty("scheduler-NumberOfDatabaseThreads", "10"));
        this.hikariConfig.addDataSourceProperty("connectionTimeout", "60000");
        this.hikariConfig.addDataSourceProperty("leakDetectionThreshold", "35000");
        this.dataSource = new HikariDataSource(this.hikariConfig);
        if( ! "".equals(metricTable) && isValidDatabaseTableName(metricTable) ) {
            logger.debug("Default Metric Table set to: %s", metricTable);
            this.defaultMetricTable = new MetricTable(metricTable, this);
        }
        if( ! "".equals(eventTable) && isValidDatabaseTableName(eventTable) ) {
            logger.debug("Default Event Table set to: %s", eventTable);
            this.defaulEventTable = new EventTable(eventTable, this);
        }
        if( ! "".equals(baselineTable) && isValidDatabaseTableName(baselineTable) ) {
            logger.debug("Default Baseline Table set to: %s", baselineTable);
            this.defaultBaselineTable = new BaselineTable(baselineTable, this);
        }
        if( ! "".equals(controlTable) && isValidDatabaseTableName(controlTable) ) {
            logger.debug("Run Control Table set to: %s", controlTable);
            this.controlTable = new ControlTable(controlTable, this);
        }
        if( firstRunHistoricNumberOfHours != null ) ((ControlTable)this.controlTable).setDefaultLoadNumberOfHoursIfControlRowMissing(firstRunHistoricNumberOfHours.intValue());
        this.tablesMap = new HashMap<>();
        logger.info("Testing Database connection returned: "+ isDatabaseAvailable());
    }
    public ControlTable getControlTable() { return (ControlTable) controlTable; }

    public boolean isDatabaseAvailable() {
        try (Connection conn = getConnection(); ){
            if( conn != null ) return true;
        } catch (Exception exception) {
            logger.error("Error testing database connection settings, Exception: %s", exception.toString());
        }
        return false;
    }

    protected IMetricTable getMetricTable(String name ) {
        if( name != null ) {
            if( ! this.tablesMap.containsKey(name) ) {
                Table table = new MetricTable(name, this);
                this.tablesMap.put(name,table);
            }
            return (IMetricTable) this.tablesMap.get(name);
        }
        return (IMetricTable) this.defaultMetricTable;
    }

    protected IBaselineTable getBaselineTable(String name ) {
        if( name != null ) {
            if( ! this.tablesMap.containsKey(name) ) {
                Table table = new BaselineTable(name, this);
                this.tablesMap.put(name,table);
            }
            return (IBaselineTable) this.tablesMap.get(name);
        }
        return (IBaselineTable) this.defaultBaselineTable;
    }

    protected IEventTable getEventTable(String name ) {
        if( name != null  ) {
            if (!this.tablesMap.containsKey(name)) {
                Table table = new EventTable(name, this);
                this.tablesMap.put(name,table);
            }
            return (IEventTable) this.tablesMap.get(name);
        }
        return (IEventTable) this.defaulEventTable;
    }

    protected IAnalyticTable getAnalyticTable(Result result ) {
        if( !this.tablesMap.containsKey(result.targetTable) ) {
            IAnalyticTable analyticTable = new AnalyticTable( result, this);
            this.tablesMap.put(result.targetTable, (Table) analyticTable);
        }
        return (IAnalyticTable) this.tablesMap.get(result.targetTable);
    }
    public Connection getConnection() throws SQLException {
        logger.trace("Getting Connection to DB for user %s",this.user);
        int tries=0;
        boolean succeeded = false;
        Connection connection = null;
        while( !succeeded && tries < 30 ) {
            try {
                tries++;
                connection = this.dataSource.getConnection();
                succeeded=true;
            } catch (SQLException sqlException) {
                logger.warn("Error getting a connection try %d: %s", tries ,sqlException.toString());
            }
        }
        if(!succeeded) {
            throw new SQLException(String.format("Could not get a database connection! in %d tries", tries));
        }
        return connection;
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
        if( tableName.length() > 64 ) throw new InvalidConfigurationException(String.format("Database table name longer than max 64 characters, by like %d characters. change this name: %s",(tableName.length()-64),tableName));
        if(! Character.isAlphabetic(tableName.charAt(0))) throw new InvalidConfigurationException(String.format("Database table name must begin with an alphabetic character, not this thing '%s' change this table: %s",tableName.charAt(0),tableName));
        //long countSpecialCharacters = tableName.chars().filter(ch -> ch == '#' || ch == '$' || ch == '_').count();
        //long countInvalidCharacters = tableName.chars().filter(ch -> ch != '#' && ch != '$' && ch != '_' && ! Character.isAlphabetic(ch) && ! Character.isDigit(ch)).count();
        long countInvalidCharacters = tableName.chars().filter(ch -> ch != '_' && ! Character.isAlphabetic(ch) && ! Character.isDigit(ch)).count();
        //if( countSpecialCharacters > 1 || countInvalidCharacters > 0 ) throw new InvalidConfigurationException(String.format("Only one of the special characters ($, #, or _) is allowed and the rest must be alphanumeric only, not my rules, (shrug), change this table name: %s",tableName));
        //if( countInvalidCharacters > 0 ) throw new InvalidConfigurationException(String.format("Only one of the special characters ('_') is allowed and the rest must be alphanumeric only, change this table name: %s",tableName));
        if( _tableNameForbiddenWords.contains(tableName.toLowerCase()) ) throw new InvalidConfigurationException(String.format("The table name is a reserved word for this database vendor %s, please picking something different than: %s",this.vendorName, tableName));
        return true; //if we made it this far without an exception, we are good to go
    }

    @Override
    public String convertToAcceptableTableName(String tableName) {
        tableName = tableName.replace('.', '_');
        if(! Character.isAlphabetic(tableName.charAt(0))) tableName = "a"+tableName;
        StringBuilder result = new StringBuilder();
        tableName.chars().filter( ch -> ch == '_' || Character.isAlphabetic(ch) || Character.isDigit(ch)).forEach( ch -> result.append((char) ch));
        try {
            if (_tableNameForbiddenWords == null) loadTableNameInvalidWords();
            if (_tableNameForbiddenWords.contains(result.toString().toLowerCase())) result.append("_c");
        } catch (InvalidConfigurationException e) {
            logger.warn("Was not able to load list of table name forbidden words: %s", e.getMessage());
        }
        tableName = result.toString();
        if( tableName.length() > 30 )
            tableName = tableName.substring(0,30);
        return tableName;
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
