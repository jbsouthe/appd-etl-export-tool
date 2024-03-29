package com.cisco.josouthe.database.mysql;

import com.cisco.josouthe.database.ColumnFeatures;
import com.cisco.josouthe.database.ControlEntry;
import com.cisco.josouthe.database.Database;
import com.cisco.josouthe.database.IControlTable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class ControlTable extends MySQLTable implements IControlTable {
    protected static final Logger logger = LogManager.getFormatterLogger();
    private Integer defaultLoadNumberOfHoursIfControlRowMissing = 48;

    public ControlTable( String tableName, Database database ) {
        super(tableName,"Control Table", database);
        columns.put("controller", new ColumnFeatures("controller", "varchar", 120, false, true));
        columns.put("application", new ColumnFeatures("application", "varchar", 120, false, true));
        columns.put("datatype", new ColumnFeatures("datatype", "varchar", 50, false, true));
        columns.put("lastruntimestamp", new ColumnFeatures("lastruntimestamp", "bigint", 20, false));
        initTable();
    }

    @Override
    public void setDefaultLoadNumberOfHoursIfControlRowMissing(int hours) { this.defaultLoadNumberOfHoursIfControlRowMissing=hours; }

    @Override
    public int insert(Object object) {
        return 0;
    }

    @Override
    public ControlEntry getLastRunTimestamp(String controller, String application, String dataType) {
        ControlEntry controlEntry = new ControlEntry();
        controlEntry.controller=controller;
        controlEntry.application=application;
        controlEntry.type=dataType;
        Long timeStamp = null;
        try (Connection conn = database.getConnection(); Statement statement = conn.createStatement(); ){
            String query = String.format(" select lastRunTimestamp from %s where lower(controller) like lower(\"%s\") and lower(application) like lower(\"%s\") and lower(dataType) like lower(\"%s\") ", this.name, controller, application, dataType);
            ResultSet resultSet = statement.executeQuery(query);
            if( resultSet.next() ) {
                timeStamp = resultSet.getLong(1);
            }
            resultSet.close();
        } catch (SQLException exception) {
            logger.error("Error getting last run time from table %s for %s:%s(%s), Exception: %s", getName(), controller, application, dataType, exception.toString());
        }
        if( timeStamp != null ) {
            controlEntry.timestamp = timeStamp;
        } else {
            controlEntry.timestamp = System.currentTimeMillis() - (this.defaultLoadNumberOfHoursIfControlRowMissing.longValue() * 60 * 60 * 1000);
        }
        return controlEntry;
    }

    @Override
    public synchronized int setLastRunTimestamp(ControlEntry controlEntry) {
        try (Connection conn = database.getConnection(); Statement statement = conn.createStatement();){
            /*
            StringBuilder update = new StringBuilder(String.format(" merge %s t using dual on ( lower(t.controller) = '%s' and lower(t.application) = '%s' and lower(t.dataType) = '%s' )", this.name, controlEntry.controller.toLowerCase(), controlEntry.application.toLowerCase(), controlEntry.type.toLowerCase() ));
            update.append(String.format(" when not matched by target then insert (controller,application,dataType,lastRunTimestamp) values ('%s','%s','%s',%d) ", controlEntry.controller, controlEntry.application, controlEntry.type, controlEntry.timestamp));
            update.append(String.format(" when matched then update set lastRunTimestamp = %d",controlEntry.timestamp));
             */
            StringBuilder update = new StringBuilder(String.format("insert into %s (controller,application,dataType,lastRunTimestamp) values ('%s','%s','%s',%d)  ", this.name,controlEntry.controller, controlEntry.application, controlEntry.type, controlEntry.timestamp ));
            update.append(String.format("on duplicate key update lastRunTimestamp = %d", controlEntry.timestamp));
            return statement.executeUpdate(update.toString());
        } catch (SQLException exception) {
            logger.error("Error setting last run time into table %s for %s:%s(%s), Exception: %s", getName(), controlEntry.controller, controlEntry.application, controlEntry.type, exception.toString());
        }
        return 0;
    }

    public List<ControlEntry> getControlEntries() {
        List<ControlEntry> entries = new ArrayList<>();
        try (Connection conn = database.getConnection(); Statement statement = conn.createStatement(); ){
            String query = String.format(" select controller,application,dataType,lastRunTimestamp from %s ", this.name);
            ResultSet resultSet = statement.executeQuery(query);
            while( resultSet.next() ) {
                ControlEntry entry = new ControlEntry();
                entry.controller = resultSet.getString("controller");
                entry.application = resultSet.getString("application");
                entry.type = resultSet.getString("dataType");
                entry.timestamp = resultSet.getLong("lastRunTimestamp");
                entries.add(entry);
            }
            resultSet.close();
        } catch (SQLException exception) {
            logger.error("Error getting Control Entries from table %s, Exception: %s", getName(), exception.toString());
        }
        return entries;
    }
}
