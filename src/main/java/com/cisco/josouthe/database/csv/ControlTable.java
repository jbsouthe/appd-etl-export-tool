package com.cisco.josouthe.database.csv;

import com.cisco.josouthe.database.ColumnFeatures;
import com.cisco.josouthe.database.ControlEntry;
import com.cisco.josouthe.database.Database;
import com.cisco.josouthe.database.Table;
import com.cisco.josouthe.util.Utility;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;

public class ControlTable extends Table implements com.cisco.josouthe.database.ControlTable {
    protected static final Logger logger = LogManager.getFormatterLogger();
    private int defaultLoadNumberOfHoursIfControlRowMissing = 48;
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
        File controlFile = getControlFile(controlEntry);
        if( controlFile.isFile() ) {
            try {
                controlEntry.timestamp = readTimestampFromFile(controlFile);
            } catch (IOException ioException) {
                logger.warn("Error reading timestamp from control file '%s', IOException: %s",controlFile.getAbsolutePath(), ioException.getMessage());
                controlEntry.timestamp= Utility.now()-(this.defaultLoadNumberOfHoursIfControlRowMissing*60*60*1000);
            }
        } else {
            controlEntry.timestamp= Utility.now()-(this.defaultLoadNumberOfHoursIfControlRowMissing*60*60*1000);
        }
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


}
