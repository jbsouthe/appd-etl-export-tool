package com.cisco.josouthe.database.csv;

import com.cisco.josouthe.data.metric.MetricData;
import com.cisco.josouthe.data.metric.MetricValue;
import com.cisco.josouthe.database.ColumnFeatures;
import com.cisco.josouthe.database.Database;
import com.cisco.josouthe.database.IMetricTable;
import com.cisco.josouthe.util.Utility;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.util.Date;


public class MetricTable extends CSVTable implements IMetricTable {
    protected static final Logger logger = LogManager.getFormatterLogger();
    private File baseDir;

    public MetricTable( String tableName, Database database, File baseDir ) {
        super(tableName,"Metric Table",database);
        this.baseDir=baseDir;
        columns.put("controller", new ColumnFeatures("controller", "string", -1, false));
        columns.put("application", new ColumnFeatures("application", "string", -1, false));
        columns.put("metricname", new ColumnFeatures("metricName", "string", -1, false));
        columns.put("metricpath", new ColumnFeatures("metricPath", "string", -1, false));
        columns.put("frequency", new ColumnFeatures("frequency", "string", -1, false));
        columns.put("userange", new ColumnFeatures("userange", "boolean", -1, false));
        for( String columnName : new String[] { "metricid","startTimeInMillis", "occurrences", "currentValue", "min", "max", "count", "sum", "value", "standardDeviation"})
            columns.put(columnName.toLowerCase(), new ColumnFeatures(columnName, "number", -1, false));
        columns.put("startTimestamp", new ColumnFeatures("startTimestamp", "date", -1, false));
    }

    public synchronized int insert(Object object) {
        MetricData metric = (MetricData) object;
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
                printStream.print("controller, application, metricname, metricpath, frequency, metricid, userange, ");
                printStream.println("startTimeInMillis, occurrences, currentvalue, min, max, count, sum, value, standardDeviation, startTimestamp");
            }
            for( MetricValue metricValue : metric.metricValues ) {
                printStream.printf("\"%s\", ", metric.controllerHostname);
                printStream.printf("\"%s\", ", metric.applicationName);
                printStream.printf("\"%s\", ", metric.metricName);
                printStream.printf("\"%s\", ", metric.metricPath);
                printStream.printf("\"%s\", ", metric.frequency);
                printStream.printf("%s, ", metric.metricId);
                printStream.printf("%s, ", String.valueOf(metricValue.useRange));
                printStream.printf("%d, ", metricValue.startTimeInMillis);
                printStream.printf("%d, ", metricValue.occurrences);
                printStream.printf("%d, ", metricValue.current);
                printStream.printf("%d, ", metricValue.min);
                printStream.printf("%d, ", metricValue.max);
                printStream.printf("%d, ", metricValue.count);
                printStream.printf("%d, ", metricValue.sum);
                printStream.printf("%d, ", metricValue.value);
                printStream.printf("%f, ", metricValue.standardDeviation);
                printStream.printf("\"%s\"\n", new Date(metricValue.startTimeInMillis).toString());
                printStream.flush();
                counter++;
            }
            printStream.close();
        } catch (FileNotFoundException e) {
            logger.warn("File not found! '%s'", dataFile.getAbsolutePath());
            return 0;
        } catch (IOException ioException) {
            logger.warn("File IO Exception: %s",ioException);
            return 0;
        }
        return counter;
    }
}
