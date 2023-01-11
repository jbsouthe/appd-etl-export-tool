package com.cisco.josouthe.database.microsoft;

import com.cisco.josouthe.data.event.EventData;
import com.cisco.josouthe.database.ColumnFeatures;
import com.cisco.josouthe.database.Database;
import com.cisco.josouthe.database.IEventTable;
import com.cisco.josouthe.exceptions.FailedDataLoadException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;

public class EventTable extends MicrosoftTable implements IEventTable {
    protected static final Logger logger = LogManager.getFormatterLogger();

    public EventTable(String tableName, Database database ) {
        super(tableName, "Event Table", database);
        columns.put("controller", new ColumnFeatures("controller", MicrosoftDatabase.STRING_TYPE, 50, false));
        columns.put("application", new ColumnFeatures("application", MicrosoftDatabase.STRING_TYPE, 50, false));
        columns.put("id", new ColumnFeatures("id", MicrosoftDatabase.INTEGER_TYPE, -1, false));
        columns.put("eventTime", new ColumnFeatures("eventTime", MicrosoftDatabase.INTEGER_TYPE, -1, false));
        columns.put("type", new ColumnFeatures("type", MicrosoftDatabase.STRING_TYPE, 50, false));
        columns.put("subType", new ColumnFeatures("subType", MicrosoftDatabase.STRING_TYPE, 50, true));
        columns.put("severity", new ColumnFeatures("severity", MicrosoftDatabase.STRING_TYPE, 20, false));
        columns.put("summary", new ColumnFeatures("summary", MicrosoftDatabase.STRING_TYPE, 120, true));
        columns.put("triggeredEntityId", new ColumnFeatures("triggeredEntityId", MicrosoftDatabase.INTEGER_TYPE, -1, false));
        columns.put("triggeredEntityName", new ColumnFeatures("triggeredEntityName", MicrosoftDatabase.STRING_TYPE, 120, true));
        columns.put("triggeredEntityType", new ColumnFeatures("triggeredEntityType", MicrosoftDatabase.STRING_TYPE, 120, false));
        columns.put("eventTimestamp", new ColumnFeatures("eventTimestamp", MicrosoftDatabase.DATE_TYPE, -1, false));
        this.initTable();
    }

    @Override
    public int insert(Object object) throws FailedDataLoadException {
        EventData event = (EventData) object;
        int counter=0;
        StringBuilder insertSQL = new StringBuilder(String.format("insert into %s (",name));
        insertSQL.append("controller, application, id, eventTime, type, subtype, severity, summary, triggeredEntityId, triggeredEntityName, triggeredEntityType, eventTimestamp");
        insertSQL.append(") VALUES (?,?,?,?,?,?,?,?,?,?,?, dateadd(s, ?/1000, '1970-01-01') )");
        logger.trace("insertMetric SQL: %s",insertSQL);
        try ( Connection conn = database.getConnection(); PreparedStatement preparedStatement = conn.prepareStatement(insertSQL.toString());){
            int parameterIndex = 1;
            preparedStatement.setString(parameterIndex++, fitToSize(event.controllerHostname, "controller"));
            preparedStatement.setString(parameterIndex++, fitToSize(event.applicationName, "application"));
            preparedStatement.setLong(parameterIndex++, event.id);
            preparedStatement.setLong(parameterIndex++, event.eventTime);
            preparedStatement.setString(parameterIndex++, fitToSize(event.type, "type"));
            preparedStatement.setString(parameterIndex++, fitToSize(event.subType, "subType"));
            preparedStatement.setString(parameterIndex++, fitToSize(event.severity, "severity"));
            preparedStatement.setString(parameterIndex++, fitToSize(event.summary, "summary"));
            if( event.triggeredEntity != null ) {
                preparedStatement.setLong(parameterIndex++, event.triggeredEntity.entityId);
                preparedStatement.setString(parameterIndex++, fitToSize(event.triggeredEntity.name,"triggeredEntityName"));
                preparedStatement.setString(parameterIndex++, fitToSize(event.triggeredEntity.entityType, "triggeredEntityType"));
            } else if( event.affectedEntities != null ) {
                    preparedStatement.setLong(parameterIndex++, event.affectedEntities.get(0).entityId);
                    preparedStatement.setString(parameterIndex++, fitToSize(event.affectedEntities.get(0).name, "triggeredEntityName"));
                    preparedStatement.setString(parameterIndex++, fitToSize(event.affectedEntities.get(0).entityType, "triggeredEntityType"));
            } else {
                preparedStatement.setLong(parameterIndex++, -1);
                preparedStatement.setString(parameterIndex++, "");
                preparedStatement.setString(parameterIndex++, "");
            }
            preparedStatement.setLong(parameterIndex++, event.eventTime);
            counter += preparedStatement.executeUpdate();
        } catch (Exception exception) {
            logger.error("Error inserting events into %s, Exception: %s Query: '%s'", name, exception.toString(), insertSQL.toString());
            throw new FailedDataLoadException( String.format("Error inserting events into %s, Exception: %s", name, exception.toString()), object);
        }
        return counter;
    }


}
