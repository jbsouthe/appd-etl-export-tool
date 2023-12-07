package com.cisco.josouthe.database.csv;

import com.cisco.josouthe.database.ColumnFeatures;
import com.cisco.josouthe.database.ControlEntry;
import com.cisco.josouthe.database.Database;
import com.cisco.josouthe.database.IControlTable;
import com.cisco.josouthe.util.Utility;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

public class ControlTable extends CSVTable implements IControlTable {
    protected static final Logger logger = LogManager.getFormatterLogger();
    private Integer defaultLoadNumberOfHoursIfControlRowMissing = 48;
    private File baseDir;

    public ControlTable( String tableName, Database database, File baseDir ) {
        super(tableName,"Control Table", database);
        this.baseDir=baseDir;
        columns.put("controller", new ColumnFeatures("controller", "varchar2", 120, false));
        columns.put("application", new ColumnFeatures("application", "varchar2", 120, false));
        columns.put("dataType", new ColumnFeatures("dataType", "varchar2", 50, false));
        columns.put("lastRunTimestamp", new ColumnFeatures("lastRunTimestamp", "number", 22, false));
    }

    @Override
    public void setDefaultLoadNumberOfHoursIfControlRowMissing(int hours) {
        this.defaultLoadNumberOfHoursIfControlRowMissing=hours;
        logger.trace("Default Load Number of hours if control row missing set to: "+ hours);
    }

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
        File controlFile = getControlFile(controlEntry);
        long defaultAdjustmentInMS = this.defaultLoadNumberOfHoursIfControlRowMissing.longValue() * 60 * 60 * 1000;
        controlEntry.timestamp = System.currentTimeMillis() - defaultAdjustmentInMS;
        logger.trace("XXX %s should be %d - %d = %d", controlEntry, System.currentTimeMillis(), defaultAdjustmentInMS, System.currentTimeMillis() - defaultAdjustmentInMS);
        if( controlFile.isFile() ) {
            try {
                controlEntry.timestamp = readTimestampFromFile(controlFile);
            } catch (IOException ioException) {
                logger.warn("Error reading timestamp from control file '%s', IOException: %s",controlFile.getAbsolutePath(), ioException.getMessage());
            }
        }
        logger.trace("Returning Control Entry: %s default hours: %d current time: %d", controlEntry, this.defaultLoadNumberOfHoursIfControlRowMissing, System.currentTimeMillis());
        return controlEntry;
    }

    private File getControlFile( ControlEntry controlEntry ) {
        return new File( baseDir.getAbsolutePath() + File.separator + ".lastRunTime-"+ Utility.cleanFileName( controlEntry.controller+"-"+controlEntry.application+"-"+controlEntry.type));
    }

    private long readTimestampFromFile(File controlFile) throws IOException {
        if( controlFile == null ) throw new IOException("Can not read null control file?!");
        if( !controlFile.canRead() ) throw new IOException("Can not read control file! "+ controlFile.getAbsolutePath());
        logger.debug("Loading file: %s",controlFile);
        BufferedReader reader = new BufferedReader( new InputStreamReader( new FileInputStream(controlFile)));
        String word;
        while ( (word = reader.readLine()) != null )
            return Long.parseLong(word);
        throw new IOException("Was not able to read the last run timestamp from the control file");
    }

    @Override
    public synchronized int setLastRunTimestamp(ControlEntry controlEntry) {
        File controlFile = getControlFile(controlEntry);
        if( controlFile.isFile() ) controlFile.delete();
        try {
            controlFile.createNewFile();
        } catch (IOException e) {
            logger.warn("Unable to create control file '%s'",controlFile.getAbsolutePath());
        }
        try {
            BufferedWriter writer = new BufferedWriter( new OutputStreamWriter( new FileOutputStream(controlFile)));
            writer.write(String.valueOf(controlEntry.timestamp));
            writer.newLine();
            writer.close();
            return 1;
        } catch (FileNotFoundException e) {
            logger.warn("File not found '%s'", e.getMessage());
        } catch (IOException ioException) {
            logger.warn("Error writing to control file '%s' IOException: %s",controlFile.getAbsolutePath(), ioException.getMessage());
        }
        return 0;
    }

    public List<ControlEntry> getControlEntries() {
        List<ControlEntry> entries = new ArrayList<>();
        logger.warn("Sorry CSV files do not support this feature");
        return entries;
    }
}
