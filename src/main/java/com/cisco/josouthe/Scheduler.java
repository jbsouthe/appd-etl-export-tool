package com.cisco.josouthe;

import com.appdynamics.agent.api.AppdynamicsAgent;
import com.appdynamics.agent.api.EntryTypes;
import com.appdynamics.agent.api.Transaction;
import com.cisco.josouthe.data.Analytics;
import com.cisco.josouthe.data.Controller;
import com.cisco.josouthe.data.analytic.Result;
import com.cisco.josouthe.data.event.EventData;
import com.cisco.josouthe.data.metric.MetricData;
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
                Transaction transaction = AppdynamicsAgent.startTransaction( "Export Application Metrics", null, EntryTypes.POJO, false);
                //Get Metrics
                MetricData[] metricData = controller.getAllMetricsForAllApplications();
                long countOfMetrics = 0;
                for( MetricData metric : metricData )
                    countOfMetrics += metric.metricValues.size();
                logger.info("Controller %s Collected %d Metrics for import into the Database", controller.hostname, countOfMetrics);
                configuration.getDatabase().importMetricData( metricData );
                transaction.end();

                transaction = AppdynamicsAgent.startTransaction( "Export Application Events", null, EntryTypes.POJO, false);
                //Get Events
                EventData[] events = controller.getAllEventsForAllApplications();
                long countOfEvents = events.length;
                logger.info("Controller %s Collected %d Events for import into the Database", controller.hostname, countOfEvents);
                if( countOfEvents > 0 ) configuration.getDatabase().importEventData( events );
                transaction.end();

                //Get Health Rule Violations

            }

            for(Analytics analytic : configuration.getAnalyticsList() ) {
                Transaction transaction = AppdynamicsAgent.startTransaction( "Export Analytics Searches", null, EntryTypes.POJO, false);
                //get Analytics search results
                Result[] results = analytic.getAllSearches();
                if( results == null ) {
                    logger.warn("No results from analytics searches?");
                } else {
                    logger.info("Analytics account %s collected %d search results for import into the Database", analytic.APIAccountName, results.length);
                    configuration.getDatabase().importAnalyticData( results );
                }
                transaction.end();
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
