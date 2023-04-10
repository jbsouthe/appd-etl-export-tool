package com.cisco.josouthe.http;

import com.cisco.josouthe.util.Utility;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;

public class WorkingStatusThread extends Thread {
    private String name,query;
    private Logger logger = null;
    private long cycleTime = 30000;
    private long startTime;
    private boolean running = true;

    public WorkingStatusThread( String name, String query, long cycleTime, Logger logger ) {
        super();
        this.name=name;
        this.query=query;
        this.cycleTime=cycleTime;
        this.logger=logger;
        this.startTime = Utility.now();
    }

    public WorkingStatusThread(String name, String query, Logger logger ) {
        this(name, query, 30000, logger);
        //if in debug set more verbose messages, trace, even more
        if( logger.isDebugEnabled() ) this.cycleTime=10000;
        if( logger.isTraceEnabled() ) this.cycleTime=3000;

    }

    public void cancel() { running=false; }

    @Override
    public void run() {
        while( running ) {
            try { Thread.sleep(cycleTime); } catch (InterruptedException e) { /*ignore */ }
            if( running ) {
                String holdName = Thread.currentThread().getName();
                Thread.currentThread().setName("Execution-Watchdog-"+ name);
                long runtimeSeconds = getRuntimeSeconds();
                logger.log( getLogLevel(runtimeSeconds), "%s '%s' is still running, so far %d (s)", this.name, this.query, runtimeSeconds );
                Thread.currentThread().setName(holdName);
            }
        }
    }

    private Level getLogLevel(long runtimeSeconds) {
        if( runtimeSeconds <= 300 ) return Level.INFO;
        if( runtimeSeconds <= 600 ) return Level.WARN;
        return Level.ERROR;
    }

    private long getRuntimeSeconds() {
        return (Utility.now()-this.startTime)/1000;
    }
}
