package com.cisco.josouthe.scheduler;

import com.cisco.josouthe.data.Analytics;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.LinkedBlockingQueue;

public class AnalyticsSearchTask implements Runnable {
    private static final Logger logger = LogManager.getFormatterLogger();

    private Analytics analytics;
    private LinkedBlockingQueue<Object[]> dataToInsertLinkedBlockingQueue;

    public AnalyticsSearchTask(Analytics analytic, LinkedBlockingQueue<Object[]> dataToInsertLinkedBlockingQueue) {
        this.analytics=analytic;
        this.dataToInsertLinkedBlockingQueue=dataToInsertLinkedBlockingQueue;
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
        this.analytics.getAllSearches(dataToInsertLinkedBlockingQueue);
    }
}
