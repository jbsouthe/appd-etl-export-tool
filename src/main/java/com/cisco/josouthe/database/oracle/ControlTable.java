package com.cisco.josouthe.database.oracle;

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

public class ControlTable extends OracleTable implements IControlTable {
    protected static final Logger logger = LogManager.getFormatterLogger();
    private Integer defaultLoadNumberOfHoursIfControlRowMissing = 48;

    public ControlTable( String tableName, Database database ) {
        super(tableName,"Control Table", database);
        columns.put("controller", new ColumnFeatures("controller", "varchar2", 120, false));
        columns.put("application", new ColumnFeatures("application", "varchar2", 120, false));
        columns.put("dataType", new ColumnFeatures("dataType", "varchar2", 50, false));
        columns.put("lastRunTimestamp", new ColumnFeatures("lastRunTimestamp", "number", 22, false));
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
            String query = String.format(" select lastRunTimestamp from %s where lower(controller) like lower('%s') and lower(application) like lower('%s') and lower(dataType) like lower('%s') ", this.name, controller, application, dataType);
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
            StringBuilder update = new StringBuilder(String.format(" merge into %s C using dual on ( lower(controller) like lower('%s') and lower(application) like lower('%s') and lower(dataType) like lower('%s') )", this.name, controlEntry.controller, controlEntry.application, controlEntry.type ));
            update.append(String.format(" when not matched then insert (controller,application,dataType,lastRunTimestamp) values ('%s','%s','%s',%d) ", controlEntry.controller, controlEntry.application, controlEntry.type, controlEntry.timestamp));
            update.append(String.format(" when matched then update set lastRunTimestamp = %d",controlEntry.timestamp));
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
