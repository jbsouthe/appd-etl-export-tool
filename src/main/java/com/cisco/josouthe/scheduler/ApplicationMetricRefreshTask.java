package com.cisco.josouthe.scheduler;

import com.cisco.josouthe.data.Application;
import com.cisco.josouthe.data.Controller;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ApplicationMetricRefreshTask implements Runnable{
    private static final Logger logger = LogManager.getFormatterLogger();

    private Application application;

    public ApplicationMetricRefreshTask(Application application ) {
        this.application=application;
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
        logger.debug("Beginning configuration refresh for application %s",application.name);
        this.application.refreshAllAvailableMetricsIfEnabled();
        logger.debug("Finished configuration refresh for application %s",application.name);
    }
}
