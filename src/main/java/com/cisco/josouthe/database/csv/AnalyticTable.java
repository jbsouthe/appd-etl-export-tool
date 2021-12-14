package com.cisco.josouthe.database.csv;

import com.cisco.josouthe.data.analytic.Field;
import com.cisco.josouthe.data.analytic.Result;
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

public class AnalyticTable extends CSVTable implements com.cisco.josouthe.database.AnalyticTable {
    protected static final Logger logger = LogManager.getFormatterLogger();
    private File baseDir;
    private String headerLine;

    public AnalyticTable(Result result, Database database, File baseDir ) {
        super(result.targetTable, "Analytic Table", database);
        this.baseDir=baseDir;
        columns.put("startTimestamp", new ColumnFeatures("startTimestamp", "date", -1, false));
        columns.put("endTimestamp", new ColumnFeatures("endTimestamp", "date", -1, false));
        this.headerLine = "startTimestamp, endTimestamp";
        for(Field field: result.fields) {
            this.headerLine += ", "+ field.label;
            columns.put( field.label,
                    new ColumnFeatures(database.convertToAcceptableColumnName(field.label, columns.values()),
                            getColumnTypeForName(field.type),
                            getColumnSizeForName(field.type),
                            true)
            );
            logger.debug("Added column for field %s column definition: %s", field.label, columns.get(field.label));
        }
    }

    private int getColumnSizeForName(String type) {
        switch(type) {
            case "string": return database.STRING_SIZE;
            case "integer": return database.INTEGER_SIZE;
            case "date": return database.DATE_SIZE;
            default: {
                logger.warn("Unknown data type: %s setting table column size to %s", type, database.STRING_SIZE);
            }
        }
        return database.STRING_SIZE;
    }

    private String getColumnTypeForName(String type) {
        switch(type) {
            case "string": return database.STRING_TYPE;
            case "integer": return database.INTEGER_TYPE;
            case "date": return database.DATE_TYPE;
            default: {
                logger.warn("Unknown data type: %s setting table column type to %s", type, database.STRING_TYPE);
            }
        }
        return database.STRING_TYPE;
    }

    @Override
    public synchronized int insert(Object object) {
        Result result = (Result) object;
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
                printStream.println(this.headerLine);
            }
            for( int dataIndex=0; dataIndex<result.results.length; dataIndex++) {
                int parameterIndex = 1;
                for (String key : columns.keySet()) {
                    switch (key) {
                        case "startTimestamp": { printStream.printf("\"%s\", ", new Date(result.startTimestamp).toString()); break; }
                        case "endTimestamp": { printStream.printf("\"%s\"", new Date(result.endTimestamp).toString()); break; }
                        default: {
                            int fieldIndex=0;
                            for (Field field : result.fields) {
                                if( field.label.equals(key)) {
                                    Object data = result.results[dataIndex][fieldIndex];
                                    if( data == null ) {
                                        printStream.print(", ");
                                    } else {
                                        if( data instanceof Integer ) {
                                            printStream.printf(", %d", (Integer) data);
                                        } else if( data instanceof Long ) {
                                            printStream.printf(", %d", (Long) data);
                                        } else if( data instanceof Double ) {
                                            printStream.printf(", %f", (Double) data);
                                        } else {
                                            printStream.printf(", \"%s\"", String.valueOf(data));
                                        }
                                    }
                                }
                                fieldIndex++;
                            }
                        }
                    }
                }
                printStream.println();
                counter++;
            }
            printStream.flush();
            printStream.close();
        } catch (Exception exception) {
            logger.error("Error inserting analytics data into %s, Exception: %s", name, exception.toString());
        }
        return counter;
    }
}
