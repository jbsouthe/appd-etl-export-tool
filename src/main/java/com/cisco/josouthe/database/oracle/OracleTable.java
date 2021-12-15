package com.cisco.josouthe.database.oracle;

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

public abstract class OracleTable extends Table {
    public OracleTable(String tableName, String tableType, Database database) {
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
            if( !exception.toString().contains("ORA-00955") ) //ignore the name is already used by an existing object, this can happen on initial load
                logger.error("Error creating new %s for %s data, SQL State: %s Exception: %s", getName(), getType(), exception.getSQLState(), exception.toString());
        }
    }

    protected void alterTableToIncreaseColumnSize(ColumnFeatures column, int size) {
        if( column == null ) return;
        try ( Connection conn = database.getConnection(); Statement statement = conn.createStatement();){
            column.size = size;
            String query = String.format("alter table %s modify %s %s ", getName(), column.name, column.printConstraints());
            logger.debug("alterTableToIncreaseColumnSize query: %s",query);
            statement.executeUpdate(query);
        } catch (Exception exception) {
            logger.error("Error altering table to add column %s.%s, Exception: %s", getName(), column.name, exception.toString());
        }
    }

    protected void alterTableToAddColumn(ColumnFeatures column) {
        try ( Connection conn = database.getConnection(); Statement statement = conn.createStatement();){
            String query = String.format("alter table %s add ( %s %s )", getName(), column.name, column.printConstraints());
            logger.debug("alterTableToAddColumn query: %s",query);
            statement.executeUpdate(query);
        } catch (Exception exception) {
            logger.error("Error altering table to add column %s.%s, Exception: %s", getName(), column.name, exception.toString());
        }
    }

    protected Map<String, ColumnFeatures> getTableColumns() {
        Map<String,ColumnFeatures> tableColumns = new HashMap<>();
        try ( Connection conn = database.getConnection(); Statement statement = conn.createStatement();){
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
            ResultSet resultSet = statement.executeQuery(query);
            while( resultSet.next() ) {
                String columnName = resultSet.getString(1);
                String columnType = resultSet.getString(2);
                int columnSize = resultSet.getInt(3);
                String columnNullable = resultSet.getString(4);
                ColumnFeatures columnFeatures = new ColumnFeatures(columnName, columnType, columnSize, ("N".equals(columnNullable) ? false : true));
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
            String query = String.format("select table_name from all_tables where lower(table_name) like lower('%s')", getName());
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
