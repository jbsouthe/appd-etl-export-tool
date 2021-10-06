package com.cisco.josouthe.database;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
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

    public String getName() { return name; }
    public String getType() { return type; }

    public Map<String, ColumnFeatures> getColumns() { return columns; }

    public String toString() { return getName(); }

    public abstract int insert(Object object);
}
