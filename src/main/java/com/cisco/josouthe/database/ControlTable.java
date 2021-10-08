package com.cisco.josouthe.database;

import com.cisco.josouthe.util.Utility;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class ControlTable extends Table{
    protected static final Logger logger = LogManager.getFormatterLogger();
    private int defaultLoadNumberOfHoursIfControlRowMissing = 48;

    public ControlTable( String tableName, Database database ) {
        super(tableName,"Control Table", database);
        columns.put("controller", new ColumnFeatures("controller", "varchar2", 50, false));
        columns.put("application", new ColumnFeatures("application", "varchar2", 50, false));
        columns.put("dataType", new ColumnFeatures("dataType", "varchar2", 50, false));
        columns.put("lastRunTimestamp", new ColumnFeatures("lastRunTimestamp", "number", 22, false));
        initTable();
    }

    public void setDefaultLoadNumberOfHoursIfControlRowMissing( int hours ) { this.defaultLoadNumberOfHoursIfControlRowMissing=hours; }

    @Override
    public int insert(Object object) {
        return 0;
    }

    public long getLastRunTimestamp( String controller, String application, String dataType) {
        Long timeStamp = null;
        Connection conn = null;
        try{
            conn = database.getConnection();
            String query = String.format(" select lastRunTimestamp from %s where lower(controller) like lower('%s') and lower(application) like lower('%s') and lower(dataType) like lower('%s') ", this.name, controller, application, dataType);
            Statement statement = conn.createStatement();
            ResultSet resultSet = statement.executeQuery(query);
            if( resultSet.next() ) {
                return resultSet.getLong(1);
            }
        } catch (SQLException exception) {
            logger.error("Error getting last run time from table %s for %s:%s(%s), Exception: %s", getName(), controller, application, dataType, exception.toString());
        } finally {
            if( conn != null ) {
                try {
                    conn.close();
                } catch (SQLException ignored) {}
            }
        }
        return Utility.now()-(this.defaultLoadNumberOfHoursIfControlRowMissing*60*60*1000);
    }

    public synchronized int setLastRunTimestamp( String controller, String application, String dataType, long timeStamp) {
        Connection conn = null;
        try{
            conn = database.getConnection();
            StringBuilder update = new StringBuilder(String.format(" merge into %s C using dual on ( lower(controller) like lower('%s') and lower(application) like lower('%s') and lower(dataType) like lower('%s') )", this.name, controller, application, dataType ));
            update.append(String.format(" when not matched then insert (controller,application,dataType,lastRunTimestamp) values ('%s','%s','%s',%d) ", controller, application, dataType, timeStamp));
            update.append(String.format(" when matched then update set lastRunTimestamp = %d",timeStamp));
            Statement statement = conn.createStatement();
            return statement.executeUpdate(update.toString());
        } catch (SQLException exception) {
            logger.error("Error setting last run time into table %s for %s:%s(%s), Exception: %s", getName(), controller, application, dataType, exception.toString());
        } finally {
            if( conn != null ) {
                try {
                    conn.close();
                } catch (SQLException ignored) {}
            }
        }
        return 0;
    }


}
