package com.cisco.josouthe.config;

import com.cisco.josouthe.data.Analytics;
import com.cisco.josouthe.data.Application;
import com.cisco.josouthe.data.ApplicationRegex;
import com.cisco.josouthe.data.Controller;
import com.cisco.josouthe.data.analytic.Search;
import com.cisco.josouthe.database.Database;
import com.cisco.josouthe.database.csv.CSVDatabase;
import com.cisco.josouthe.database.microsoft.MicrosoftDatabase;
import com.cisco.josouthe.database.mysql.MySQLDatabase;
import com.cisco.josouthe.database.oracle.OracleDatabase;
import com.cisco.josouthe.database.postgresql.PGSQLDatabase;
import com.cisco.josouthe.exceptions.InvalidConfigurationException;
import com.cisco.josouthe.util.Utility;
import org.apache.commons.digester3.Digester;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.builder.api.*;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

public class Configuration {
    private static final Logger logger = LogManager.getFormatterLogger();
    private Properties properties = null;
    private Database database = null;
    private HashMap<String, Controller> controllerMap = null;
    private ArrayList<Application> applications = new ArrayList<>();;
    private ArrayList<String> metrics = new ArrayList<>();;
    private ArrayList<Analytics> analytics = new ArrayList<>();
    private ArrayList<Search> searches = new ArrayList<>();
    private boolean definedScheduler = false;
    private boolean definedController = false;
    private boolean definedApplication = false;
    private boolean definedAnalytics = false;
    private boolean running = true;
    private List<ApplicationRegex> applicationRegexList = new ArrayList<>();
    private List<LoggerConfigItem> loggerConfigItemList = new ArrayList<>();
    private static final String LOGGER_NAME = "fileLogger";

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

