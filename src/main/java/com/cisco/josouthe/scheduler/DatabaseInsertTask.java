package com.cisco.josouthe.scheduler;

import com.cisco.josouthe.Configuration;
import com.cisco.josouthe.database.Database;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class DatabaseInsertTask implements Runnable{
    private static final Logger logger = LogManager.getFormatterLogger();

    private Configuration configuration;
    private Database database;
    private LinkedBlockingQueue<Object[]> dataQueue;

    public DatabaseInsertTask( Configuration configuration, Database database, LinkedBlockingQueue<Object[]> dataQueue ) {
        this.configuration=configuration;
        this.database=database;
        this.dataQueue=dataQueue;
    }
    /**
     * When an object implementing interface {@code Runnable} is used
     * to create a thread, starting the thread causes the object's
     * {@code run} method to be called in that separately executing
     * thread.
     * <p>
     * The general contract of the method {@code run} is that it may
     * take any action whatsoever.
     *
     * @see Thread#run()
     */
    @Override
    public void run() {
        while( configuration.isRunning() || !dataQueue.isEmpty() ) {
            try {
                Object[] data = dataQueue.poll(5000, TimeUnit.MILLISECONDS);
                if( data != null ) {
                    logger.debug("Poll returned %d data elements to insert into the database", (data == null ? 0 : data.length));
                    this.database.importData(data);
                }
            } catch (InterruptedException ignored) {
                //ignore it
            }
        }
        logger.debug("Shutting down database Insert Task");
    }
}
