package com.cisco.josouthe.config;

import com.cisco.josouthe.config.jaxb.AnalyticsConfig;
import com.cisco.josouthe.config.jaxb.AnalyticsSearchConfig;
import com.cisco.josouthe.config.jaxb.ApplicationConfig;
import com.cisco.josouthe.config.jaxb.ApplicationMetricConfig;
import com.cisco.josouthe.config.jaxb.ControllerConfig;
import com.cisco.josouthe.config.jaxb.ETLTool;
import com.cisco.josouthe.config.jaxb.LoggingConfig;
import com.cisco.josouthe.config.jaxb.OptionalLoggerConfig;
import com.cisco.josouthe.config.jaxb.SchedulerConfig;
import com.cisco.josouthe.config.jaxb.TargetDBConfig;
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
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.builder.api.AppenderComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.ComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory;
import org.apache.logging.log4j.core.config.builder.api.LayoutComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.LoggerComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.RootLoggerComponentBuilder;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

public class Configuration {
    private static final Logger logger = LogManager.getFormatterLogger();
    private final ETLTool configXML;
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
        File configFile = new File(configFileName);
        if (!configFile.exists()) throw new IOException("Config File not found!");
        if (!configFile.canRead()) throw new IOException("Config File not readable!");
        if (!configFile.isFile()) throw new IOException("Config File not a file?!?!");
        if( !printInfoLogs ) Configurator.setAllLevels(logger.getName(), Level.WARN);
        logger.info("Processing Config File: %s", configFileName);
        this.properties = new Properties();
        this.controllerMap = new HashMap<>();

        try {
            // Creating JAXB Context for CMDBSync class
            JAXBContext jaxbContext = JAXBContext.newInstance(ETLTool.class);

            // Creating Unmarshaller
            Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();

            // Unmarshalling XML file to CMDBSync object
            configXML = (ETLTool) jaxbUnmarshaller.unmarshal(configFile);
        } catch (JAXBException jaxbException) {
            throw new InvalidConfigurationException(String.format("XML Format error: %s",jaxbException.toString()));
        }

        setLoggingConfiguration( configXML.getLogging() );

        setSchedulerProperties( configXML.getScheduler() );

        setTargetDBProperties( configXML.getTargetDB() );

        for(ControllerConfig controllerConfig : configXML.getControllerList() )
            addController(controllerConfig);

        for(AnalyticsConfig analyticsConfig : configXML.getAnalyticsList() )
            addAnalytics(analyticsConfig);

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

    private void setLoggingConfiguration (LoggingConfig loggingConfig) throws InvalidConfigurationException {
        setLoggingConfiguration( loggingConfig.getLevel(), loggingConfig.getFileName(), loggingConfig.getFilePattern(),
                loggingConfig.getFileCronPolicy(), loggingConfig.getFileSizePolicy(), loggingConfig.getPattern());
        for(OptionalLoggerConfig loggerConfig : loggingConfig.getLoggerList())
            addLoggingLogger( loggerConfig.getName(), loggerConfig.getLevel(), String.valueOf(loggerConfig.isAdditivity()));
    }

    public void addLoggingLogger( String name, String level, String additivity ) throws InvalidConfigurationException {
        this.loggerConfigItemList.add(new LoggerConfigItem(name, level, additivity) );
    }

    public void setLoggingConfiguration( String level, String fileName, String filePattern, String fileCronPolicy, String fileSizePolicy, String pattern ) throws InvalidConfigurationException {
        if( level == null ) level="INFO";
        if( fileName == null ) fileName="etl-export.log";
        if( filePattern == null ) filePattern="etl-export-%d{MM-dd-yy}.log.gz";
        if( pattern == null ) pattern="%d{HH:mm:ss.SSS} [%t] %-5level %logger{36} - %msg%n";
        if( !Utility.isStringSet(fileCronPolicy) && !Utility.isStringSet(fileSizePolicy) ) {
            fileCronPolicy = "0 0 0 * * ?";
        }

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

        ComponentBuilder triggeringPolicies = builder.newComponent("Policies");
        if( Utility.isStringSet(fileCronPolicy)) triggeringPolicies.addComponent(builder.newComponent("CronTriggeringPolicy")
                .addAttribute("schedule", fileCronPolicy));
        if( Utility.isStringSet(fileSizePolicy)) triggeringPolicies.addComponent(builder.newComponent("SizeBasedTriggeringPolicy")
                .addAttribute("size", fileSizePolicy));
        rollingFile.addComponent(triggeringPolicies);

        builder.add(rollingFile);

        for( LoggerConfigItem loggerConfigItem : loggerConfigItemList ) {
            LoggerComponentBuilder loggerComponentBuilder = builder.newAsyncLogger( loggerConfigItem.name, loggerConfigItem.level);
            loggerComponentBuilder.add(builder.newAppenderRef(LOGGER_NAME));
            loggerComponentBuilder.addAttribute("additivity", loggerConfigItem.additivity);
            builder.add(loggerComponentBuilder);
        }

        Configurator.initialize(builder.build());
    }


