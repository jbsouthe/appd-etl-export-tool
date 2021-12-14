package com.cisco.josouthe.database.csv;

import com.cisco.josouthe.data.event.EventData;
import com.cisco.josouthe.database.ColumnFeatures;
import com.cisco.josouthe.database.Database;
import com.cisco.josouthe.util.Utility;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;

public class EventTable extends CSVTable implements com.cisco.josouthe.database.EventTable {
    protected static final Logger logger = LogManager.getFormatterLogger();
    private File baseDir;

    public EventTable( String tableName, Database database, File baseDir) {
        super(tableName, "Event Table", database);
        this.baseDir=baseDir;
        columns.put("controller", new ColumnFeatures("controller", "varchar2", 50, false));
        columns.put("application", new ColumnFeatures("application", "varchar2", 50, false));
        columns.put("id", new ColumnFeatures("id", "number", 22, false));
        columns.put("eventTime", new ColumnFeatures("eventTime", "number", 22, false));
        columns.put("type", new ColumnFeatures("type", "varchar2", 50, false));
        columns.put("subType", new ColumnFeatures("subType", "varchar2", 50, false));
        columns.put("severity", new ColumnFeatures("severity", "varchar2", 20, false));
        columns.put("summary", new ColumnFeatures("summary", "varchar2", 120, false));
        columns.put("triggeredEntityId", new ColumnFeatures("triggeredEntityId", "number", 22, false));
        columns.put("triggeredEntityName", new ColumnFeatures("triggeredEntityName", "varchar2", 50, true));
        columns.put("triggeredEntityType", new ColumnFeatures("triggeredEntityType", "varchar2", 50, false));
        columns.put("eventTimestamp", new ColumnFeatures("eventTimestamp", "date", -1, false));
    }

    @Override
    public synchronized int insert(Object object) {
        EventData event = (EventData) object;
        int counter=0;
        boolean writeHeaderLine = false;
        File dataFile = new File(baseDir.getAbsolutePath()+ File.separator + Utility.cleanFileName(this.name) +".csv");
        if( ! dataFile.isFile() ) {
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                logger.warn("Can not create output file! '%s' Exception: %s",dataFile.getAbsolutePath(), e.getMessage());
                return 0;
            }
            writeHeaderLine=true;
        }
        try {
            PrintWriter printStream = new PrintWriter( new FileWriter(dataFile, true));
            if( writeHeaderLine ) {
                /*
                Iterator<ColumnFeatures> it = this.columns.values().iterator();
                while( it.hasNext() ) {
                    ColumnFeatures columnFeatures = it.next();
                    printStream.print(columnFeatures.name);
                    if( it.hasNext() ) printStream.print(",");
                }
                printStream.println();
                */
                printStream.println("controller, application, id, eventTime, type, subtype, severity, summary, triggeredEntityId, triggeredEntityName, triggeredEntityType, eventTimestamp");
            }
            printStream.printf("\"%s\", ", event.controllerHostname);
            printStream.printf("\"%s\", ", event.applicationName);
            printStream.printf("%d, ", event.id);
            printStream.printf("%d, ", event.eventTime);
            printStream.printf("\"%s\", ", event.type);
            printStream.printf("\"%s\", ", event.subType);
            printStream.printf("\"%s\", ", event.severity);
            printStream.printf("\"%s\", ", event.summary);
            if( event.triggeredEntity != null ) {
                printStream.printf("%d, ", event.triggeredEntity.entityId);
                printStream.printf("\"%s\", ", event.triggeredEntity.name);
                printStream.printf("\"%s\", ", event.triggeredEntity.entityType);
            } else if( event.affectedEntities != null ) {
                printStream.printf("%d, ", event.affectedEntities.get(0).entityId);
                printStream.printf("\"%s\", ", event.affectedEntities.get(0).name);
                printStream.printf("\"%s\", ", event.affectedEntities.get(0).entityType);
            } else {
                printStream.printf("%d, ", -1);
                printStream.printf("\"%s\", ", "");
                printStream.printf("\"%s\", ", "");
            }
            printStream.printf("\"%s\"\n", new Date(event.eventTime).toString());
            counter++;
            printStream.flush();
            printStream.close();
        } catch (Exception exception) {
            logger.error("Error inserting events into %s, Exception: %s", dataFile.getAbsolutePath(), exception.toString());
        }
        return counter;
    }


}
