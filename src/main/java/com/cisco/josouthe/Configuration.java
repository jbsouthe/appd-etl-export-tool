package com.cisco.josouthe;

import com.cisco.josouthe.data.*;
import com.cisco.josouthe.data.analytic.Search;
import com.cisco.josouthe.data.metric.ApplicationMetric;
import com.cisco.josouthe.database.Database;
import com.cisco.josouthe.database.csv.CSVDatabase;
import com.cisco.josouthe.database.oracle.OracleDatabase;
import com.cisco.josouthe.exceptions.InvalidConfigurationException;
import com.cisco.josouthe.util.Utility;
import org.apache.commons.digester3.Digester;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.MalformedURLException;
import java.util.*;

public class Configuration {
    private static final Logger logger = LogManager.getFormatterLogger();
    private Properties properties = null;
    private Database database = null;
    private HashMap<String, Controller> controllerMap = null;
    private ArrayList<Application> applications = new ArrayList<>();;
    private ArrayList<ApplicationMetric> metrics = new ArrayList<>();;
    private ArrayList<Analytics> analytics = new ArrayList<>();
    private ArrayList<Search> searches = new ArrayList<>();
    private boolean definedScheduler = false;
    private boolean definedController = false;
    private boolean definedApplication = false;
    private boolean definedAnalytics = false;
    private boolean running = true;

    public boolean isRunning() { return this.running; }
    public void setRunning( boolean b ) { this.running=b; }

    public String getProperty( String key ) {
        return getProperty(key, (String)null);
    }
    public String getProperty( String key , String defaultValue) {
        return this.properties.getProperty(key, defaultValue);
    }
    public Boolean getProperty( String key, Boolean defaultBoolean) {
        return Boolean.parseBoolean( getProperty(key, defaultBoolean.toString()));
    }
    public Long getProperty( String key, Long defaultLong ) {
        return Long.parseLong( getProperty(key, defaultLong.toString()));
    }
    public Integer getProperty( String key, Integer defaultInteger ) {
        return Integer.parseInt( getProperty(key, defaultInteger.toString()));
    }

    public Database getDatabase() { return database; }
    public Controller getController( String hostname ) { return controllerMap.get(hostname); }
    public Controller[] getControllerList() { return controllerMap.values().toArray(new Controller[0]); }
    public Analytics[] getAnalyticsList() { return analytics.toArray( new Analytics[0]); }

