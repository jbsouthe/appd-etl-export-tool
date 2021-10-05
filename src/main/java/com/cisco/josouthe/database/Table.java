package com.cisco.josouthe.database;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

public abstract class Table {
    protected static final Logger logger = LogManager.getFormatterLogger();
    protected Map<String,ColumnFeatures> columns = null;
    protected String name = null;
    protected Database database = null;

    public Table( String tableName, Database database ) {
        this.name = tableName;
        this.database = database;
        columns = new HashMap<>();
    }

    public String getName() { return name; }

    public Map<String, ColumnFeatures> getColumns() { return columns; }

    public String toString() { return getName(); }

    public abstract int insert(Object object);
}
