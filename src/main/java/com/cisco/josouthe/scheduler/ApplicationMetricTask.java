package com.cisco.josouthe.scheduler;

import com.cisco.josouthe.data.Application;
import com.cisco.josouthe.data.Controller;
import com.cisco.josouthe.data.metric.MetricData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.LinkedBlockingQueue;

public class ApplicationMetricTask implements Runnable{
    private static final Logger logger = LogManager.getFormatterLogger();

    private Application application;
    private Controller controller;
    private LinkedBlockingQueue<Object[]> dataQueue;

    public ApplicationMetricTask(Application application, Controller controller, LinkedBlockingQueue<Object[]> dataQueue) {
        this.application=application;
        this.controller=controller;
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
        this.controller.getAllMetrics(this.application, dataQueue);
    }
}
