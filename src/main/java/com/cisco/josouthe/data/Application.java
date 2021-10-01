package com.cisco.josouthe.data;

import com.cisco.josouthe.exceptions.InvalidConfigurationException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;

public class Application {
    private static final Logger logger = LogManager.getFormatterLogger();
    public boolean getAllAvailableMetrics = true;
    public String name;
    public String defaultTimeRangeType = "BEFORE_NOW";
    public String defaultDurationInMinutes = "60";
    public ApplicationMetric[] metrics = null;

    public Application(String getAllAvailableMetrics, String name, String defaultTimeRangeType, String defaultDurationInMinutes, ApplicationMetric[] metrics) {
        if( getAllAvailableMetrics != null ) this.getAllAvailableMetrics= Boolean.parseBoolean(getAllAvailableMetrics);
        this.name = name;
        if( defaultTimeRangeType != null ) this.defaultTimeRangeType = defaultTimeRangeType;
        if( defaultDurationInMinutes != null ) this.defaultDurationInMinutes = defaultDurationInMinutes;
        this.metrics = metrics;
    }

    public void validateConfiguration(Controller controller) throws InvalidConfigurationException {
        if( !getAllAvailableMetrics && (metrics == null || metrics.length == 0)) {
            logger.warn("getAllAvailableMetrics is false, but the application has no metrics configured, not sure what to do here so i'm just going to toss this Exception");
            throw new InvalidConfigurationException("getAllAvailableMetrics is false, but the application has no metrics configured, not sure what to do here so i'm just going to toss this Exception");
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
                    metricsToAdd.add( new ApplicationMetric(defaultTimeRangeType, defaultDurationInMinutes, path+"|"+something.name));
                }
            }
    }
}