    public Configuration( String configFileName) throws Exception {
        logger.info("Processing Config File: %s", configFileName);
        this.properties = new Properties();
        this.controllerMap = new HashMap<>();
        Digester digester = new Digester();
        digester.push(this);
        int paramCounter=0;
        //scheduler config section default enabled with 10 minute run intervals
        digester.addCallMethod("ETLTool/Scheduler", "setSchedulerProperties", 6 );
        digester.addCallParam("ETLTool/Scheduler", 0 , "enabled");
        digester.addCallParam("ETLTool/Scheduler/PollIntervalMinutes", 1 );
        digester.addCallParam("ETLTool/Scheduler/FirstRunHistoricNumberOfHours", 2 );
        digester.addCallParam("ETLTool/Scheduler/ControllerThreads", 3 );
        digester.addCallParam("ETLTool/Scheduler/DatabaseThreads", 4 );
        digester.addCallParam("ETLTool/Scheduler/ConfigurationRefreshEveryHours", 5 );

        //database configuration section
        digester.addCallMethod("ETLTool/TargetDB", "setTargetDBProperties", 6);
        digester.addCallParam("ETLTool/TargetDB/ConnectionString", 0);
        digester.addCallParam("ETLTool/TargetDB/User", 1);
        digester.addCallParam("ETLTool/TargetDB/Password", 2);
        digester.addCallParam("ETLTool/TargetDB/DefaultMetricTable", 3);
        digester.addCallParam("ETLTool/TargetDB/ControlTable", 4);
        digester.addCallParam("ETLTool/TargetDB/DefaultEventTable", 5);


        //controller section, which centralizes authentication config
        digester.addCallMethod("ETLTool/Controller", "addController", 4);
        digester.addCallParam("ETLTool/Controller/URL", 0);
        digester.addCallParam("ETLTool/Controller/ClientID", 1);
        digester.addCallParam("ETLTool/Controller/ClientSecret", 2);
        digester.addCallParam("ETLTool/Controller", 3, "getAllAnalyticsSearches");

        //application config, within a controller
        digester.addCallMethod("ETLTool/Controller/Application", "addApplication", 10);
        digester.addCallParam("ETLTool/Controller/Application", 0, "getAllAvailableMetrics");
        digester.addCallParam("ETLTool/Controller/Application/Name", 1);
        digester.addCallParam("ETLTool/Controller/Application/Defaults/DisableDataRollup", 2);
        digester.addCallParam("ETLTool/Controller/Application/Defaults/MetricTable", 3);
        digester.addCallParam("ETLTool/Controller/Application/Defaults/EventTable", 4);
        digester.addCallParam("ETLTool/Controller/Application", 5, "getAllEvents");
        digester.addCallParam("ETLTool/Controller/Application", 6, "getAllHealthRuleViolations");
        digester.addCallParam("ETLTool/Controller/Application/Events/Include", 7);
        digester.addCallParam("ETLTool/Controller/Application/Events/Exclude", 8);
        digester.addCallParam("ETLTool/Controller/Application/Events/Severities", 9);



        //metric config, within an application
        digester.addCallMethod( "ETLTool/Controller/Application/Metric", "addMetric", 4);
        digester.addCallParam("ETLTool/Controller/Application/Metric", 0, "time-range-type");
        digester.addCallParam("ETLTool/Controller/Application/Metric", 1, "duration-in-mins");
        digester.addCallParam("ETLTool/Controller/Application/Metric", 2, "disable-data-rollup");
        digester.addCallParam("ETLTool/Controller/Application/Metric", 3);

        paramCounter=0;
        digester.addCallMethod("ETLTool/Analytics", "addAnalytics", 4);
        digester.addCallParam("ETLTool/Analytics/URL", paramCounter++);
        digester.addCallParam("ETLTool/Analytics/GlobalAccountName", paramCounter++);
        digester.addCallParam("ETLTool/Analytics/APIKey", paramCounter++);
        digester.addCallParam("ETLTool/Analytics/TableNamePrefix", paramCounter++);

        paramCounter=0;
        digester.addCallMethod("ETLTool/Analytics/Search", "addAnalyticsSearch", 4);
        digester.addCallParam("ETLTool/Analytics/Search", paramCounter++, "name");
        digester.addCallParam("ETLTool/Analytics/Search", paramCounter++);
        digester.addCallParam("ETLTool/Analytics/Search", paramCounter++, "limit");
        digester.addCallParam("ETLTool/Analytics/Search", paramCounter++, "visualization");

        digester.parse( new File(configFileName) );
        if( ! definedScheduler ) {
            logger.warn("No scheduler defined in the config file, we are going to configure a basic one run scheduler for you");
            setSchedulerProperties("false","", "1", "10", "50", "12");
        }

        logger.info("Validating Configured Settings");
        for( Controller controller : getControllerList() ) {
            controller.setControlTable( database.getControlTable() );
            definedController=true;
            logger.info("%s Authentication: %s",controller.hostname,controller.getBearerToken());
            for( Application application : controller.applications ){
                try {
                    application.validateConfiguration(controller);
                    definedApplication=true;
                    logger.info("%s %s is valid",controller.hostname,application.name);
                } catch (InvalidConfigurationException e) {
                    logger.info("%s %s is invalid, reason: ",controller.hostname,application.name,e);
                }
            }
        }
        for( Analytics analytic : analytics ) {
            analytic.setControlTable( database.getControlTable() );
        }
        if( (! definedController || ! definedApplication ) && ! definedAnalytics) {
            logger.warn("Config file doesn't have a controller or application and no analytics collection configured? not one? we can't do much here");
            throw new InvalidConfigurationException("Config file doesn't have a controller or application configured? not one? we can't do much here");
        }
        if(database != null && database.isDatabaseAvailable()) {
            logger.info("Database is available");
        } else {
            throw new InvalidConfigurationException("Database is not configured, available, or authentication information is incorrect. Something is wrong, giving up on this buddy");
        }
    }

    public void addAnalyticsSearch( String name, String query, String limit, String visualization ) throws InvalidConfigurationException {
        if( name == null || query == null ) {
            logger.warn("No valid minimum config parameters for Analytics Search! The name is used for the destination table and the query is the search");
            throw new InvalidConfigurationException("No valid minimum config parameters for Analytics Search! The name is used for the destination table and the query is the search");
        }
        if( visualization == null ) visualization="TABLE";
        if( limit == null ) limit="20000";
        searches.add(new Search(name, query, Integer.parseInt(limit), visualization));
        logger.info("Added Search %s: '%s' to list for collection",name, query);
    }

    public void addAnalytics( String urlString, String accountName, String apiKey, String tableNamePrefix ) throws InvalidConfigurationException {
        if( urlString == null || accountName == null || apiKey == null ) {
            logger.warn("No valid minimum config paramters for Analytics, must have a url, global account name, and apikey, try again!");
            throw new InvalidConfigurationException("No valid minimum config paramters for Analytics, must have a url, global account name, and apikey, try again!");
        }
        //if( this.searches.size() == 0 ) throw new InvalidConfigurationException("We can't add an Analytics section without any Searches!");
        try {
            Analytics analytic = new Analytics( urlString, accountName, apiKey, tableNamePrefix, (ArrayList<Search>) this.searches.clone());
            this.searches = new ArrayList<>();
            this.analytics.add(analytic);
            this.definedAnalytics=true;
        } catch (MalformedURLException exception) {
            exception.printStackTrace();
        }
    }

