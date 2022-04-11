package com.cisco.josouthe.database.mysql;

import com.cisco.josouthe.data.event.EventData;
import com.cisco.josouthe.database.ColumnFeatures;
import com.cisco.josouthe.database.Database;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;

public class EventTable extends MySQLTable implements com.cisco.josouthe.database.EventTable {
    protected static final Logger logger = LogManager.getFormatterLogger();

    public EventTable( String tableName, Database database ) {
        super(tableName, "Event Table", database);
        columns.put("controller", new ColumnFeatures("controller", "varchar", 50, false));
        columns.put("application", new ColumnFeatures("application", "varchar", 50, false));
        columns.put("id", new ColumnFeatures("id", "bigint", 20, false));
        columns.put("eventtime", new ColumnFeatures("eventtime", "bigint", 20, false));
        columns.put("type", new ColumnFeatures("type", "varchar", 50, false));
        columns.put("subtype", new ColumnFeatures("subtype", "varchar", 50, true));
        columns.put("severity", new ColumnFeatures("severity", "varchar", 20, false));
        columns.put("summary", new ColumnFeatures("summary", "varchar", 120, true));
        columns.put("triggeredentityid", new ColumnFeatures("triggeredentityid", "bigint", 20, false));
        columns.put("triggeredentityname", new ColumnFeatures("triggeredentityname", "varchar", 120, true));
        columns.put("triggeredentitytype", new ColumnFeatures("triggeredentitytype", "varchar", 120, false));
        columns.put("eventtimestamp", new ColumnFeatures("eventtimestamp", "timestamp", -1, false));
        this.initTable();
    }

    @Override
    public int insert(Object object) {
        EventData event = (EventData) object;
        int counter=0;
        StringBuilder insertSQL = new StringBuilder(String.format("insert into %s (",name));
        insertSQL.append("controller, application, id, eventTime, type, subtype, severity, summary, triggeredEntityId, triggeredentityname, triggeredentitytype, eventtimestamp");
        insertSQL.append(") VALUES (?,?,?,?,?,?,?,?,?,?,?,?)");
        logger.trace("insertMetric SQL: %s",insertSQL);
        try ( Connection conn = database.getConnection(); PreparedStatement preparedStatement = conn.prepareStatement(insertSQL.toString());){
            int parameterIndex = 1;
            preparedStatement.setString(parameterIndex++, fitToSize(event.controllerHostname, "controller"));
            preparedStatement.setString(parameterIndex++, fitToSize(event.applicationName, "application"));
            preparedStatement.setLong(parameterIndex++, event.id);
            preparedStatement.setLong(parameterIndex++, event.eventTime);
            preparedStatement.setString(parameterIndex++, fitToSize(event.type, "type"));
            preparedStatement.setString(parameterIndex++, fitToSize(event.subType, "subtype"));
            preparedStatement.setString(parameterIndex++, fitToSize(event.severity, "severity"));
            preparedStatement.setString(parameterIndex++, fitToSize(event.summary, "summary"));
            if( event.triggeredEntity != null ) {
                preparedStatement.setInt(parameterIndex++, event.triggeredEntity.entityId);
                preparedStatement.setString(parameterIndex++, fitToSize(event.triggeredEntity.name,"triggeredentityname"));
                preparedStatement.setString(parameterIndex++, fitToSize(event.triggeredEntity.entityType, "triggeredentitytype"));
            } else if( event.affectedEntities != null ) {
                    preparedStatement.setInt(parameterIndex++, event.affectedEntities.get(0).entityId);
                    preparedStatement.setString(parameterIndex++, fitToSize(event.affectedEntities.get(0).name, "triggeredentityname"));
                    preparedStatement.setString(parameterIndex++, fitToSize(event.affectedEntities.get(0).entityType, "triggeredentitytype"));
            } else {
                preparedStatement.setInt(parameterIndex++, -1);
                preparedStatement.setString(parameterIndex++, "");
                preparedStatement.setString(parameterIndex++, "");
            }
            preparedStatement.setTimestamp(parameterIndex++, new Timestamp(event.eventTime));
            counter += preparedStatement.executeUpdate();
        } catch (Exception exception) {
            logger.error("Error inserting events into %s, Exception: %s", name, exception.toString());
            logger.warn("Bad SQL: %s",insertSQL.toString());
        }
        return counter;
    }


}
