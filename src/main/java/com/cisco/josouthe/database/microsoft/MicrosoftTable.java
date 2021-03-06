package com.cisco.josouthe.database.microsoft;

import com.cisco.josouthe.database.ColumnFeatures;
import com.cisco.josouthe.database.Database;
import com.cisco.josouthe.database.Table;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public abstract class MicrosoftTable extends Table {
    public MicrosoftTable(String tableName, String tableType, Database database) {
        super(tableName, tableType, database);
    }

    protected void createTable() {
        String objectTypeName="UNKNOWN";
        StringBuilder query = new StringBuilder(String.format("create table %s ( ",this.getName()));
        Iterator<ColumnFeatures> iterator = getColumns().values().iterator();
        while( iterator.hasNext() ) {
            ColumnFeatures column = iterator.next();
            query.append(String.format("%s %s",column.name, column.printConstraints()));
            if(iterator.hasNext()) query.append(", ");
        }
        query.append(")");
        logger.debug("create table query string: %s",query.toString());


        try (Connection conn = database.getConnection(); Statement statement = conn.createStatement();){
            statement.executeUpdate(query.toString());
        } catch (SQLException exception) {
            //if( !exception.toString().contains("ORA-00955") ) //ignore the name is already used by an existing object, this can happen on initial load
                logger.error("Error creating new %s for %s data, SQL State: %s Exception: %s", getName(), getType(), exception.getSQLState(), exception.toString());
        }
    }

    protected void alterTableToIncreaseColumnSize(ColumnFeatures column, int size) {
        if( column == null ) return;
        try ( Connection conn = database.getConnection(); Statement statement = conn.createStatement();){
            column.size = size;
            String query = String.format("alter table %s { alter column %s %s }", getName(), column.name, column.printConstraints());
            logger.debug("alterTableToIncreaseColumnSize query: %s",query);
            statement.executeUpdate(query);
        } catch (Exception exception) {
            logger.error("Error altering table to add column %s.%s, Exception: %s", getName(), column.name, exception.toString());
        }
    }

    protected void alterTableToAddColumn(ColumnFeatures column) {
        try ( Connection conn = database.getConnection(); Statement statement = conn.createStatement();){
            String query = String.format("alter table %s add { %s %s }", getName(), column.name, column.printConstraints());
            logger.debug("alterTableToAddColumn query: %s",query);
            statement.executeUpdate(query);
        } catch (Exception exception) {
            logger.error("Error altering table to add column %s.%s, Exception: %s", getName(), column.name, exception.toString());
        }
    }

    protected Map<String, ColumnFeatures> getTableColumns() {
        Map<String,ColumnFeatures> tableColumns = new HashMap<>();
        try ( Connection conn = database.getConnection(); Statement statement = conn.createStatement();){
            String query = String.format("select column_name, data_type, CHARACTER_MAXIMUM_LENGTH, is_nullable "+
                    "where lower(table_name) like lower('%s')", getName());
            ResultSet resultSet = statement.executeQuery(query);
            while( resultSet.next() ) {
                String columnName = resultSet.getString(1);
                String columnType = resultSet.getString(2);
                Integer columnSize = resultSet.getInt(3);
                String columnNullable = resultSet.getString(4);
                ColumnFeatures columnFeatures = new ColumnFeatures(columnName, columnType, ( columnSize==null ? -1 : columnSize.intValue()), ("NO".equals(columnNullable) ? false : true));
                tableColumns.put(columnFeatures.name,columnFeatures);
            }
            resultSet.close();
        } catch (Exception exception) {
            logger.error("Error describing table %s, Exception: %s", getName(), exception.toString());
        }
        return tableColumns;
    }

    protected boolean doesTableExist() {
        boolean tableExists = false;
        try ( Connection conn = database.getConnection(); Statement statement = conn.createStatement();){
            String query = String.format("select table_name from INFORMATION_SCHEMA.TABLES where lower(table_name) like lower('%s')", getName());
            ResultSet resultSet = statement.executeQuery(query);
            if( resultSet.next() ) {
                String table_name = resultSet.getString(1);
                logger.debug("doesTableExist(%s): Yes",getName());
                tableExists = true;
            } else {
                logger.debug("doesTableExist(%s): No it does not",getName());
                tableExists = false;
            }
            resultSet.close();
        } catch (Exception exception) {
            logger.error("Error checking for database table existence, Exception: %s", exception.toString());
        }
        return tableExists;
    }
}
