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
                createTableIfDoesNotExistOrColumnsAreMissing(table);
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
                createTableIfDoesNotExistOrColumnsAreMissing(table);
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
        int cntStarted = 0;
        int cntFinished = 0;
        for( MetricData metric : metricData ) {
            if( "METRIC DATA NOT FOUND".equals(metric.metricName) ) continue;
            cntStarted+=metric.metricValues.size();
            MetricTable table = getMetricTable(metric.targetTable);
            cntFinished += table.insert(metric);
        }
        logger.info("Attempted to load %d metrics, succeeded in loading %d metrics",cntStarted,cntFinished);
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
            long durationTimeTransaction = Utility.now() - startTimeTransaction;
            if( durationTimeTransaction > maxDurationTime ) maxDurationTime = durationTimeTransaction;
            if( durationTimeTransaction < minDurationTime ) minDurationTime = durationTimeTransaction;
            cntFinished += table.insert(event);
        }
        long durationTimeOverallMS = Utility.now() - startTimeOverall;
        logger.info("Attempted to load %d events, succeeded in loading %d events. Total Time %d(ms), Max Time %d(ms), Min Time %d(ms), Avg Time %d(ms)",cntStarted,cntFinished,durationTimeOverallMS, maxDurationTime, minDurationTime, durationTimeOverallMS/cntStarted);
    }

    private List<String> _createTableIfDoesNotExistOrColumnsAreMissingAlreadyDoneList = null;
    private void createTableIfDoesNotExistOrColumnsAreMissing(Table table) {
        if( _createTableIfDoesNotExistOrColumnsAreMissingAlreadyDoneList == null ) _createTableIfDoesNotExistOrColumnsAreMissingAlreadyDoneList = new ArrayList<>();
        if( _createTableIfDoesNotExistOrColumnsAreMissingAlreadyDoneList.contains(table.getName()) ) return;
        if( doesTableExist(table) ) {
            addMissingColumns(table);
        } else {
            createTable( table);
        }
        _createTableIfDoesNotExistOrColumnsAreMissingAlreadyDoneList.add(table.getName());
    }

    private void createTable(Table table) {
        Connection conn = null;
        String objectTypeName="UNKNOWN";
        //StringBuilder query = new StringBuilder(String.format("create table %s ( id serial NOT NULL, ",tableName));
        StringBuilder query = new StringBuilder(String.format("create table %s ( ",table.getName()));
        if( table instanceof MetricTable ) {
            objectTypeName="Metric Data";
            Iterator<ColumnFeatures> iterator = table.getColumns().values().iterator();
            while( iterator.hasNext() ) {
                ColumnFeatures column = iterator.next();
                query.append(String.format("%s %s",column.name, column.printConstraints()));
                if(iterator.hasNext()) query.append(", ");
            }
            query.append(")");
            //query.append(String.format(", constraint pk_%s primary key (id) )", tableName));
            logger.debug("create table query string: %s",query.toString());
        } else {
            logger.warn("Oops, unsupported table type for table %s",table.getName());
            return;
        }

        try{
            conn = DriverManager.getConnection( this.connectionString, this.user, this.password);
            Statement statement = conn.createStatement();
            statement.executeUpdate(query.toString());
        } catch (SQLException exception) {
            logger.error("Error creating new %s for %s data, SQL State: %s Exception: %s", table.getName(), objectTypeName, exception.getSQLState(), exception.toString());
        } finally {
            if( conn != null ) {
                try {
                    conn.close();
                } catch (SQLException ignored) {}
            }
        }
    }

    private void addMissingColumns(Table table) {
        Map<String,ColumnFeatures> columns = getMissingColumns(table);
        for( ColumnFeatures column : columns.values() ) {
            if( column.isMissing ) alterTableToAddColumn( table, column);
            if( column.isWrongNullable || column.isWrongType ) logger.warn("We can't fix this problem with the table %s.%s, wrong nullable: %s, wrong type: %s is %s but should be", table.getName(), column.name, column.isWrongNullable, column.isWrongType, column.type, table.getColumns().get(column.name).type);
            if( column.isWrongSize && !column.isWrongType ) {
                if( column.size < table.getColumns().get(column.name).size ) { //we can increase column size
                    alterTableToIncreaseColumnSize(table, column, table.getColumns().get(column.name).size);
                } else {
                    logger.info("We can't fix this problem with the table %s.%s, actual column size(%d) is larger than required(%d), this arguably isn't a problem now that i think about it",table.getName(),column.name, column.size, table.getColumns().get(column.name).size);
                }
            }
        }
    }

    private void alterTableToIncreaseColumnSize(Table table, ColumnFeatures column, int size) {
        Connection conn = null;
        try{
            conn = DriverManager.getConnection( this.connectionString, this.user, this.password);
            column.size = size;
            String query = String.format("alter table %s modify %s %s )", table.getName(), column.name, column.printConstraints());
            logger.debug("alterTableToIncreaseColumnSize query: %s",query);
            Statement statement = conn.createStatement();
            statement.executeUpdate(query);
        } catch (Exception exception) {
            logger.error("Error altering table to add column %s.%s, Exception: %s", table.getName(), column.name, exception.toString());
        } finally {
            if( conn != null ) {
                try {
                    conn.close();
                } catch (SQLException ignored) {}
            }
        }
    }

    private void alterTableToAddColumn(Table table, ColumnFeatures column) {
        Connection conn = null;
        try{
            conn = DriverManager.getConnection( this.connectionString, this.user, this.password);
            String query = String.format("alter table %s add ( %s %s )", table.getName(), column.name, column.printConstraints());
            logger.debug("alterTableToAddColumn query: %s",query);
            Statement statement = conn.createStatement();
            statement.executeUpdate(query);
        } catch (Exception exception) {
            logger.error("Error altering table to add column %s.%s, Exception: %s", table.getName(), column.name, exception.toString());
        } finally {
            if( conn != null ) {
                try {
                    conn.close();
                } catch (SQLException ignored) {}
            }
        }
    }

    private Map<String, ColumnFeatures> getMissingColumns(Table table) {
        Map<String,ColumnFeatures> columns = getTableColumns(table);
        if( table instanceof MetricTable ) {
            for( ColumnFeatures masterColumn : table.getColumns().values() ) {
                ColumnFeatures existingColumn = columns.get(masterColumn.name);
                if( existingColumn != null ) { //column exists, check all features
                    boolean foundADifference=false;
                    if( ! existingColumn.type.equals(masterColumn.type) ) {
                        existingColumn.isWrongType=true;
                        foundADifference=true;
                    }
                    if( existingColumn.size != masterColumn.size ) {
                        existingColumn.isWrongSize=true;
                        foundADifference=true;
                    }
                    if( existingColumn.isNull != masterColumn.isNull ) {
                        existingColumn.isWrongNullable=true;
                        foundADifference=true;
                    }
                    if( !foundADifference ) columns.remove(existingColumn.name);
                } else { //column is missing entirely, mark for creation
                    ColumnFeatures newColumn = masterColumn.clone();
                    newColumn.isMissing=true;
                    columns.put(newColumn.name, newColumn);
                }
            }
            return columns;
        }
        logger.warn("Unknown object type %s, can't check table %s for validity");
        return null;
    }

    private Map<String, ColumnFeatures> getTableColumns(Table table) {
        Map<String,ColumnFeatures> columns = new HashMap<>();
        Connection conn = null;
        try{
            conn = DriverManager.getConnection( this.connectionString, this.user, this.password);
            String query = String.format("select sys.all_tab_columns.column_name, sys.all_tab_columns.data_type, sys.all_tab_columns.data_length, sys.all_tab_columns.nullable\n" +
                    "from sys.all_tab_columns\n" +
                    "         left join sys.all_ind_columns\n" +
                    "                   on sys.all_ind_columns.index_owner = sys.all_tab_columns.owner\n" +
                    "                       and sys.all_ind_columns.table_name = sys.all_tab_columns.table_name\n" +
                    "                       and sys.all_ind_columns.column_name = sys.all_tab_columns.column_name\n" +
                    "         left join sys.all_indexes\n" +
                    "                   on sys.all_indexes.owner = sys.all_tab_columns.owner\n" +
                    "                       and sys.all_indexes.table_name = sys.all_tab_columns.table_name\n" +
                    "                       and sys.all_indexes.index_name = sys.all_ind_columns.index_name\n" +
                    "                       and sys.all_indexes.index_type = 'NORMAL'\n" +
                    "                       and sys.all_indexes.status = 'VALID'\n" +
                    "where lower(sys.all_tab_columns.table_name) like lower('%s')\n" +
                    "order by sys.all_tab_columns.column_id", table.getName());
            Statement statement = conn.createStatement();
            ResultSet resultSet = statement.executeQuery(query);
            while( resultSet.next() ) {
                String columnName = resultSet.getString(1);
                String columnType = resultSet.getString(2);
                int columnSize = resultSet.getInt(3);
                String columnNullable = resultSet.getString(4);
                ColumnFeatures columnFeatures = new ColumnFeatures(columnName, columnType, columnSize, ("N".equals(columnNullable) ? false : true));
                columns.put(columnFeatures.name,columnFeatures);
            }
        } catch (Exception exception) {
            logger.error("Error describing table %s, Exception: %s", table.getName(), exception.toString());
        } finally {
            if( conn != null ) {
                try {
                    conn.close();
                } catch (SQLException ignored) {}
            }
        }
        return columns;
    }

    private boolean doesTableExist(Table table) {
        Connection conn = null;
        try{
            conn = DriverManager.getConnection( this.connectionString, this.user, this.password);
            String query = String.format("select table_name from all_tables where lower(table_name) like lower('%s')", table.getName());
            Statement statement = conn.createStatement();
            ResultSet resultSet = statement.executeQuery(query);
            if( resultSet.next() ) {
                String table_name = resultSet.getString(1);
                logger.debug("doesTableExist(%s): Yes",table.getName());
                return true;
            } else {
                logger.debug("doesTableExist(%s): No it does not",table.getName());
                return false;
            }
        } catch (Exception exception) {
            logger.error("Error checking for database table existence, Exception: %s", exception.toString());
        } finally {
            if( conn != null ) {
                try {
                    conn.close();
                } catch (SQLException ignored) {}
            }
        }
        return false;
    }

}
