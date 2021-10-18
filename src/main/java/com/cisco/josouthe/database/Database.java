package com.cisco.josouthe.database;

import com.cisco.josouthe.data.analytic.Result;
import com.cisco.josouthe.data.event.EventData;
import com.cisco.josouthe.data.metric.MetricData;
import com.cisco.josouthe.exceptions.InvalidConfigurationException;
import com.cisco.josouthe.util.Utility;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;

public abstract class Database {
    private static final Logger logger = LogManager.getFormatterLogger();

    public static final String STRING_TYPE = "varchar2";
    public static final int STRING_SIZE = 120;
    public static final String INTEGER_TYPE = "number";
    public static final int INTEGER_SIZE = 22;
    public static final String DATE_TYPE = "date";
    public static final int DATE_SIZE = -1;

    protected String connectionString, user, password, vendorName;
    protected Table defaultMetricTable, controlTable, defaulEventTable;
    protected Map<String,Table> tablesMap;

    public Database( String connectionString, String user, String password ) {
        this.connectionString = connectionString;
        this.user = user;
        this.password = password;
        this.vendorName = Utility.parseDatabaseVendor(connectionString);
    }


    public ControlTable getControlTable() { return (ControlTable) controlTable; }


    public abstract boolean isDatabaseAvailable();

    public abstract void importMetricData(MetricData[] metricData);

    public abstract void importEventData(EventData[] events);

    public abstract Connection getConnection() throws SQLException;

    public abstract void importAnalyticData(Result[] results);

    public abstract String convertToAcceptableColumnName(String label, Collection<ColumnFeatures> existingColumns);

    public abstract boolean isValidDatabaseTableName( String tableName ) throws InvalidConfigurationException;

}
