package com.cisco.josouthe.database;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public abstract class Table {
    protected static final Logger logger = LogManager.getFormatterLogger();
    protected Map<String,ColumnFeatures> columns = null;
    protected String name = null;
    protected String type = "UNKNOWN TABLE TYPE";
    protected Database database = null;

    public Table( String tableName, String tableType, Database database ) {
        this.name = tableName;
        this.type = tableType;
        this.database = database;
        columns = new HashMap<>();
    }

    protected void initTable() {
        if( doesTableExist() ) {
            addMissingColumns();
        } else {
            createTable();
        }
    }

    public String getName() { return name; }
    public String getType() { return type; }

    public Map<String, ColumnFeatures> getColumns() { return columns; }

    public String toString() { return getName(); }

    public abstract int insert(Object object);

    private void createTable() {
        Connection conn = null;
        String objectTypeName="UNKNOWN";
        //StringBuilder query = new StringBuilder(String.format("create table %s ( id serial NOT NULL, ",tableName));
        StringBuilder query = new StringBuilder(String.format("create table %s ( ",this.getName()));
        Iterator<ColumnFeatures> iterator = getColumns().values().iterator();
        while( iterator.hasNext() ) {
            ColumnFeatures column = iterator.next();
            query.append(String.format("%s %s",column.name, column.printConstraints()));
            if(iterator.hasNext()) query.append(", ");
        }
        query.append(")");
        //query.append(String.format(", constraint pk_%s primary key (id) )", tableName));
        logger.debug("create table query string: %s",query.toString());


        try{
            conn = database.getConnection();
            Statement statement = conn.createStatement();
            statement.executeUpdate(query.toString());
        } catch (SQLException exception) {
            logger.error("Error creating new %s for %s data, SQL State: %s Exception: %s", getName(), getType(), exception.getSQLState(), exception.toString());
        } finally {
            if( conn != null ) {
                try {
                    conn.close();
                } catch (SQLException ignored) {}
            }
        }
    }

    private void addMissingColumns() {
        Map<String,ColumnFeatures> missingColumns = getMissingColumns();
        if( missingColumns == null ) return;
        for( ColumnFeatures column : missingColumns.values() ) {
            if( column.isMissing ) alterTableToAddColumn( column);
            if( column.isWrongNullable || column.isWrongType ) logger.warn("We can't fix this problem with the table %s.%s, wrong nullable: %s, wrong type: %s is %s but should be", getName(), column.name, column.isWrongNullable, column.isWrongType, column.type, getColumns().get(column.name).type);
            if( column.isWrongSize && !column.isWrongType ) {
                if( column.size < getColumns().get(column.name).size ) { //we can increase column size
                    alterTableToIncreaseColumnSize(column, getColumns().get(column.name).size);
                } else {
                    logger.info("We can't fix this problem with the table %s.%s, actual column size(%d) is larger than required(%d), this arguably isn't a problem now that i think about it",getName(),column.name, column.size, getColumns().get(column.name).size);
                }
            }
        }
    }
    private void alterTableToIncreaseColumnSize(ColumnFeatures column, int size) {
        Connection conn = null;
        try{
            conn = database.getConnection();
            column.size = size;
            String query = String.format("alter table %s modify %s %s )", getName(), column.name, column.printConstraints());
            logger.debug("alterTableToIncreaseColumnSize query: %s",query);
            Statement statement = conn.createStatement();
            statement.executeUpdate(query);
        } catch (Exception exception) {
            logger.error("Error altering table to add column %s.%s, Exception: %s", getName(), column.name, exception.toString());
        } finally {
            if( conn != null ) {
                try {
                    conn.close();
                } catch (SQLException ignored) {}
            }
        }
    }

    private void alterTableToAddColumn(ColumnFeatures column) {
        Connection conn = null;
        try{
            conn = database.getConnection();
            String query = String.format("alter table %s add ( %s %s )", getName(), column.name, column.printConstraints());
            logger.debug("alterTableToAddColumn query: %s",query);
            Statement statement = conn.createStatement();
            statement.executeUpdate(query);
        } catch (Exception exception) {
            logger.error("Error altering table to add column %s.%s, Exception: %s", getName(), column.name, exception.toString());
        } finally {
            if( conn != null ) {
                try {
                    conn.close();
                } catch (SQLException ignored) {}
            }
        }
    }

    private Map<String, ColumnFeatures> getMissingColumns() {
        Map<String,ColumnFeatures> tableColumns = getTableColumns();
        for( ColumnFeatures masterColumn : getColumns().values() ) {
            ColumnFeatures existingColumn = tableColumns.get(masterColumn.name);
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
                if( !foundADifference ) tableColumns.remove(existingColumn.name);
            } else { //column is missing entirely, mark for creation
                ColumnFeatures newColumn = masterColumn.clone();
                newColumn.isMissing=true;
                tableColumns.put(newColumn.name, newColumn);
            }
            return tableColumns;
        }
        return null;
    }

    private Map<String, ColumnFeatures> getTableColumns() {
        Map<String,ColumnFeatures> tableColumns = new HashMap<>();
        Connection conn = null;
        try{
            conn = database.getConnection();
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
                    "order by sys.all_tab_columns.column_id", getName());
            Statement statement = conn.createStatement();
            ResultSet resultSet = statement.executeQuery(query);
            while( resultSet.next() ) {
                String columnName = resultSet.getString(1);
                String columnType = resultSet.getString(2);
                int columnSize = resultSet.getInt(3);
                String columnNullable = resultSet.getString(4);
                ColumnFeatures columnFeatures = new ColumnFeatures(columnName, columnType, columnSize, ("N".equals(columnNullable) ? false : true));
                tableColumns.put(columnFeatures.name,columnFeatures);
            }
        } catch (Exception exception) {
            logger.error("Error describing table %s, Exception: %s", getName(), exception.toString());
        } finally {
            if( conn != null ) {
                try {
                    conn.close();
                } catch (SQLException ignored) {}
            }
        }
        return tableColumns;
    }

    private boolean doesTableExist() {
        Connection conn = null;
        try{
            conn = database.getConnection();
            String query = String.format("select table_name from all_tables where lower(table_name) like lower('%s')", getName());
            Statement statement = conn.createStatement();
            ResultSet resultSet = statement.executeQuery(query);
            if( resultSet.next() ) {
                String table_name = resultSet.getString(1);
                logger.debug("doesTableExist(%s): Yes",getName());
                return true;
            } else {
                logger.debug("doesTableExist(%s): No it does not",getName());
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
