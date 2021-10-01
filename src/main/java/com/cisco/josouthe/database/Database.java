package com.cisco.josouthe.database;

import com.cisco.josouthe.data.MetricData;
import com.cisco.josouthe.data.MetricValue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;
import java.util.*;

public class Database {
    private static final Logger logger = LogManager.getFormatterLogger();

    String connectionString, user, password, defaultTable;
    Map<String,ColumnFeatures> metricDataDatabaseColumns = null;
    Map<String,ColumnFeatures> analyticsDataDatabaseColumns = null;

    public Database( String connectionString, String user, String password, String defaultTable) {
        this.connectionString = connectionString;
        this.user = user;
        this.password = password;
        this.defaultTable = defaultTable;
        metricDataDatabaseColumns = new HashMap<>();
        metricDataDatabaseColumns.put("controller", new ColumnFeatures("controller", "varchar2", 50, false));
        metricDataDatabaseColumns.put("application", new ColumnFeatures("application", "varchar2", 50, false));
        metricDataDatabaseColumns.put("metricname", new ColumnFeatures("metricName", "varchar2", 200, false));
        metricDataDatabaseColumns.put("metricpath", new ColumnFeatures("metricPath", "varchar2", 200, false));
        metricDataDatabaseColumns.put("frequency", new ColumnFeatures("frequency", "varchar2", 50, false));
        metricDataDatabaseColumns.put("userange", new ColumnFeatures("userange", "number", 22, false));
        for( String columnName : new String[] { "metricid","startTimeInMillis", "occurrences", "currentValue", "min", "max", "count", "sum", "value", "standardDeviation"})
            metricDataDatabaseColumns.put(columnName.toLowerCase(), new ColumnFeatures(columnName, "number", 22, false));

        analyticsDataDatabaseColumns = new HashMap<>();
        logger.info("Testing Database connection returned: "+ isDatabaseAvailable());
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
            String table = metric.targetTable;
            if( table == null ) table = defaultTable;
            createTableIfDoesNotExistOrColumnsAreMissing(table, metric);
            cntFinished += insertMetric(table, metric);
        }
        logger.info("Attempted to load %d metrics, succeeded in loading %d metrics",cntStarted,cntFinished);
    }

    private int insertMetric(String table, MetricData metric) {
        int counter=0;
        StringBuilder insertSQL = new StringBuilder(String.format("insert into %s (",table));
        insertSQL.append("controller, application, metricname, metricpath, frequency, metricid, userange, ");
        insertSQL.append("startTimeInMillis, occurrences, currentvalue, min, max, count, sum, value, standardDeviation");
        insertSQL.append(") VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
        logger.debug("insertMetric SQL: %s",insertSQL);
        Connection conn = null;
        try{
            conn = DriverManager.getConnection( this.connectionString, this.user, this.password);
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
                counter += preparedStatement.executeUpdate();
            }
        } catch (Exception exception) {
            logger.error("Error inserting metrics into %s, Exception: %s", table, exception.toString());
        } finally {
            if( conn != null ) {
                try {
                    conn.close();
                } catch (SQLException ignored) {}
            }
        }
        return counter;
    }

    private List<String> _createTableIfDoesNotExistOrColumnsAreMissingAlreadyDoneList = null;
    private void createTableIfDoesNotExistOrColumnsAreMissing(String table, Object object) {
        if( _createTableIfDoesNotExistOrColumnsAreMissingAlreadyDoneList == null ) _createTableIfDoesNotExistOrColumnsAreMissingAlreadyDoneList = new ArrayList<>();
        if( _createTableIfDoesNotExistOrColumnsAreMissingAlreadyDoneList.contains(table) ) return;
        if( doesTableExist(table) ) {
            addMissingColumns(table, object);
        } else {
            createTable( table, object );
        }
        _createTableIfDoesNotExistOrColumnsAreMissingAlreadyDoneList.add(table);
    }

    private void createTable(String tableName, Object object) {
        Connection conn = null;
        String objectTypeName="UNKNOWN";
        //StringBuilder query = new StringBuilder(String.format("create table %s ( id serial NOT NULL, ",tableName));
        StringBuilder query = new StringBuilder(String.format("create table %s ( ",tableName));
        if( object instanceof MetricData ) {
            objectTypeName="Metric Data";
            Iterator<ColumnFeatures> iterator = metricDataDatabaseColumns.values().iterator();
            while( iterator.hasNext() ) {
                ColumnFeatures column = iterator.next();
                query.append(String.format("%s %s",column.name, column.printConstraints()));
                if(iterator.hasNext()) query.append(", ");
            }
            query.append(")");
            //query.append(String.format(", constraint pk_%s primary key (id) )", tableName));
            logger.debug("create table query string: %s",query.toString());
        } else {
            logger.warn("Oops, unsupported table type for table %s",tableName);
            return;
        }

        try{
            conn = DriverManager.getConnection( this.connectionString, this.user, this.password);
            Statement statement = conn.createStatement();
            statement.executeUpdate(query.toString());
        } catch (SQLException exception) {
            logger.error("Error creating new %s for %s data, SQL State: %s Exception: %s", tableName, objectTypeName, exception.getSQLState(), exception.toString());
        } finally {
            if( conn != null ) {
                try {
                    conn.close();
                } catch (SQLException ignored) {}
            }
        }
    }

    private void addMissingColumns(String tableName, Object object) {
        Map<String,ColumnFeatures> columns = getMissingColumns(tableName, object);
        for( ColumnFeatures column : columns.values() ) {
            if( column.isMissing ) alterTableToAddColumn( tableName, column);
            if( column.isWrongNullable || column.isWrongType ) logger.warn("We can't fix this problem with the table %s.%s, wrong nullable: %s, wrong type: %s is %s but should be", tableName, column.name, column.isWrongNullable, column.isWrongType, column.type, metricDataDatabaseColumns.get(column.name).type);
            if( column.isWrongSize && !column.isWrongType ) {
                if( column.size < metricDataDatabaseColumns.get(column.name).size ) { //we can increase column size
                    alterTableToIncreaseColumnSize(tableName, column, metricDataDatabaseColumns.get(column.name).size);
                } else {
                    logger.info("We can't fix this problem with the table %s.%s, actual column size(%d) is larger than required(%d), this arguably isn't a problem now that i think about it",tableName,column.name, column.size, metricDataDatabaseColumns.get(column.name).size);
                }
            }
        }
    }

    private void alterTableToIncreaseColumnSize(String tableName, ColumnFeatures column, int size) {
        Connection conn = null;
        try{
            conn = DriverManager.getConnection( this.connectionString, this.user, this.password);
            column.size = size;
            String query = String.format("alter table %s modify %s %s )", tableName, column.name, column.printConstraints());
            logger.debug("alterTableToIncreaseColumnSize query: %s",query);
            Statement statement = conn.createStatement();
            statement.executeUpdate(query);
        } catch (Exception exception) {
            logger.error("Error altering table to add column %s.%s, Exception: %s", tableName, column.name, exception.toString());
        } finally {
            if( conn != null ) {
                try {
                    conn.close();
                } catch (SQLException ignored) {}
            }
        }
    }

    private void alterTableToAddColumn(String tableName, ColumnFeatures column) {
        Connection conn = null;
        try{
            conn = DriverManager.getConnection( this.connectionString, this.user, this.password);
            String query = String.format("alter table %s add ( %s %s )", tableName, column.name, column.printConstraints());
            logger.debug("alterTableToAddColumn query: %s",query);
            Statement statement = conn.createStatement();
            statement.executeUpdate(query);
        } catch (Exception exception) {
            logger.error("Error altering table to add column %s.%s, Exception: %s", tableName, column.name, exception.toString());
        } finally {
            if( conn != null ) {
                try {
                    conn.close();
                } catch (SQLException ignored) {}
            }
        }
    }

    private Map<String, ColumnFeatures> getMissingColumns(String tableName, Object object) {
        Map<String,ColumnFeatures> columns = getTableColumns(tableName);
        if( object instanceof MetricData ) {
            for( ColumnFeatures masterColumn : metricDataDatabaseColumns.values() ) {
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

    private Map<String, ColumnFeatures> getTableColumns(String tableName) {
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
                    "order by sys.all_tab_columns.column_id", tableName);
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
            logger.error("Error describing table %s, Exception: %s", tableName, exception.toString());
        } finally {
            if( conn != null ) {
                try {
                    conn.close();
                } catch (SQLException ignored) {}
            }
        }
        return columns;
    }

    private boolean doesTableExist(String table) {
        Connection conn = null;
        try{
            conn = DriverManager.getConnection( this.connectionString, this.user, this.password);
            String query = String.format("select table_name from all_tables where lower(table_name) like lower('%s')", table);
            Statement statement = conn.createStatement();
            ResultSet resultSet = statement.executeQuery(query);
            if( resultSet.next() ) {
                String table_name = resultSet.getString(1);
                logger.debug("doesTableExist(%s): Yes",table);
                return true;
            } else {
                logger.debug("doesTableExist(%s): No it does not",table);
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
