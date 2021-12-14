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
}