    private void addAnalytics (AnalyticsConfig analyticsConfig) throws InvalidConfigurationException {
        for(AnalyticsSearchConfig searchConfig : analyticsConfig.getSearchList()) {
            addAnalyticsSearch(searchConfig.getName(), searchConfig.getValue(), searchConfig.getLimit(), searchConfig.getVizualization());
        }
        addAnalytics(analyticsConfig.getUrl(), analyticsConfig.getGlobalAccountName(), analyticsConfig.getaPIKey(),
                analyticsConfig.getTableNamePrefix(), analyticsConfig.getLinkToControllerHostname(),
                analyticsConfig.getAdjustEndTimeMinutes());
    }

    public void addAnalyticsSearch( String name, String query, long limit, String visualization ) throws InvalidConfigurationException {
        if( name == null || query == null ) {
            logger.warn("No valid minimum config parameters for Analytics Search! The name is used for the destination table and the query is the search");
            throw new InvalidConfigurationException("No valid minimum config parameters for Analytics Search! The name is used for the destination table and the query is the search");
        }
        if( visualization == null ) visualization="TABLE";
        if( limit == 0 ) limit = 20000;
        searches.add(new Search(name, query, limit, visualization));
        logger.info("Added Search %s: '%s' to list for collection",name, query);
    }

