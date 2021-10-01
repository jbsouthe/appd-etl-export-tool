package com.cisco.josouthe;

import com.cisco.josouthe.data.Controller;
import com.cisco.josouthe.data.MetricData;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Scheduler {
    private static final Logger logger = LogManager.getFormatterLogger();
    Configuration configuration;
    HttpClient httpClient;

    public Scheduler(Configuration configuration ) {
        this.configuration = configuration;
        this.httpClient = HttpClients.createDefault();
    }

    public void run() {
        boolean keepRunning=true;
        while( keepRunning ) {
            for( Controller controller : configuration.getControllerList() ) {
                MetricData[] metricData = controller.getAllMetricsForAllApplications();
                logger.info("Controller %s Collected %d Metrics for import into the Database", controller.hostname, metricData.length);
                configuration.getDatabase().importMetricData( metricData );
            }
            if( configuration.getPropertyAsBoolean("scheduler-enabled", true) ) {
                logger.info("Scheduler is enabled, so sleeping for %d minutes and running again", configuration.getPropertyAsLong("scheduler-pollIntervalMinutes", 60L));
                try {
                    Thread.sleep( configuration.getPropertyAsLong("scheduler-pollIntervalMinutes", 60L) * 60000 );
                } catch (InterruptedException ignored) { }
            } else {
                logger.info("Scheduler is disabled, so exiting now");
                keepRunning=false;
            }
        }

    }
}