    public void addMetric( String timeRangeType, String durationInMins, String disableDataRollup, String name ) throws InvalidConfigurationException {
        if( name == null ) {
            logger.warn("No valid minimum config parameters for Metric! Ensure Metric is named with fully qualified metric path name");
            throw new InvalidConfigurationException("No valid minimum config parameters for Metric! Ensure Metric is named with fully qualified metric path name");
        }
        ApplicationMetric metric = new ApplicationMetric( disableDataRollup, name);
        metrics.add(metric);
        logger.info("Added metric to list for collection: %s", name);
    }

    public void addApplication( String getAllAvailableMetrics, String name , String defaultDisableAutoRollup,
                                String metricTable, String eventTable,
                                String getAllEvents, String getAllHealthRuleViolations,
                                String includeEventList, String excludeEventList, String eventSeverities
                                ) throws InvalidConfigurationException {
        if( name == null ) {
            logger.warn("No valid minimum config parameters for Application! Ensure Name is configured");
            throw new InvalidConfigurationException("No valid minimum config parameters for Application! Ensure Name is configured");
        }
        if( metricTable != null && database.isValidDatabaseTableName(metricTable) ) logger.debug("Application %s Metric Table set to: %s", name, metricTable);
        if( eventTable != null && database.isValidDatabaseTableName(eventTable) ) logger.debug("Application %s Event Table set to: %s", name, eventTable);
        Application application = new Application( getAllAvailableMetrics, name, defaultDisableAutoRollup, metricTable, eventTable, getAllEvents, getAllHealthRuleViolations, metrics.toArray( new ApplicationMetric[0] ));
        application.setEventTypeList( getEventListForApplication(includeEventList, excludeEventList));
        if( eventSeverities != null ) application.eventSeverities = eventSeverities;
        applications.add(application);
        metrics = new ArrayList<>();
    }

    public void addController( String urlString, String clientID, String clientSecret, String getAllAnalyticsSearches) throws InvalidConfigurationException {
        if( urlString == null || clientID == null || clientSecret == null ) {
            logger.warn("No valid minimum config parameters for Controller! Ensure URL, ClientID, and ClientSecret are configured");
            throw new InvalidConfigurationException("No valid minimum config parameters for Controller! Ensure URL, ClientID, and ClientSecret are configured");
        }
        if( applications == null || applications.isEmpty() ) {
            logger.warn("Controller configured, but no applications configured, please add at least one application");
            throw new InvalidConfigurationException("Controller configured, but no applications configured, please add at least one application");
        }
        boolean getAllAnalyticsSearchesFlag=false;
        if( "true".equals(getAllAnalyticsSearches) )
            getAllAnalyticsSearchesFlag=true;
        try{
            Controller controller = new Controller(urlString, clientID, clientSecret, applications.toArray( new Application[0] ), getAllAnalyticsSearchesFlag);
            applications = new ArrayList<>();;
            controllerMap.put( controller.hostname, controller);
            logger.info("Added Controller  to config for host: %s url: %s", controller.hostname, urlString);
            if( controller.isGetAllAnalyticsSearchesFlag() ) {
                for( Search search : controller.getAllSavedSearchesFromController() )
                    addAnalyticsSearch(search.getName(), search.getQuery(), null, search.visualization);
            }
        } catch (MalformedURLException exception) {
            logger.error("Could not create controller from config file because of a bad URL: %s Exception: %s", urlString, exception.getMessage());
        }
    }

