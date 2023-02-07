package com.cisco.josouthe.database;

import com.cisco.josouthe.config.Configuration;
import com.cisco.josouthe.exceptions.FailedDataLoadException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.LinkedHashMap;
import java.util.Map;

public abstract class Table {
    protected static final Logger logger = LogManager.getFormatterLogger();
    protected Map<String,ColumnFeatures> columns = null;
    protected String name = null;
    protected String type = "UNKNOWN TABLE TYPE";
    protected Database database = null;
    protected Configuration configuration = null;
    protected boolean initialized=false;

    public Table( String tableName, String tableType, Database database ) {
        this.name = tableName;
        this.type = tableType;
        this.database = database;
        this.configuration = database.configuration;
        columns = new LinkedHashMap<>();
    }

    protected void initTable() {
        if( doesTableExist() ) {
            addMissingColumns();
        } else {
            createTable();
        }
        this.initialized=true;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public String getName() { return name; }
    public String getType() { return type; }

    public Map<String, ColumnFeatures> getColumns() { return columns; }
    public ColumnFeatures getColumn( String name ) {
        ColumnFeatures columnFeatures = getColumns().get(name);
        if( columnFeatures == null ) columnFeatures = getColumn(name.toLowerCase());
        if( columnFeatures == null )
            for( ColumnFeatures possibleColumn : getColumns().values() )
                if( possibleColumn.name.toLowerCase().equals(name.toLowerCase()) ) columnFeatures = possibleColumn;
        return columnFeatures;
    }

    public String toString() { return getName(); }

    public String fitToSize( String text, String name ) {
        return fitToSize(text, getColumn(name).size);
    }

    public String fitToSize( String text, int maxSize) {
        if( text.length() <= maxSize) return text;
        logger.warn("Truncating insert text to '%s'(%d) original value: '%s'(%d)", text.substring(0,maxSize), maxSize, text, text.length());
        return text.substring(0,maxSize);
    }

    //each table has a specific method for the insert
    public abstract int insert(Object object) throws FailedDataLoadException;

    //each database vendor has their own specific SQL for these methods
    protected abstract void createTable();
    protected abstract void alterTableToIncreaseColumnSize(ColumnFeatures column, int size);
    protected abstract void alterTableToAddColumn(ColumnFeatures column);
    protected abstract Map<String, ColumnFeatures> getTableColumns();
    protected abstract boolean doesTableExist();
    ///////

    protected void addMissingColumns() {
        Map<String,ColumnFeatures> missingColumns = getMissingColumns();
        if( missingColumns == null ) return;
        for( ColumnFeatures column : missingColumns.values() ) {
            if( column.isMissing ) alterTableToAddColumn( column);
            if( column.isWrongNullable || column.isWrongType ) {
                ColumnFeatures columnFeatures = getColumns().get(column.name);
                if( columnFeatures == null ) logger.warn("About to error because getColumns().get(%s) returns null",column.name);
                logger.warn("We can't fix this problem with the table '%s.%s', wrong nullable: '%s', wrong type: '%s' is '%s' but should be '%s'", getName(), column.name, column.isWrongNullable, column.isWrongType, column.type, columnFeatures.type);
            }
            if( column.isWrongSize && !column.isWrongType ) {
                ColumnFeatures columnFeatures = getColumnDefinition(column.name);
                if( columnFeatures != null && column.size < columnFeatures.size ) { //we can increase column size
                    alterTableToIncreaseColumnSize(column, columnFeatures.size);
                } else {
                    //logger.trace("We can't fix this problem with the table %s.%s, actual column size(%d) is larger than required(%d), this arguably isn't a problem now that i think about it",getName(),column.name, column.size, columnFeatures.size);
                }
            }
        }
    }

    protected ColumnFeatures getColumnDefinition(String name) {
        ColumnFeatures columnFeatures = getColumns().get(name);
        if( columnFeatures != null ) return columnFeatures;
        for( ColumnFeatures column : getColumns().values() ) {
            if( column.name.toLowerCase().equals(name.toLowerCase()) )
                return column;
        }
        return null;
    }

    protected Map<String, ColumnFeatures> getMissingColumns() {
        Map<String,ColumnFeatures> tableColumns = getTableColumns();
        for( ColumnFeatures masterColumn : getColumns().values() ) {
            ColumnFeatures existingColumn = tableColumns.get(masterColumn.name);
            logger.debug("Checking data column '%s' for matching database column '%s'", masterColumn, existingColumn);
            if( existingColumn != null ) { //column exists, check all features
                boolean foundADifference=false;
                if( ! existingColumn.type.equals(masterColumn.type) ) {
                    existingColumn.isWrongType=true;
                    foundADifference=true;
                    logger.trace("Column %s is of wrong type %s, should be %s", existingColumn.name, existingColumn.type, masterColumn.type);
                }
                if( existingColumn.size != masterColumn.size ) {
                    existingColumn.isWrongSize=true;
                    foundADifference=true;
                    logger.trace("Column %s is of wrong size %d, should be %d", existingColumn.name, existingColumn.size, masterColumn.size);
                }
                if( existingColumn.isNull != masterColumn.isNull ) {
                    existingColumn.isWrongNullable=true;
                    foundADifference=true;
                    logger.trace("Column %s is of wrong null allowed %s, should be %s", existingColumn.name, existingColumn.isNull, masterColumn.isNull);
                }
                if( !foundADifference ) tableColumns.remove(existingColumn.name);
            } else { //column is missing entirely, mark for creation
                ColumnFeatures newColumn = masterColumn.clone();
                newColumn.isMissing=true;
                tableColumns.put(newColumn.name, newColumn);
                logger.debug("Column %s of type %s is missing entirely!", newColumn.name, newColumn.printConstraints());
            }
        }
        return tableColumns;
    }

}
