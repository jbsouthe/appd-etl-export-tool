package com.cisco.josouthe.data;

import com.cisco.josouthe.data.event.EventData;
import com.cisco.josouthe.data.metric.Baseline;
import com.cisco.josouthe.data.metric.MetricData;
import com.cisco.josouthe.data.metric.MetricGraph;
import com.cisco.josouthe.data.metric.MetricPaths;
import com.cisco.josouthe.data.model.TreeNode;
import com.cisco.josouthe.exceptions.InvalidConfigurationException;
import com.cisco.josouthe.http.WorkingStatusThread;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

public class Application {
    private static final Logger logger = LogManager.getFormatterLogger();
    private Controller controller;
    private boolean finishedInitialization = false;
    public boolean getAllAvailableMetrics = true;
    public boolean getAllEvents = false;
    public boolean getAllHealthRuleViolations = false;
    public String name;
    public long id = -1;
    public boolean defaultDisableDataRollup = false;
    public String defaultMetricTableName = null;
    public String defaultEventTableName = null;
    public String defaultBaselineTableName = null;
    public String eventTypeList = null;
    public String eventSeverities = "INFO,WARN,ERROR";
    public MetricGraph metricGraph = null;
    public List<Baseline> baselines = null;
    public Baseline defaultBaseline = null;
    private MetricPaths metricPaths = null;
    private int granularityMinutes = 1;
    private boolean onlyGetDefaultBaseline = true;

    public Application(boolean getAllAvailableMetrics,
                       String name,
                       boolean defaultDisableDataRollup,
                       String defaultMetricTableName,
                       String defaultEventTableName,
                       String defaultBaselineTableName,
                       boolean getAllEvents,
                       boolean getAllHealthRuleViolations,
                       List<String> metrics,
                       int granularityMinutes,
                       boolean onlyGetDefaultBaseline) {
        this.getAllAvailableMetrics = getAllAvailableMetrics;
        this.getAllEvents = getAllEvents;
        this.getAllHealthRuleViolations = getAllHealthRuleViolations;
        this.name = name;
        this.defaultDisableDataRollup = defaultDisableDataRollup;
        if( defaultMetricTableName != null ) this.defaultMetricTableName = defaultMetricTableName;
        if( defaultEventTableName != null ) this.defaultEventTableName = defaultEventTableName;
        if( defaultBaselineTableName != null ) this.defaultBaselineTableName = defaultBaselineTableName;
        this.metricGraph = new MetricGraph(metrics);
        this.baselines = new ArrayList<>();
        this.metricPaths = new MetricPaths();
        this.granularityMinutes = granularityMinutes;
        this.onlyGetDefaultBaseline = onlyGetDefaultBaseline;
    }

    public boolean isFinishedInitialization() { return finishedInitialization; }
    public void setEventTypeList( String events ) { this.eventTypeList=events; }
    public int getGranularityMinutes() { return granularityMinutes; }

    public void validateConfiguration(Controller controller) throws InvalidConfigurationException {
        if( !getAllAvailableMetrics && metricGraph.size() == 0 && !getAllEvents && !getAllHealthRuleViolations ) {
            logger.warn("getAllAvailableMetrics, getAllEvents, and getAllHealthRuleViolations are false, but the application has no metrics configured, not sure what to do here so i'm just going to toss this Exception");
            throw new InvalidConfigurationException("getAllAvailableMetrics, getAllEvents, and getAllHealthRuleViolations are false, but the application has no metrics configured, not sure what to do here so i'm just going to toss this Exception");
        }
        this.controller=controller;
        setBaselines( controller.getAllBaselines(this) );
        if( ! getAllAvailableMetrics ) this.finishedInitialization=true;
        //this.refreshAllAvailableMetricsIfEnabled(); moved to beginning of scheduler, to get some concurrency
    }

    public void refreshAllAvailableMetricsIfEnabled() {
        synchronized (this.metricsToAdd) {
            if( getAllAvailableMetrics ) {
                WorkingStatusThread workingStatusThread = new WorkingStatusThread("Refresh Metrics", "Recursive Tree Walk", logger);
                workingStatusThread.start();
                TreeNode[] folders = controller.getApplicationMetricFolders(this, "Application Infrastructure Performance");
                logger.debug("Found %d folders we can go into", (folders == null ? "0" : folders.length));
                findMetrics( controller, folders, "");
                workingStatusThread.cancel();
                workingStatusThread = new WorkingStatusThread("Refresh Metrics", "Build Metric Graph", logger);
                workingStatusThread.start();
                this.metricGraph = new MetricGraph(metricsToAdd);
                this.metricGraph.addMetricNames( this.metricPaths.getMetricPaths() );
                workingStatusThread.cancel();
                this.metricsToAdd.clear(); //possible memory leak, moving to the end of this method instead of beginning
            }
        }
        this.finishedInitialization=true; //setting this here because we want to continue, even if partial data
    }

    private ArrayList<String> metricsToAdd = new ArrayList<>();
    private void findMetrics(Controller controller, TreeNode[] somethings, String path) {
        if( somethings == null || somethings.length == 0 ) return;
        if( !"".equals(path) ) path += "|";

        for( TreeNode something : somethings ) {
            if( something.isFolder() ) {
                findMetrics( controller, controller.getApplicationMetricFolders(this, path+something.name), path+something.name);
            } else if( "Custom Metrics".contains(path + something.name)){
                logger.debug("Adding metric: %s%s",path,something.name);
                metricsToAdd.add(path+something.name);
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

    private void writeMetricListToFile( String[] metrics ) {
        try {
            BufferedWriter out = new BufferedWriter( new FileWriter( String.format("Metrics-%s.txt",this.name)));
            for( String metric: metrics ){
                out.write(metric);
                out.newLine();
            }
            out.flush();
            out.close();
        } catch (IOException ioException) {
            logger.warn("Exception while trying to write metrics to temp file Metrics-%s.txt for dev, Exception: %s", this.getName(), ioException.getMessage() );
        }
    }

    public void setBaselines(Baseline[] baselines) {
        if( baselines == null ) return;
        this.baselines.clear();
        for( Baseline baseline : baselines ) {
            this.baselines.add(baseline);
            if( baseline.defaultBaseline ) this.defaultBaseline=baseline;
        }
    }

    public Baseline getDefaultBaseline (String name ) {
        if( name == null ) return this.defaultBaseline;
        for( Baseline baseline : baselines )
            if( name.equals(baseline.name) )
                return baseline;
        return null;
    }
    public Baseline getDefaultBaseline () { return getDefaultBaseline(null); }

    public boolean isOnlyGetDefaultBaselineFlagSet() { return this.onlyGetDefaultBaseline; }

    public List<Baseline> getAllBaselines() { return this.baselines; }
}
