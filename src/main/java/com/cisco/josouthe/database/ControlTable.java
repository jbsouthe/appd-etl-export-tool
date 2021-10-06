package com.cisco.josouthe.database;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ControlTable extends Table{
    protected static final Logger logger = LogManager.getFormatterLogger();


    public ControlTable( String tableName, Database database ) {
        super(tableName,"Control Table", database);
        columns.put("controller", new ColumnFeatures("controller", "varchar2", 50, false));
        columns.put("application", new ColumnFeatures("application", "varchar2", 50, false));
        columns.put("metricname", new ColumnFeatures("metricName", "varchar2", 200, false));
        columns.put("metricpath", new ColumnFeatures("metricPath", "varchar2", 200, false));
        columns.put("frequency", new ColumnFeatures("frequency", "varchar2", 50, false));
        columns.put("userange", new ColumnFeatures("userange", "number", 22, false));
        for( String columnName : new String[] { "metricid","startTimeInMillis", "occurrences", "currentValue", "min", "max", "count", "sum", "value", "standardDeviation"})
            columns.put(columnName.toLowerCase(), new ColumnFeatures(columnName, "number", 22, false));
        columns.put("startTimestamp", new ColumnFeatures("startTimestamp", "date", -1, false));
    }

    @Override
    public int insert(Object object) {
        return 0;
    }


}