    public Configuration( String configFileName ) throws Exception { this( configFileName, false); }
    public Configuration( String configFileName, boolean printInfoLogs ) throws Exception {
        File file = new File(configFileName);
        if (!file.exists()) throw new IOException("Config File not found!");
        if (!file.canRead()) throw new IOException("Config File not readable!");
        if (!file.isFile()) throw new IOException("Config File not a file?!?!");
        if( !printInfoLogs ) Configurator.setAllLevels(logger.getName(), Level.WARN);
        logger.info("Processing Config File: %s", configFileName);
        this.properties = new Properties();
        this.controllerMap = new HashMap<>();
        Digester digester = new Digester();
        digester.push(this);
        int paramCounter = 0;

        //scheduler config section default enabled with 10 minute run intervals
        digester.addCallMethod("ETLTool/Scheduler", "setSchedulerProperties", 8);
        digester.addCallParam("ETLTool/Scheduler", paramCounter++, "enabled");
        digester.addCallParam("ETLTool/Scheduler/PollIntervalMinutes", paramCounter++);
        digester.addCallParam("ETLTool/Scheduler/FirstRunHistoricNumberOfHours", paramCounter++); //leaving this for a little while, will aim to delete by August 2023
        digester.addCallParam("ETLTool/Scheduler/ControllerThreads", paramCounter++);
        digester.addCallParam("ETLTool/Scheduler/DatabaseThreads", paramCounter++);
        digester.addCallParam("ETLTool/Scheduler/ConfigurationRefreshEveryHours", paramCounter++);
        digester.addCallParam("ETLTool/Scheduler/FirstRunHistoricNumberOfDays", paramCounter++);
        digester.addCallParam("ETLTool/Scheduler/MaxNumberOfDaysToQueryAtATime", paramCounter++);

        //Log4j Configuration
        paramCounter = 0;
        digester.addCallMethod("ETLTool/Logging", "setLoggingConfiguration", 6);
        digester.addCallParam("ETLTool/Logging/Level", paramCounter++);
        digester.addCallParam("ETLTool/Logging/FileName", paramCounter++);
        digester.addCallParam("ETLTool/Logging/FilePattern", paramCounter++);
        digester.addCallParam("ETLTool/Logging/FileCronPolicy", paramCounter++);
        digester.addCallParam("ETLTool/Logging/FileSizePolicy", paramCounter++);
        digester.addCallParam("ETLTool/Logging/Pattern", paramCounter++);

        paramCounter = 0;
        digester.addCallMethod("ETLTool/Logging/Logger", "addLoggingLogger", 3);
        digester.addCallParam("ETLTool/Logging/Logger/Name", paramCounter++);
        digester.addCallParam("ETLTool/Logging/Logger/Level", paramCounter++);
        digester.addCallParam("ETLTool/Logging/Logger/Additivity", paramCounter++);

        //database configuration section
        paramCounter = 0;
        digester.addCallMethod("ETLTool/TargetDB", "setTargetDBProperties", 8);
        digester.addCallParam("ETLTool/TargetDB/ConnectionString", paramCounter++);
        digester.addCallParam("ETLTool/TargetDB/User", paramCounter++);
        digester.addCallParam("ETLTool/TargetDB/Password", paramCounter++);
        digester.addCallParam("ETLTool/TargetDB/DefaultMetricTable", paramCounter++);
        digester.addCallParam("ETLTool/TargetDB/ControlTable", paramCounter++);
        digester.addCallParam("ETLTool/TargetDB/DefaultEventTable", paramCounter++);
        digester.addCallParam("ETLTool/TargetDB/DefaultBaselineTable", paramCounter++);
        digester.addCallParam("ETLTool/TargetDB/MaximumColumnNameLength", paramCounter++);

        //controller section, which centralizes authentication config
        paramCounter = 0;
        digester.addCallMethod("ETLTool/Controller", "addController", 5);
        digester.addCallParam("ETLTool/Controller/URL", paramCounter++);
        digester.addCallParam("ETLTool/Controller/ClientID", paramCounter++);
        digester.addCallParam("ETLTool/Controller/ClientSecret", paramCounter++);
        digester.addCallParam("ETLTool/Controller", paramCounter++, "getAllAnalyticsSearches");
        digester.addCallParam("ETLTool/Controller/AdjustEndTimeMinutes", paramCounter++);

        //application config, within a controller
        digester.addCallMethod("ETLTool/Controller/Application", "addApplication", 12);
        digester.addCallParam("ETLTool/Controller/Application", 0, "getAllAvailableMetrics");
        digester.addCallParam("ETLTool/Controller/Application/Name", 1);
        digester.addCallParam("ETLTool/Controller/Application/Defaults/DisableDataRollup", 2);
        digester.addCallParam("ETLTool/Controller/Application/Defaults/MetricTable", 3);
        digester.addCallParam("ETLTool/Controller/Application/Defaults/EventTable", 4);
        digester.addCallParam("ETLTool/Controller/Application/Defaults/BaselineTable", 5);
        digester.addCallParam("ETLTool/Controller/Application", 6, "getAllEvents");
        digester.addCallParam("ETLTool/Controller/Application", 7, "getAllHealthRuleViolations");
        digester.addCallParam("ETLTool/Controller/Application/Events/Include", 8);
        digester.addCallParam("ETLTool/Controller/Application/Events/Exclude", 9);
        digester.addCallParam("ETLTool/Controller/Application/Events/Severities", 10);
        digester.addCallParam("ETLTool/Controller/Application/Name", 11, "regex");

        //metric config, within an application
        digester.addCallMethod("ETLTool/Controller/Application/Metric", "addMetric", 3);
        digester.addCallParam("ETLTool/Controller/Application/Metric", 0, "time-range-type");
        digester.addCallParam("ETLTool/Controller/Application/Metric", 1, "duration-in-mins");
        digester.addCallParam("ETLTool/Controller/Application/Metric", 2);

        paramCounter = 0;
        digester.addCallMethod("ETLTool/Analytics", "addAnalytics", 6);
        digester.addCallParam("ETLTool/Analytics/URL", paramCounter++);
        digester.addCallParam("ETLTool/Analytics/GlobalAccountName", paramCounter++);
        digester.addCallParam("ETLTool/Analytics/APIKey", paramCounter++);
        digester.addCallParam("ETLTool/Analytics/TableNamePrefix", paramCounter++);
        digester.addCallParam("ETLTool/Analytics/LinkToControllerHostname", paramCounter++);
        digester.addCallParam("ETLTool/Analytics/AdjustEndTimeMinutes", paramCounter++);

        paramCounter = 0;
        digester.addCallMethod("ETLTool/Analytics/Search", "addAnalyticsSearch", 4);
        digester.addCallParam("ETLTool/Analytics/Search", paramCounter++, "name");
        digester.addCallParam("ETLTool/Analytics/Search", paramCounter++);
        digester.addCallParam("ETLTool/Analytics/Search", paramCounter++, "limit");
        digester.addCallParam("ETLTool/Analytics/Search", paramCounter++, "visualization");

        setSchedulerProperties("false", "", "", "10", "50", "12", "2", "14", false);
        digester.parse(new InputStreamReader(new FileInputStream(configFileName), StandardCharsets.UTF_8));


        logger.info("Validating Configured Settings");
        for (Controller controller : getControllerList()) {
            controller.setControlTable(database.getControlTable());
            definedController = true;
            logger.info("%s Authentication: %s", controller.hostname, controller.getBearerToken());
            for (Application application : controller.applications) {
                try {
                    application.validateConfiguration(controller);
                    definedApplication = true;
                    logger.info("%s %s is valid", controller.hostname, application.name);
                } catch (InvalidConfigurationException e) {
                    logger.info("%s %s is invalid, reason: ", controller.hostname, application.name, e);
                }
            }
        }
        for (Analytics analytic : analytics) {
            analytic.setControlTable(database.getControlTable());
        }
        if ((!definedController || !definedApplication) && !definedAnalytics) {
            logger.warn("Config file doesn't have a controller or application and no analytics collection configured? not one? we can't do much here");
            throw new InvalidConfigurationException("Config file doesn't have a controller or application configured? not one? we can't do much here");
        }

        if(database != null && database.isDatabaseAvailable()) {
            logger.info("Database is available");
        } else {
            throw new InvalidConfigurationException("Database is not configured, available, or authentication information is incorrect. Something is wrong, giving up on this buddy");
        }
    }

