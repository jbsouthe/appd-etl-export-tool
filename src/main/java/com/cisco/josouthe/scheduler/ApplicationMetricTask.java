package com.cisco.josouthe.scheduler;

import com.cisco.josouthe.data.Application;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;

public class ApplicationMetricTask implements Runnable{
    private static final Logger logger = LogManager.getFormatterLogger();

    private Application application;
    private LinkedBlockingQueue<Object[]> dataQueue;
    private CountDownLatch countDownLatch;

    public ApplicationMetricTask(Application application, LinkedBlockingQueue<Object[]> dataQueue, CountDownLatch fetchDataLatch) {
        this.application=application;
        this.dataQueue=dataQueue;
        this.countDownLatch=fetchDataLatch;
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
        while( ! this.application.isFinishedInitialization() ) {
            try {
                Thread.sleep(10000);
            } catch (InterruptedException ignored) { }
        }
        this.application.getAllMetrics(dataQueue);
        this.countDownLatch.countDown();
    }
}
