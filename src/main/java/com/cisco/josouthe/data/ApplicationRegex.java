package com.cisco.josouthe.data;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ApplicationRegex {
    private static final Logger logger = LogManager.getFormatterLogger();
    private String regexString,getAllAvailableMetrics,defaultDisableAutoRollup,metricTable,eventTable,baselineTable,getAllEvents,getAllHealthRuleViolations;
    private ArrayList<String> metrics;
    private Pattern pattern;
    public ApplicationRegex(String regexString, String getAllAvailableMetrics, String defaultDisableAutoRollup, String metricTable,
            String eventTable, String baselineTable, String getAllEvents, String getAllHealthRuleViolations, ArrayList<String> metrics) {
        logger.debug("Added new Application Regex for pattern: '%s'",regexString);
        this.regexString=regexString;
        this.getAllAvailableMetrics=getAllAvailableMetrics;
        this.defaultDisableAutoRollup=defaultDisableAutoRollup;
        this.metricTable=metricTable;
        this.eventTable=eventTable;
        this.baselineTable=baselineTable;
        this.getAllEvents=getAllEvents;
        this.getAllHealthRuleViolations=getAllHealthRuleViolations;
        this.metrics=metrics;
        this.pattern = Pattern.compile(regexString);
    }

    public Application getApplicationIfMatches( String name ) {
        if( "".equals(name) || name == null ) return null;
        Matcher matcher = pattern.matcher(name);
        if(matcher.matches()) {
            logger.debug("Application name '%s' matches pattern '%s', returning application object to Controller", name, this.regexString);
            return new Application(getAllAvailableMetrics, name, defaultDisableAutoRollup, metricTable, eventTable, baselineTable,
                    getAllEvents, getAllHealthRuleViolations, metrics);
        }
        return null;
    }
}