    public void addLoggingLogger( String name, String level, String additivity ) throws InvalidConfigurationException {
        this.loggerConfigItemList.add(new LoggerConfigItem(name, level, additivity) );
    }

    public void setLoggingConfiguration( String level, String fileName, String filePattern, String fileCronPolicy, String fileSizePolicy, String pattern ) throws InvalidConfigurationException {
        if( level == null ) level="INFO";
        if( fileName == null ) fileName="etl-export.log";
        if( filePattern == null ) filePattern="etl-export-%d{MM-dd-yy}.log.gz";
        if( pattern == null ) pattern="%d{HH:mm:ss.SSS} [%t] %-5level %logger{36} - %msg%n";

        ConfigurationBuilder<BuiltConfiguration> builder = ConfigurationBuilderFactory.newConfigurationBuilder();

        LayoutComponentBuilder patternLayout = builder.newLayout("PatternLayout");
        patternLayout.addAttribute("pattern", pattern);

        Level rootLevel = Utility.getLevel(level);
        if( rootLevel == null ) throw new InvalidConfigurationException(String.format("<ETLTool><Logging><Level> of '%s' is not valid, please set to one of TRACE|DEBUG|INFO|WARN|ERROR", level));
        RootLoggerComponentBuilder rootLoggerComponentBuilder = builder.newRootLogger( rootLevel );
        builder.add(rootLoggerComponentBuilder);

        AppenderComponentBuilder console = builder.newAppender("stdout", "Console");
        console.add(patternLayout);
        builder.add(console);

        AppenderComponentBuilder rollingFile = builder.newAppender(LOGGER_NAME, "RollingFile");
        rollingFile.addAttribute("fileName", fileName);
        rollingFile.addAttribute("filePattern", filePattern);
        rollingFile.add(patternLayout);
        //optional trigger policies
        if( Utility.isStringSet(fileCronPolicy) || Utility.isStringSet(fileSizePolicy) ) {
            ComponentBuilder triggeringPolicies = builder.newComponent("Policies");
            if( Utility.isStringSet(fileCronPolicy)) triggeringPolicies.addComponent(builder.newComponent("CronTriggeringPolicy")
                    .addAttribute("schedule", fileCronPolicy));
            if( Utility.isStringSet(fileSizePolicy)) triggeringPolicies.addComponent(builder.newComponent("SizeBasedTriggeringPolicy")
                    .addAttribute("size", fileSizePolicy));
            rollingFile.addComponent(triggeringPolicies);
        }
        builder.add(rollingFile);

        for( LoggerConfigItem loggerConfigItem : loggerConfigItemList ) {
            LoggerComponentBuilder loggerComponentBuilder = builder.newAsyncLogger( loggerConfigItem.name, loggerConfigItem.level);
            loggerComponentBuilder.add(builder.newAppenderRef(LOGGER_NAME));
            loggerComponentBuilder.addAttribute("additivity", loggerConfigItem.additivity);
            builder.add(loggerComponentBuilder);
        }

        Configurator.initialize(builder.build());
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

    public void addAnalytics( String urlString, String accountName, String apiKey, String tableNamePrefix, String linkedControllerHostname, String minutesToAdjustEndTimestampByString ) throws InvalidConfigurationException {
        if( urlString == null || accountName == null || apiKey == null ) {
            logger.warn("No valid minimum config paramters for Analytics, must have a url, global account name, and apikey, try again!");
            throw new InvalidConfigurationException("No valid minimum config paramters for Analytics, must have a url, global account name, and apikey, try again!");
        }
        Controller controller = getController( linkedControllerHostname);
        if( controller != null && controller.isGetAllAnalyticsSearchesFlag() ) {
            for( Search search : controller.getAllSavedSearchesFromController() )
                addAnalyticsSearch(search.getName(), search.getQuery(), null, search.visualization);
        }
        if( minutesToAdjustEndTimestampByString == null ) minutesToAdjustEndTimestampByString="5";
        int minutesToAdjustEndTimestampBy = Integer.parseInt(minutesToAdjustEndTimestampByString);
        if( minutesToAdjustEndTimestampBy < 0 ) {
            logger.warn("Configuration option <AdjustEndTimeMinutes>%d</AdjustEndTimeMinutes> is a bit strange, we subtract this from the end timestamp, we are going to swap the negative to positive and subtract that for you, maybe rethink this setting", minutesToAdjustEndTimestampBy);
            minutesToAdjustEndTimestampBy *= -1;
        }
        //if( this.searches.size() == 0 ) throw new InvalidConfigurationException("We can't add an Analytics section without any Searches!");
        try {
            Analytics analytic = new Analytics( urlString, accountName, apiKey, tableNamePrefix, getDatabase(), (ArrayList<Search>) this.searches.clone(), minutesToAdjustEndTimestampBy);
            this.searches = new ArrayList<>();
            this.analytics.add(analytic);
            this.definedAnalytics=true;
        } catch (MalformedURLException exception) {
            exception.printStackTrace();
        }
    }

    public void addMetric( String timeRangeType, String durationInMins, String name ) throws InvalidConfigurationException {
        if( name == null ) {
            logger.warn("No valid minimum config parameters for Metric! Ensure Metric is named with fully qualified metric path name");
            throw new InvalidConfigurationException("No valid minimum config parameters for Metric! Ensure Metric is named with fully qualified metric path name");
        }
        metrics.add(name);
        logger.info("Added metric to list for collection: %s", name);
    }

    public void addApplication( String getAllAvailableMetrics, String name , String defaultDisableAutoRollup,
                                String metricTable, String eventTable, String baselineTable,
                                String getAllEvents, String getAllHealthRuleViolations,
                                String includeEventList, String excludeEventList, String eventSeverities,
                                String isRegexAppNameFlag
                                ) throws InvalidConfigurationException {
        if( name == null ) {
            logger.warn("No valid minimum config parameters for Application! Ensure Name is configured");
            throw new InvalidConfigurationException("No valid minimum config parameters for Application! Ensure Name is configured");
        }
        if( metricTable != null && database.isValidDatabaseTableName(metricTable) ) logger.debug("Application %s Metric Table set to: %s", name, metricTable);
        if( eventTable != null && database.isValidDatabaseTableName(eventTable) ) logger.debug("Application %s Event Table set to: %s", name, eventTable);
        if( baselineTable != null && database.isValidDatabaseTableName(baselineTable) ) logger.debug("Application %s Baseline Table set to: %s", name, baselineTable);
        boolean isRegexAppName = false;
        if( isRegexAppNameFlag != null ) isRegexAppName = Boolean.parseBoolean(isRegexAppNameFlag);
        if( isRegexAppName ) {
            applicationRegexList.add( new ApplicationRegex( name, getAllAvailableMetrics, defaultDisableAutoRollup, metricTable, eventTable, baselineTable,
                    getAllEvents, getAllHealthRuleViolations, metrics ));
        } else {
            Application application = new Application(getAllAvailableMetrics, name, defaultDisableAutoRollup, metricTable, eventTable, baselineTable,
                    getAllEvents, getAllHealthRuleViolations, metrics);
            application.setEventTypeList(getEventListForApplication(includeEventList, excludeEventList));
            if (eventSeverities != null) application.eventSeverities = eventSeverities;
            applications.add(application);
        }
        metrics = new ArrayList<>();
    }

    public void addController( String urlString, String clientID, String clientSecret, String getAllAnalyticsSearches, String minutesToAdjustEndTimestampByString ) throws InvalidConfigurationException {
        if( urlString == null || clientID == null || clientSecret == null ) {
            logger.warn("No valid minimum config parameters for Controller! Ensure URL, ClientID, and ClientSecret are configured");
            throw new InvalidConfigurationException("No valid minimum config parameters for Controller! Ensure URL, ClientID, and ClientSecret are configured");
        }
        boolean getAllAnalyticsSearchesFlag=false;
        if( "true".equals(getAllAnalyticsSearches) )
            getAllAnalyticsSearchesFlag=true;
        if( ((applications == null || applications.isEmpty()) && (applicationRegexList == null || applicationRegexList.isEmpty())) && !getAllAnalyticsSearchesFlag ) {
            logger.warn("Controller configured, but no applications configured, please add at least one application, or set getAllAnalyticsSearches Flag to true");
            throw new InvalidConfigurationException("Controller configured, but no applications configured, please add at least one application, or set getAllAnalyticsSearches Flag to true");
        }
        String hostname = "<hostname>";
        try{
            hostname = new URL(urlString).getHost();
            if( hostname.contains(".") )
                hostname = hostname.split("\\.")[0];
        } catch (MalformedURLException exception) {
            logger.warn("Url is really bad, '%s' Error: %s",urlString, exception);
            throw new InvalidConfigurationException(String.format("Controller URL just can't be correct, let's fix this '%s'",urlString));
        }
        if( !Utility.isAPIKeyValidFormat( clientID, hostname) ) {
            String error = String.format("API Key is missing the hostname, it should probably be %s@%s instead of just %s, but I'm not going to automate config error corrections, it is just bad practice, so bailing early", clientID, hostname, clientID);
            throw new InvalidConfigurationException(error);
        }
        if( minutesToAdjustEndTimestampByString == null ) minutesToAdjustEndTimestampByString="5";
        int minutesToAdjustEndTimestampBy = Integer.parseInt(minutesToAdjustEndTimestampByString);
        if( minutesToAdjustEndTimestampBy < 0 ) {
            logger.warn("Configuration option <AdjustEndTimeMinutes>%d</AdjustEndTimeMinutes> is a bit strange, we subtract this from the end timestamp, we are going to swap the negative to positive and subtract that for you, maybe rethink this setting", minutesToAdjustEndTimestampBy);
            minutesToAdjustEndTimestampBy *= -1;
        }
        try{
            Controller controller = new Controller(urlString, clientID, clientSecret, applications.toArray( new Application[0] ), getAllAnalyticsSearchesFlag, applicationRegexList.toArray( new ApplicationRegex[0]), minutesToAdjustEndTimestampBy, this);
            applications = new ArrayList<>();
            applicationRegexList = new ArrayList<>();
            controllerMap.put( controller.hostname, controller);
            logger.info("Added Controller  to config for host: %s url: %s", controller.hostname, urlString);
        } catch (MalformedURLException exception) {
            logger.error("Could not create controller from config file because of a bad URL: %s Exception: %s", urlString, exception.getMessage());
        }
    }

    public void setSchedulerProperties( String enabledFlag, String pollIntervalMinutes, String firstRunHistoricNumberOfHours, String numberOfControllerThreads, String numberOfDatabaseThreads, String numberConfigRefreshHours, String firstRunHistoricNumberOfDays, String maxNumberOfDaysToQueryAtATime ) {
        setSchedulerProperties(enabledFlag, pollIntervalMinutes, firstRunHistoricNumberOfHours, numberOfControllerThreads, numberOfDatabaseThreads, numberConfigRefreshHours, firstRunHistoricNumberOfDays, maxNumberOfDaysToQueryAtATime, true );
    }
    public void setSchedulerProperties( String enabledFlag, String pollIntervalMinutes, String firstRunHistoricNumberOfHours, String numberOfControllerThreads, String numberOfDatabaseThreads, String numberConfigRefreshHours, String firstRunHistoricNumberOfDays, String maxNumberOfDaysToQueryAtATime, boolean printOutput ) {
        if( "false".equalsIgnoreCase(enabledFlag) ) {
            if(printOutput) logger.info("MainControlScheduler is disabled, running only once");
            properties.setProperty("scheduler-enabled", "false");
        } else {
            properties.setProperty("scheduler-enabled", "true");
            this.definedScheduler = true;
        }
        if( "".equals(pollIntervalMinutes) || pollIntervalMinutes == null ) {
            pollIntervalMinutes = "10";
        }
        if(printOutput) logger.info("Setting poll interval to every %s minutes", pollIntervalMinutes);
        this.properties.setProperty("scheduler-pollIntervalMinutes", pollIntervalMinutes);
        if( firstRunHistoricNumberOfDays != null && !"".equals(firstRunHistoricNumberOfDays) )
            firstRunHistoricNumberOfHours = String.format("%d", Integer.parseInt(firstRunHistoricNumberOfDays)*24);
        if( "".equals(firstRunHistoricNumberOfHours) || firstRunHistoricNumberOfHours == null ) {
            firstRunHistoricNumberOfHours = "48";
        }
        if(printOutput) logger.info("Setting first run historic data to pull to %s hours", firstRunHistoricNumberOfHours);
        this.properties.setProperty("scheduler-FirstRunHistoricNumberOfHours", firstRunHistoricNumberOfHours);
        if( "".equals(numberOfControllerThreads) || numberOfControllerThreads == null ) {
            numberOfControllerThreads="10";
        }
        if(printOutput) logger.info("Setting Number of Controller Communication Threads to %s", numberOfControllerThreads);
        this.properties.setProperty("scheduler-NumberOfControllerThreads", numberOfControllerThreads);
        if( "".equals(numberOfDatabaseThreads) || numberOfDatabaseThreads == null ) {
            numberOfDatabaseThreads="50";
        }
        if(printOutput) logger.info("Setting Number of Database Communication Threads to %s", numberOfDatabaseThreads);
        this.properties.setProperty("scheduler-NumberOfDatabaseThreads", numberOfDatabaseThreads);
        if( "".equals(numberConfigRefreshHours) || numberConfigRefreshHours == null ) {
            numberConfigRefreshHours="12";
        }
        if(printOutput) logger.info("Setting Number of Hours to refresh controller application metric list to %s", numberConfigRefreshHours);
        this.properties.setProperty("scheduler-ConfigRefreshHours", numberConfigRefreshHours);
        if( "".equals(maxNumberOfDaysToQueryAtATime) || maxNumberOfDaysToQueryAtATime == null ) {
            maxNumberOfDaysToQueryAtATime="14";
        }
        if(printOutput) logger.info("Setting Max Number of Days to Query at a time on the controller to %s", maxNumberOfDaysToQueryAtATime);
        this.properties.setProperty("scheduler-MaxQueryDays", maxNumberOfDaysToQueryAtATime);
    }

    public void setTargetDBProperties( String connectionString, String user, String password, String metricTable, String controlTable, String eventTable, String baselineTable, String maximumColumnNameLengthString ) throws InvalidConfigurationException {
        if( connectionString == null ) {
            logger.warn("No valid minimum config parameters for ETL Database! Ensure Connection String is configured");
            throw new InvalidConfigurationException("No valid minimum config parameters for ETL Database! Ensure Connection String is configured");
        }
        logger.debug("Setting Target DB: %s", connectionString);
        properties.setProperty("database-vendor", Utility.parseDatabaseVendor(connectionString));
        if( metricTable == null ) metricTable = "AppDynamics_MetricTable";
        if( controlTable == null ) controlTable = "AppDynamics_SchedulerControl";
        if( eventTable == null ) eventTable = "AppDynamics_EventTable";
        if( baselineTable == null ) baselineTable = "AppDynamics_BaselineTable";
        int maxColumnNameLength = 30;
        if( maximumColumnNameLengthString != null ) {
            try {
                maxColumnNameLength = Integer.parseInt(maximumColumnNameLengthString);
            } catch (NumberFormatException e) {
                throw new InvalidConfigurationException(String.format("Bad value for MaximumColumnNameLength in Database Configuration Section: %s", maximumColumnNameLengthString) );
            }
        }
        switch( Utility.parseDatabaseVendor(connectionString).toLowerCase() ) {
            case "oracle": {
                this.database = new OracleDatabase(this, connectionString, user, password, metricTable, controlTable, eventTable, baselineTable, getProperty("scheduler-FirstRunHistoricNumberOfHours", 48L), maxColumnNameLength);
                break;
            }
            case "csv": {
                this.database = new CSVDatabase(this, connectionString, metricTable, controlTable, eventTable, baselineTable, getProperty("scheduler-FirstRunHistoricNumberOfHours", 48L));
                break;
            }
            case "sqlserver": {
                this.database = new MicrosoftDatabase(this, connectionString, user, password, metricTable, controlTable, eventTable, baselineTable, getProperty("scheduler-FirstRunHistoricNumberOfHours", 48L));
                break;
            }
            case "mysql": {
                this.database = new MySQLDatabase(this, connectionString, user, password, metricTable, controlTable, eventTable, baselineTable, getProperty("scheduler-FirstRunHistoricNumberOfHours", 48L));
                break;
            }
            case "postgresql": {
                this.database = new PGSQLDatabase(this, connectionString, user, password, metricTable, controlTable, eventTable, baselineTable, getProperty("scheduler-FirstRunHistoricNumberOfHours", 48L));
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

    public boolean isTooLongATime(long naturalDurationMS) {
        return naturalDurationMS > getMaxQueryDurationInMS();
    }

    public long getMaxQueryDurationInMS() {
        return getProperty("scheduler-MaxQueryDays", 14) *24*60*60*1000;
    }
}