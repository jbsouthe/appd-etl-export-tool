package com.cisco.josouthe.data;

import com.cisco.josouthe.data.event.EventData;
import com.cisco.josouthe.data.metric.ApplicationMetric;
import com.cisco.josouthe.data.metric.MetricData;
import com.cisco.josouthe.data.metric.MetricGraph;
import com.cisco.josouthe.data.model.TreeNode;
import com.cisco.josouthe.exceptions.InvalidConfigurationException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;

public class Application {
    private static final Logger logger = LogManager.getFormatterLogger();
    private Controller controller;
    private boolean finishedInitialization = false;
    public boolean getAllAvailableMetrics = true;
    public boolean getAllEvents = false;
    public boolean getAllHealthRuleViolations = false;
    public String name;
    public int id;
    public String defaultDisableDataRollup = "false";
    public String defaultMetricTableName = null;
    public String defaultEventTableName = null;
    public String eventTypeList = null;
    public String eventSeverities = "INFO,WARN,ERROR";
    public ApplicationMetric[] metrics = null;


    public Application(String getAllAvailableMetrics, String name, String defaultDisableDataRollup, String defaultMetricTableName, String defaultEventTableName, String getAllEvents, String getAllHealthRuleViolations, ApplicationMetric[] metrics) {
        if( getAllAvailableMetrics != null ) this.getAllAvailableMetrics= Boolean.parseBoolean(getAllAvailableMetrics);
        if( getAllEvents != null ) this.getAllEvents= Boolean.parseBoolean(getAllEvents);
        if( getAllHealthRuleViolations != null ) this.getAllHealthRuleViolations= Boolean.parseBoolean(getAllHealthRuleViolations);
        this.name = name;
        if( defaultDisableDataRollup != null ) this.defaultDisableDataRollup = defaultDisableDataRollup;
        if( defaultMetricTableName != null ) this.defaultMetricTableName = defaultMetricTableName;
        if( defaultEventTableName != null ) this.defaultEventTableName = defaultEventTableName;
        this.metrics = metrics;
    }

    public boolean isFinishedInitialization() { return finishedInitialization; }
    public void setEventTypeList( String events ) { this.eventTypeList=events; }

    public void validateConfiguration(Controller controller) throws InvalidConfigurationException {
        if( !getAllAvailableMetrics && (metrics == null || metrics.length == 0) && !getAllEvents && !getAllHealthRuleViolations) {
            logger.warn("getAllAvailableMetrics, getAllEvents, and getAllHealthRuleViolations are false, but the application has no metrics configured, not sure what to do here so i'm just going to toss this Exception");
            throw new InvalidConfigurationException("getAllAvailableMetrics, getAllEvents, and getAllHealthRuleViolations are false, but the application has no metrics configured, not sure what to do here so i'm just going to toss this Exception");
        }
        this.controller=controller;
        if( ! getAllAvailableMetrics ) this.finishedInitialization=true;
        //this.refreshAllAvailableMetricsIfEnabled(); moved to beginning of scheduler, to get some concurrency
    }

    public void refreshAllAvailableMetricsIfEnabled() {
        synchronized (this.metricsToAdd) {
            this.metricsToAdd.clear();
            if( getAllAvailableMetrics ) {
                TreeNode[] folders = controller.getApplicationMetricFolders(this, "");
                logger.debug("Found %d folders we can go into", (folders == null ? "0" : folders.length));
                findMetrics( controller, folders, "");
                MetricGraph graph = new MetricGraph();
                this.metrics = graph.compress(metricsToAdd); //metricsToAdd.toArray( new ApplicationMetric[0] );
                //if( logger.isDebugEnabled() ) writeMetricListToFile( metricsToAdd.toArray( new ApplicationMetric[0] ) );
            }
        }
        this.finishedInitialization=true; //setting this here because we want to continue, even if partial data
    }

    private ArrayList<ApplicationMetric> metricsToAdd = new ArrayList<>();
    private void findMetrics(Controller controller, TreeNode[] somethings, String path) {
        if( somethings == null || somethings.length == 0 ) return;
        if( !"".equals(path) ) path += "|";

        for( TreeNode something : somethings ) {
            if( something.isFolder() ) {
                findMetrics( controller, controller.getApplicationMetricFolders(this, path+something.name), path+something.name);
            } else {
                logger.debug("Adding metric: %s%s",path,something.name);
                metricsToAdd.add(new ApplicationMetric(defaultDisableDataRollup, path+something.name));
            }
        }
    }

    public synchronized ArrayList<MetricData> getAllMetrics(LinkedBlockingQueue<Object[]> dataQueue ) {
        return this.controller.getAllMetrics(this, dataQueue);
    }

    public synchronized ArrayList<EventData> getAllEvents(LinkedBlockingQueue<Object[]> dataQueue ) {
        return this.controller.getAllEvents(this, dataQueue);
    }

    public String getName() { return this.name; }

    private void writeMetricListToFile( ApplicationMetric[] metrics ) {
        try {
            BufferedWriter out = new BufferedWriter( new FileWriter( String.format("Metrics-%s.txt",this.name)));
            for( ApplicationMetric metric: metrics ){
                out.write(metric.name);
                out.newLine();
            }
            out.flush();
            out.close();
        } catch (IOException ioException) {
            logger.warn("Exception while trying to write metrics to temp file Metrics-%s.txt for dev, Exception: %s", this.getName(), ioException.getMessage() );
        }
    }
}