    public void addAnalytics( String urlString, String accountName, String apiKey, String tableNamePrefix, String linkedControllerHostname, long minutesToAdjustEndTimestampBy ) throws InvalidConfigurationException {
        if( urlString == null || accountName == null || apiKey == null ) {
            logger.warn("No valid minimum config paramters for Analytics, must have a url, global account name, and apikey, try again!");
            throw new InvalidConfigurationException("No valid minimum config paramters for Analytics, must have a url, global account name, and apikey, try again!");
        }
        Controller controller = getController( linkedControllerHostname);
        if( controller != null && controller.isGetAllAnalyticsSearchesFlag() ) {
            for( Search search : controller.getAllSavedSearchesFromController() )
                addAnalyticsSearch(search.getName(), search.getQuery(), 0, search.visualization);
        }
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

    public void addMetric( String timeRangeType, Integer durationInMins, String name ) throws InvalidConfigurationException {
        if( name == null ) {
            logger.warn("No valid minimum config parameters for Metric! Ensure Metric is named with fully qualified metric path name");
            throw new InvalidConfigurationException("No valid minimum config parameters for Metric! Ensure Metric is named with fully qualified metric path name");
        }
        metrics.add(name);
        logger.info("Added metric to list for collection: %s", name);
    }

    public void addApplication( boolean getAllAvailableMetrics, String name , boolean defaultDisableAutoRollup,
                                String metricTable, String eventTable, String baselineTable,
                                boolean getAllEvents, boolean getAllHealthRuleViolations,
                                String includeEventList, String excludeEventList, String eventSeverities,
                                boolean isRegexAppName,
                                int granularityMinutes, boolean onlyGetDefaultBaseline
                                ) throws InvalidConfigurationException {
        if( name == null ) {
            logger.warn("No valid minimum config parameters for Application! Ensure Name is configured");
            throw new InvalidConfigurationException("No valid minimum config parameters for Application! Ensure Name is configured");
        }
        if( metricTable != null && database.isValidDatabaseTableName(metricTable) ) logger.debug("Application %s Metric Table set to: %s", name, metricTable);
        if( eventTable != null && database.isValidDatabaseTableName(eventTable) ) logger.debug("Application %s Event Table set to: %s", name, eventTable);
        if( baselineTable != null && database.isValidDatabaseTableName(baselineTable) ) logger.debug("Application %s Baseline Table set to: %s", name, baselineTable);
        logger.debug("Application %s Granularity Minutes set to %s", name, granularityMinutes);
        if( isRegexAppName ) {
            applicationRegexList.add( new ApplicationRegex( name, getAllAvailableMetrics, defaultDisableAutoRollup, metricTable, eventTable, baselineTable,
                    getAllEvents, getAllHealthRuleViolations, metrics, granularityMinutes, onlyGetDefaultBaseline ));
        } else {
            Application application = new Application(getAllAvailableMetrics, name, defaultDisableAutoRollup, metricTable, eventTable, baselineTable,
                    getAllEvents, getAllHealthRuleViolations, metrics, granularityMinutes, onlyGetDefaultBaseline);
            application.setEventTypeList(getEventListForApplication(includeEventList, excludeEventList));
            if (eventSeverities != null) application.eventSeverities = eventSeverities;
            applications.add(application);
        }
        metrics = new ArrayList<>();
    }


    private void addController (ControllerConfig controllerConfig) throws InvalidConfigurationException {
        //order of this is important, because Apache Digester didn't have context so external lists exist that are added as we go
        for(ApplicationConfig applicationConfig : controllerConfig.getApplicationList()) {
            //First, add metrics to the global metric list
            for(ApplicationMetricConfig metricConfig : applicationConfig.getMetricList() )
                addMetric( metricConfig.getTimeRangeType(), metricConfig.getDurationInMins(), metricConfig.getValue());
            //Second, add applications, each of which will purge the metric list and
            addApplication( applicationConfig.isGetAllAvailableMetrics(), applicationConfig.getName().getName(),
                    applicationConfig.getDefaults().isDisableDataRollup(), applicationConfig.getDefaults().getMetricTable(),
                    applicationConfig.getDefaults().getEventTable(), applicationConfig.getDefaults().getBaselineTable(),
                    applicationConfig.isGetAllEvents(), applicationConfig.isGetAllHealthRuleViolations(),
                    applicationConfig.getEvents().getInclude(), applicationConfig.getEvents().getExclude(),
                    applicationConfig.getEvents().getSeverities(), applicationConfig.getName().isRegex(),
                    applicationConfig.getDefaults().getGranularityMinutes(), applicationConfig.getDefaults().isOnlyGetDefaultBaseline());
        }

        //Last, add the controller, which adds all the applications in the global list and purges it
        addController( controllerConfig.getUrl(), controllerConfig.getClientID(), controllerConfig.getClientSecret(),
                controllerConfig.isGetAllAnalyticsSearches(), controllerConfig.getAdjustEndTimeMinutes());
    }


    public void addController( String urlString, String clientID, String clientSecret, boolean getAllAnalyticsSearches, long minutesToAdjustEndTimestampBy ) throws InvalidConfigurationException {
        if( urlString == null || clientID == null || clientSecret == null ) {
            logger.warn("No valid minimum config parameters for Controller! Ensure URL, ClientID, and ClientSecret are configured");
            throw new InvalidConfigurationException("No valid minimum config parameters for Controller! Ensure URL, ClientID, and ClientSecret are configured");
        }
        if( ((applications == null || applications.isEmpty()) && (applicationRegexList == null || applicationRegexList.isEmpty())) && !getAllAnalyticsSearches ) {
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
        if( minutesToAdjustEndTimestampBy < 0 ) {
            logger.warn("Configuration option <AdjustEndTimeMinutes>%d</AdjustEndTimeMinutes> is a bit strange, we subtract this from the end timestamp, we are going to swap the negative to positive and subtract that for you, maybe rethink this setting", minutesToAdjustEndTimestampBy);
            minutesToAdjustEndTimestampBy *= -1;
        }
        try{
            Controller controller = new Controller(urlString, clientID, clientSecret, applications.toArray( new Application[0] ), getAllAnalyticsSearches, applicationRegexList.toArray( new ApplicationRegex[0]), minutesToAdjustEndTimestampBy, this);
            applications = new ArrayList<>();
            applicationRegexList = new ArrayList<>();
            controllerMap.put( controller.hostname, controller);
            logger.info("Added Controller  to config for host: %s url: %s", controller.hostname, urlString);
        } catch (MalformedURLException exception) {
            logger.error("Could not create controller from config file because of a bad URL: %s Exception: %s", urlString, exception.getMessage());
        }
    }

    private void setSchedulerProperties (SchedulerConfig schedulerConfig) {
        if( schedulerConfig == null ) {
            setSchedulerProperties(false,0l, 2l, 10l, 50l, 12l, 2l, 14l, false);
        } else {
            setSchedulerProperties(schedulerConfig.isEnabled(), schedulerConfig.getPollIntervalMinutes(), schedulerConfig.getFirstRunHistoricNumberOfHours(), schedulerConfig.getControllerThreads(), schedulerConfig.getDatabaseThreads(), schedulerConfig.getConfigurationRefreshEveryHours(), schedulerConfig.getFirstRunHistoricNumberOfDays(), schedulerConfig.getMaxNumberOfDaysToQueryAtATime(), true);
        }
    }

    public void setSchedulerProperties( boolean enabledFlag, Long pollIntervalMinutes, Long firstRunHistoricNumberOfHours, Long numberOfControllerThreads, Long numberOfDatabaseThreads, Long numberConfigRefreshHours, Long firstRunHistoricNumberOfDays, Long maxNumberOfDaysToQueryAtATime, boolean printOutput ) {
        if( enabledFlag ) {
            properties.setProperty("scheduler-enabled", "true");
            this.definedScheduler = true;
        } else {
            if(printOutput) logger.info("MainControlScheduler is disabled, running only once");
            properties.setProperty("scheduler-enabled", "false");
        }
        if(printOutput) logger.info("Setting poll interval to every %d minutes", pollIntervalMinutes);
        this.properties.setProperty("scheduler-pollIntervalMinutes", pollIntervalMinutes.toString());
        if( firstRunHistoricNumberOfDays != null && firstRunHistoricNumberOfDays > 0 )
            firstRunHistoricNumberOfHours = firstRunHistoricNumberOfDays*24;
        if( firstRunHistoricNumberOfHours == null || firstRunHistoricNumberOfHours <= 0 ) {
            firstRunHistoricNumberOfHours = 48l;
        }
        if(printOutput) logger.info("Setting first run historic data to pull to %d hours", firstRunHistoricNumberOfHours);
        this.properties.setProperty("scheduler-FirstRunHistoricNumberOfHours", firstRunHistoricNumberOfHours.toString());
        if( numberOfControllerThreads == null || numberOfControllerThreads < 1 ) {
            numberOfControllerThreads = 10l;
        }
        if(printOutput) logger.info("Setting Number of Controller Communication Threads to %d", numberOfControllerThreads);
        this.properties.setProperty("scheduler-NumberOfControllerThreads", numberOfControllerThreads.toString());
        if( "".equals(numberOfDatabaseThreads) || numberOfDatabaseThreads == null ) {
            numberOfDatabaseThreads = 50l;
        }
        if(printOutput) logger.info("Setting Number of Database Communication Threads to %d", numberOfDatabaseThreads);
        this.properties.setProperty("scheduler-NumberOfDatabaseThreads", numberOfDatabaseThreads.toString());
        if( numberConfigRefreshHours == null || numberConfigRefreshHours < 0 ) {
            numberConfigRefreshHours = 12l;
        }
        if(printOutput) logger.info("Setting Number of Hours to refresh controller application metric list to %d", numberConfigRefreshHours);
        this.properties.setProperty("scheduler-ConfigRefreshHours", numberConfigRefreshHours.toString());
        if( maxNumberOfDaysToQueryAtATime == null || maxNumberOfDaysToQueryAtATime < 1) {
            maxNumberOfDaysToQueryAtATime = 14l;
        }
        if(printOutput) logger.info("Setting Max Number of Days to Query at a time on the controller to %d", maxNumberOfDaysToQueryAtATime);
        this.properties.setProperty("scheduler-MaxQueryDays", maxNumberOfDaysToQueryAtATime.toString());
    }

    private void setTargetDBProperties (TargetDBConfig targetDB) throws InvalidConfigurationException {
        setTargetDBProperties( targetDB.getConnectionString(), targetDB.getUser(), targetDB.getPassword(),
                targetDB.getDefaultMetricTable(), targetDB.getControlTable(), targetDB.getDefaultEventTable(),
                targetDB.getDefaultBaselineTable(), targetDB.getMaximumColumnNameLength());
    }

    public void setTargetDBProperties( String connectionString, String user, String password, String metricTable, String controlTable, String eventTable, String baselineTable, Long maximumColumnNameLength ) throws InvalidConfigurationException {
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

        switch( Utility.parseDatabaseVendor(connectionString).toLowerCase() ) {
            case "oracle": {
                this.database = new OracleDatabase(this, connectionString, user, password, metricTable, controlTable, eventTable, baselineTable, getProperty("scheduler-FirstRunHistoricNumberOfHours", 48L), maximumColumnNameLength.intValue());
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
        return getProperty("scheduler-MaxQueryDays", 14l) *24*60*60*1000;
    }
}