    public void setSchedulerProperties( String enabledFlag, String pollIntervalMinutes, String firstRunHistoricNumberOfHours, String numberOfControllerThreads, String numberOfDatabaseThreads, String numberConfigRefreshHours ) {
        if( "false".equalsIgnoreCase(enabledFlag) ) {
            logger.info("MainControlScheduler is disabled, running only once");
            properties.setProperty("scheduler-enabled", "false");
            return;
        } else {
            properties.setProperty("scheduler-enabled", "true");
            this.definedScheduler = true;
        }
        if( "".equals(pollIntervalMinutes) || pollIntervalMinutes == null ) {
            pollIntervalMinutes = "10";
        }
        logger.info("Setting poll interval to every %s minutes", pollIntervalMinutes);
        this.properties.setProperty("scheduler-pollIntervalMinutes", pollIntervalMinutes);
        if( "".equals(firstRunHistoricNumberOfHours) || firstRunHistoricNumberOfHours == null ) {
            firstRunHistoricNumberOfHours = "48";
        }
        logger.info("Setting first run historic data to pull to %s hours", firstRunHistoricNumberOfHours);
        this.properties.setProperty("scheduler-FirstRunHistoricNumberOfHours", firstRunHistoricNumberOfHours);
        if( "".equals(numberOfControllerThreads) || numberOfControllerThreads == null ) {
            numberOfControllerThreads="10";
        }
        logger.info("Setting Number of Controller Communication Threads to %s", numberOfControllerThreads);
        this.properties.setProperty("scheduler-NumberOfControllerThreads", numberOfControllerThreads);
        if( "".equals(numberOfDatabaseThreads) || numberOfDatabaseThreads == null ) {
            numberOfDatabaseThreads="50";
        }
        logger.info("Setting Number of Database Communication Threads to %s", numberOfDatabaseThreads);
        this.properties.setProperty("scheduler-NumberOfDatabaseThreads", numberOfDatabaseThreads);
        if( "".equals(numberConfigRefreshHours) || numberConfigRefreshHours == null ) {
            numberConfigRefreshHours="12";
        }
        logger.info("Setting Number of Hours to refresh controller application metric list to %s", numberConfigRefreshHours);
        this.properties.setProperty("scheduler-ConfigRefreshHours", numberConfigRefreshHours);
    }

    public void setTargetDBProperties( String connectionString, String user, String password, String metricTable, String controlTable, String eventTable ) throws InvalidConfigurationException {
        if( connectionString == null ) {
            logger.warn("No valid minimum config parameters for ETL Database! Ensure Connection String is configured");
            throw new InvalidConfigurationException("No valid minimum config parameters for ETL Database! Ensure Connection String is configured");
        }
        logger.debug("Setting Target DB: %s", connectionString);
        properties.setProperty("database-vendor", Utility.parseDatabaseVendor(connectionString));
        if( metricTable == null ) metricTable = "AppDynamics_MetricTable";
        if( controlTable == null ) controlTable = "AppDynamics_SchedulerControl";
        if( eventTable == null ) eventTable = "AppDynamics_EventTable";
        switch( Utility.parseDatabaseVendor(connectionString).toLowerCase() ) {
            case "oracle": {
                this.database = new OracleDatabase(this, connectionString, user, password, metricTable, controlTable, eventTable, getProperty("scheduler-FirstRunHistoricNumberOfHours", 48L));
                break;
            }
            case "csv": {
                this.database = new CSVDatabase(this, connectionString, metricTable, controlTable, eventTable, getProperty("scheduler-FirstRunHistoricNumberOfHours", 48L));
                break;
            }
            default: {
                throw new InvalidConfigurationException("Unknown Database Vendor: '"+ Utility.parseDatabaseVendor(connectionString)+"' this is a typo or an unsupported database: "+ connectionString);
            }
        }
    }



    private List<String> _eventList = null;
    private String getEventListForApplication( String includeListString, String excludeListString) throws InvalidConfigurationException {
        if(_eventList == null) loadEventList();
        List<String> includeList=null, excludeList=null;
        if( includeListString != null ) {
            includeList = new ArrayList<>();
            for( String event : includeListString.toUpperCase().split(","))
                includeList.add(event);
        }
        if( excludeListString != null) {
            excludeList = new ArrayList<>();
            for( String event : excludeListString.toUpperCase().split(","))
                excludeList.add(event);
        }
        StringBuilder eventListStringBuilder = new StringBuilder();
        for( String event : _eventList ) {
            if( includeList != null && !includeList.contains(event) ) continue;
            if( excludeList != null && excludeList.contains(event) ) continue;
            if(eventListStringBuilder.length() > 0 ) eventListStringBuilder.append(",");
            eventListStringBuilder.append(event);
        }
        logger.debug("Returning event list: %s",eventListStringBuilder);
        return eventListStringBuilder.toString();
    }

    private void loadEventList() throws InvalidConfigurationException {
        String filename = "eventList.txt";
        logger.debug("Loading file: %s",filename);
        try (BufferedReader reader = new BufferedReader( new InputStreamReader(getClass().getClassLoader().getResourceAsStream(filename)))) {
            _eventList = new ArrayList<>();
            String word;
            while ( (word = reader.readLine()) != null )
                _eventList.add(word.toUpperCase());
        } catch (IOException e) {
            logger.warn("Error reading list of forbidden table names from internal file %s, this database may not be supported: %s Exception: %s", filename, getProperty("database-vendor","UNKNOWN DATABASE!"), e.getMessage());
            throw new InvalidConfigurationException(String.format("Error reading list of forbidden table names from internal file %s, this database may not be supported: %s", filename, getProperty("database-vendor","UNKNOWN DATABASE!")));
        }
    }
}
