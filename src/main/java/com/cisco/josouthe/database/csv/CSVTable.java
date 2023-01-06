package com.cisco.josouthe.database.csv;

import com.cisco.josouthe.database.ColumnFeatures;
import com.cisco.josouthe.database.Database;
import com.cisco.josouthe.database.Table;

import java.util.Map;

public abstract class CSVTable extends Table {
    public CSVTable(String tableName, String tableType, Database database) {
        super(tableName, tableType, database);
        this.initialized=true;
    }

    @Override
    protected void createTable() {

    }

    @Override
    protected void alterTableToIncreaseColumnSize(ColumnFeatures column, int size) {

    }

    @Override
    protected void alterTableToAddColumn(ColumnFeatures column) {

    }

    @Override
    protected Map<String, ColumnFeatures> getTableColumns() {
        return null;
    }

    @Override
    protected boolean doesTableExist() {
        return false;
    }

    public int getColumnSizeForName(String type) {
        switch(type) {
            case "string": return database.STRING_SIZE;
            case "integer": return database.INTEGER_SIZE;
            case "float": return database.FLOAT_SIZE;
            case "boolean": return database.BOOLEAN_SIZE;
            case "date": return database.DATE_SIZE;
            default: {
                logger.debug("Unknown data type: %s setting table column size to %s", type, database.STRING_SIZE);
            }
        }
        return database.STRING_SIZE;
    }

    public String getColumnTypeForName(String type) {
        switch(type) {
            case "string": return database.STRING_TYPE;
            case "integer": return database.INTEGER_TYPE;
            case "float": return database.FLOAT_TYPE;
            case "boolean": return database.BOOLEAN_TYPE;
            case "date": return database.DATE_TYPE;
            default: {
                logger.debug("Unknown data type: %s setting table column type to %s", type, database.STRING_TYPE);
            }
        }
        return database.STRING_TYPE;
    }

}
