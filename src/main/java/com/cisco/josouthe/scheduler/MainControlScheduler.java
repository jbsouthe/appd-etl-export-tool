package com.cisco.josouthe.scheduler;

import com.cisco.josouthe.config.Configuration;
import com.cisco.josouthe.data.Analytics;
import com.cisco.josouthe.data.Application;
import com.cisco.josouthe.data.Controller;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.*;

public class MainControlScheduler {
    private static final Logger logger = LogManager.getFormatterLogger();
    Configuration configuration;
    private LinkedBlockingQueue<Object[]> dataToInsertLinkedBlockingQueue;
    private ThreadPoolExecutor executorFetchData;
    private ThreadPoolExecutor executorInsertData;
    private ScheduledThreadPoolExecutor executorConfigRefresh;
    private CountDownLatch fetchDataLatch;

    public MainControlScheduler(Configuration configuration ) {
        this.configuration = configuration;
        dataToInsertLinkedBlockingQueue = new LinkedBlockingQueue<>();
        executorFetchData = (ThreadPoolExecutor) Executors.newFixedThreadPool( this.configuration.getProperty("scheduler-NumberOfControllerThreads", 50), new NamedThreadFactory("ControllerAPI") );
        executorInsertData = (ThreadPoolExecutor) Executors.newFixedThreadPool( this.configuration.getProperty("scheduler-NumberOfDatabaseThreads", 50), new NamedThreadFactory("Database") );
        executorConfigRefresh = (ScheduledThreadPoolExecutor)  Executors.newScheduledThreadPool(this.configuration.getProperty("scheduler-NumberOfControllerThreads", 50), new NamedThreadFactory("ConfigRefresh"));
        for( Controller controller : configuration.getControllerList() ) {
            for (Application application : controller.applications) {
                executorConfigRefresh.scheduleAtFixedRate(new ApplicationMetricRefreshTask(application), 0, this.configuration.getProperty("scheduler-ConfigRefreshHours", 12l), TimeUnit.HOURS);
            }
        }
    }

    public void run() {
        for( int i=0; i < this.configuration.getProperty("scheduler-NumberOfDatabaseThreads", 50); i++) {
            executorInsertData.execute( new DatabaseInsertTask(configuration, configuration.getDatabase(), dataToInsertLinkedBlockingQueue));
        }
        logger.info("Started %d Database Insert Tasks, all looking for work", executorInsertData.getPoolSize());
        while(configuration.isRunning() ) {
            int numberOfJobs = 0;

            for( Controller controller : configuration.getControllerList() ) {
                for (Application application : controller.applications) {
                    numberOfJobs += 2;
                }
            }
            if( configuration.getAnalyticsList() != null )
                numberOfJobs += configuration.getAnalyticsList().length;

            this.fetchDataLatch = new CountDownLatch(numberOfJobs);

            for( Controller controller : configuration.getControllerList() ) {
                for(Application application : controller.applications ) {
                    logger.info("Running collector for %s@%s", application.getName(), controller.hostname);
                    executorFetchData.execute(new ApplicationMetricTask( application, dataToInsertLinkedBlockingQueue, fetchDataLatch));
                    executorFetchData.execute( new ApplicationEventTask( application, dataToInsertLinkedBlockingQueue, fetchDataLatch));
                }
            }

            for(Analytics analytic : configuration.getAnalyticsList() ) {
                executorFetchData.execute( new AnalyticsSearchTask( analytic, dataToInsertLinkedBlockingQueue, fetchDataLatch) );
            }
            sleep(200);
            try {
                logger.debug("starting await for fetchDataLatch awaiting %d tasks", fetchDataLatch.getCount());
                fetchDataLatch.await();
                logger.debug("finished fetchDataLatch.await() %d jobs (expecting 0) toString: '%s'", fetchDataLatch.getCount(), fetchDataLatch.toString());
            } catch (InterruptedException ignored) {}

            if( configuration.getProperty("scheduler-enabled", true) ) {
                logger.info("MainControlScheduler is enabled, so sleeping for %d minutes and running again", configuration.getProperty("scheduler-pollIntervalMinutes", 60L));
                sleep( configuration.getProperty("scheduler-pollIntervalMinutes", 60L) * 60000 );
                logger.info("MainControlScheduler is awakened and running once more");
            } else {
                sleep(5000);
                for( Controller controller : configuration.getControllerList() ) {
                   logger.debug("Waiting for Controller %s to finish initializing all %d applications",controller.hostname, controller.applications.length);
                   for (Application application : controller.applications) {
                        logger.debug("Waiting for Application %s to finish initializing",application.getName());
                        while( ! application.isFinishedInitialization() ) sleep(10000 );
                        logger.debug("Application %s finished initializing",application.getName());
                   }
                   logger.debug("Controller %s finished initializing",controller.hostname);
                }
                executorConfigRefresh.shutdownNow();
                sleep(5000);
                logger.info("MainControlScheduler is disabled, so exiting when database queue is drained");
                while(!dataToInsertLinkedBlockingQueue.isEmpty()) {
                    sleep(5000);
                }
                configuration.setRunning(false);
                sleep(10000); //so database workers can finish up
                executorInsertData.shutdown();
                executorFetchData.shutdown();
            }
        }

    }

    private void sleep( long forMilliseconds ) {
        try {
            Thread.sleep( forMilliseconds );
        } catch (InterruptedException ignored) { }
    }
}
