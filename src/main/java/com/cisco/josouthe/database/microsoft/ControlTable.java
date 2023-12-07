package com.cisco.josouthe.database.microsoft;

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

public class ControlTable extends MicrosoftTable implements IControlTable {
    protected static final Logger logger = LogManager.getFormatterLogger();
    private Integer defaultLoadNumberOfHoursIfControlRowMissing = 48;

    public ControlTable(String tableName, Database database ) {
        super(tableName,"Control Table", database);
        columns.put("controller", new ColumnFeatures("controller", MicrosoftDatabase.STRING_TYPE, 120, false));
        columns.put("application", new ColumnFeatures("application", MicrosoftDatabase.STRING_TYPE, 120, false));
        columns.put("dataType", new ColumnFeatures("dataType", MicrosoftDatabase.STRING_TYPE, 50, false));
        columns.put("lastRunTimestamp", new ColumnFeatures("lastRunTimestamp", MicrosoftDatabase.INTEGER_TYPE, -1, false));
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
            controlEntry.rowExistsInDB = true;
        } else {
            controlEntry.timestamp = System.currentTimeMillis() - (this.defaultLoadNumberOfHoursIfControlRowMissing.longValue() * 60 * 60 * 1000);
            controlEntry.rowExistsInDB = false;
        }
        return controlEntry;
    }

    @Override
    public synchronized int setLastRunTimestamp(ControlEntry controlEntry) {
        /* Merge sucks in ms sql so we have to do it differently */
        StringBuilder update = new StringBuilder("Not yet initialized");
        try (Connection conn = database.getConnection(); Statement statement = conn.createStatement();){
            if( controlEntry.rowExistsInDB ) { //update
                update = new StringBuilder(String.format("update %s set lastRunTimestamp = %d where lower(controller) like lower('%s') and lower(application) like lower('%s') and lower(dataType) like lower('%s')", this.name, controlEntry.controller, controlEntry.application, controlEntry.type ));
            } else { //insert
                update = new StringBuilder(String.format("insert into %s (controller,application,dataType,lastRunTimestamp) values ('%s','%s','%s',%d) ", this.name, controlEntry.controller, controlEntry.application, controlEntry.type, controlEntry.timestamp));
            }
            /* one day maybe....
            update = new StringBuilder(String.format(" merge into %s on ( lower(controller) like lower('%s') and lower(application) like lower('%s') and lower(dataType) like lower('%s') )", this.name, controlEntry.controller, controlEntry.application, controlEntry.type ));
            update.append(String.format(" when not matched then insert (controller,application,dataType,lastRunTimestamp) values ('%s','%s','%s',%d) ", controlEntry.controller, controlEntry.application, controlEntry.type, controlEntry.timestamp));
            update.append(String.format(" when matched then update set lastRunTimestamp = %d;",controlEntry.timestamp));
             */
            return statement.executeUpdate(update.toString());
        } catch (SQLException exception) {
            logger.error("Error setting last run time into table %s for %s:%s(%s), Exception: %s merge sql: '%s'", getName(), controlEntry.controller, controlEntry.application, controlEntry.type, exception.toString(), update);
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
