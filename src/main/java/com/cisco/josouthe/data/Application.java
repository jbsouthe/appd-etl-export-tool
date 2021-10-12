package com.cisco.josouthe.data;

import com.cisco.josouthe.data.metric.ApplicationMetric;
import com.cisco.josouthe.data.model.TreeNode;
import com.cisco.josouthe.exceptions.InvalidConfigurationException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;

public class Application {
    private static final Logger logger = LogManager.getFormatterLogger();
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

    public void setEventTypeList( String events ) { this.eventTypeList=events; }

    public void validateConfiguration(Controller controller) throws InvalidConfigurationException {
        if( !getAllAvailableMetrics && (metrics == null || metrics.length == 0) && !getAllEvents && !getAllHealthRuleViolations) {
            logger.warn("getAllAvailableMetrics, getAllEvents, and getAllHealthRuleViolations are false, but the application has no metrics configured, not sure what to do here so i'm just going to toss this Exception");
            throw new InvalidConfigurationException("getAllAvailableMetrics, getAllEvents, and getAllHealthRuleViolations are false, but the application has no metrics configured, not sure what to do here so i'm just going to toss this Exception");
        }
        //TODO validate the application exists in the controller, we have access to it, and populate metrics if get all availablle metric is set
        TreeNode[] folders = controller.getApplicationMetricFolders(this, "");
        logger.debug("Found %d folders we can go into", (folders == null ? "0" : folders.length));
        if( getAllAvailableMetrics ) {
            findMetrics( controller, folders, "");
            this.metrics = metricsToAdd.toArray( new ApplicationMetric[0] );
        }
    }

    private ArrayList<ApplicationMetric> metricsToAdd = new ArrayList<>();
    private void findMetrics(Controller controller, TreeNode[] somethings, String path) {
        if( somethings == null || somethings.length == 0 ) return;
            for( TreeNode something : somethings ) {
                if( something.isFolder() ) {
                    if( !"".equals(path) ) path += "|";
                    path+=something.name;
                    findMetrics( controller, controller.getApplicationMetricFolders(this, path), path);
                } else {
                    logger.debug("Adding metric: %s|%s",path,something.name);
                    metricsToAdd.add( new ApplicationMetric(defaultDisableDataRollup, path+"|"+something.name));
                }
            }
    }
}
