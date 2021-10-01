package com.cisco.josouthe;

import com.cisco.josouthe.data.*;
import com.cisco.josouthe.database.Database;
import com.cisco.josouthe.exceptions.InvalidConfigurationException;
import com.cisco.josouthe.util.Utility;
import org.apache.commons.digester3.Digester;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.xml.sax.SAXException;

import java.io.*;
import java.net.MalformedURLException;
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
    private ArrayList<ApplicationMetric> metrics = new ArrayList<>();;
    private boolean definedScheduler = false;
    private boolean definedController = false;
    private boolean definedApplication = false;

    public String getProperty( String key ) {
        return getProperty(key, null);
    }
    public String getProperty( String key , String defaultValue) {
        return this.properties.getProperty(key, defaultValue);
    }
    public Boolean getPropertyAsBoolean( String key, Boolean defaultBoolean) {
        return Boolean.parseBoolean( getProperty(key, defaultBoolean.toString()));
    }
    public Long getPropertyAsLong( String key, Long defaultLong ) {
        return Long.parseLong( getProperty(key, defaultLong.toString()));
    }

    public Database getDatabase() { return database; }
    public Controller getController( String hostname ) { return controllerMap.get(hostname); }
    public Controller[] getControllerList() { return controllerMap.values().toArray(new Controller[0]); }

    public Configuration( String configFileName) throws Exception {
        logger.info("Processing Config File: %s", configFileName);
        this.properties = new Properties();
        this.controllerMap = new HashMap<>();
        Digester digester = new Digester();
        digester.push(this);
        //scheduler config section default enabled with 10 minute run intervals
        digester.addCallMethod("ETLTool/Scheduler", "setSchedulerProperties", 2 );
        digester.addCallParam("ETLTool/Scheduler", 0 , "enabled");
        digester.addCallParam("ETLTool/Scheduler/PollIntervalMinutes", 1 );

        //database configuration section
        digester.addCallMethod("ETLTool/TargetDB", "setTargetDBProperties", 4);
        digester.addCallParam("ETLTool/TargetDB/ConnectionString", 0);
        digester.addCallParam("ETLTool/TargetDB/User", 1);
        digester.addCallParam("ETLTool/TargetDB/Password", 2);
        digester.addCallParam("ETLTool/TargetDB/DefaultTable", 3);

        //controller section, which centralizes authentication config
        digester.addCallMethod("ETLTool/Controller", "addController", 3);
        digester.addCallParam("ETLTool/Controller/URL", 0);
        digester.addCallParam("ETLTool/Controller/ClientID", 1);
        digester.addCallParam("ETLTool/Controller/ClientSecret", 2);

        //application config, within a controller
        digester.addCallMethod("ETLTool/Controller/Application", "addApplication", 5);
        digester.addCallParam("ETLTool/Controller/Application", 0, "getAllAvailableMetrics");
        digester.addCallParam("ETLTool/Controller/Application/Name", 1);
        digester.addCallParam("ETLTool/Controller/Application/Defaults/TimeRangeType", 2);
        digester.addCallParam("ETLTool/Controller/Application/Defaults/DurationInMins", 3);
        digester.addCallParam("ETLTool/Controller/Application/Defaults/DisableDataRollup", 4);


        //metric config, within an application
        digester.addCallMethod( "ETLTool/Controller/Application/Metric", "addMetric", 4);
        digester.addCallParam("ETLTool/Controller/Application/Metric", 0, "time-range-type");
        digester.addCallParam("ETLTool/Controller/Application/Metric", 1, "duration-in-mins");
        digester.addCallParam("ETLTool/Controller/Application/Metric", 2, "disable-data-rollup");
        digester.addCallParam("ETLTool/Controller/Application/Metric", 3);

        digester.parse( new File(configFileName) );
        if( ! definedScheduler ) {
            logger.warn("No scheduler defined in the config file, we are going to configure a basic one run scheduler for you");
            setSchedulerProperties("false","");
        }

        logger.info("Validating Configured Settings");
        for( Controller controller : getControllerList() ) {
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
        if( ! definedController || ! definedApplication ) {
            logger.warn("Config file doesn't have a controller or application configured? not one? we can't do much here");
            throw new InvalidConfigurationException("Config file doesn't have a controller or application configured? not one? we can't do much here");
        }
        if(database != null && database.isDatabaseAvailable()) {
            logger.info("Database is available");
        } else {
            throw new InvalidConfigurationException("Database is not configured, available, or authetication information is incorrect. Something is wrong, giving up on this buddy");
        }
    }

    public void addMetric( String timeRangeType, String durationInMins, String disableDataRollup, String name ) throws InvalidConfigurationException {
        if( name == null ) {
            logger.warn("No valid minimum config parameters for Metric! Ensure Metric is named with fully qualified metric path name");
            throw new InvalidConfigurationException("No valid minimum config parameters for Metric! Ensure Metric is named with fully qualified metric path name");
        }
        ApplicationMetric metric = new ApplicationMetric( timeRangeType, durationInMins, disableDataRollup, name);
        metrics.add(metric);
        logger.info("Added metric to list for collection: %s", name);
    }

    public void addApplication( String getAllAvailableMetrics, String name , String defaultTimeRangeType, String defaultDurationInMinutes, String defaultDisableAutoRollup ) throws InvalidConfigurationException {
        if( name == null ) {
            logger.warn("No valid minimum config parameters for Application! Ensure Name is configured");
            throw new InvalidConfigurationException("No valid minimum config parameters for Application! Ensure Name is configured");
        }
        Application application = new Application( getAllAvailableMetrics, name, defaultTimeRangeType, defaultDurationInMinutes, defaultDisableAutoRollup, metrics.toArray( new ApplicationMetric[0] ));
        applications.add(application);
        metrics = new ArrayList<>();
    }

    public void addController( String urlString, String clientID, String clientSecret) throws InvalidConfigurationException {
        if( urlString == null || clientID == null || clientSecret == null ) {
            logger.warn("No valid minimum config parameters for Controller! Ensure URL, ClientID, and ClientSecret are configured");
            throw new InvalidConfigurationException("No valid minimum config parameters for Controller! Ensure URL, ClientID, and ClientSecret are configured");
        }
        if( applications == null || applications.isEmpty() ) {
            logger.warn("Controller configured, but no applications configured, please add at least one application");
            throw new InvalidConfigurationException("Controller configured, but no applications configured, please add at least one application");
        }
        try{
            Controller controller = new Controller(urlString, clientID, clientSecret, applications.toArray( new Application[0] ));
            applications = new ArrayList<>();;
            controllerMap.put( controller.hostname, controller);
            logger.info("Added Controller  to config for host: %s url: %s", controller.hostname, urlString);
        } catch (MalformedURLException exception) {
            logger.error("Could not create controller from config file because of a bad URL: %s Exception: %s", urlString, exception.getMessage());
        }
    }

    public void setSchedulerProperties( String enabledFlag, String pollIntervalMinutes ) {
        Boolean enabled = true;
        if( "false".equalsIgnoreCase(enabledFlag) ) {
            logger.info("Scheduler is disabled, running only once");
            properties.setProperty("scheduler-enabled", "false");
            return;
        } else {
            properties.setProperty("scheduler-enabled", "true");
        }
        if( "".equals(pollIntervalMinutes) ) {
            pollIntervalMinutes = "10";
        }
        logger.info("Setting poll interval to every %s minutes", pollIntervalMinutes);
        this.properties.setProperty("scheduler-pollIntervalMinutes", pollIntervalMinutes);
    }

    public void setTargetDBProperties( String connectionString, String user, String password, String defaultTable ) throws InvalidConfigurationException {
        if( connectionString == null || user == null || password == null ) {
            logger.warn("No valid minimum config parameters for ETL Database! Ensure Connection String, User, and Password are configured");
            throw new InvalidConfigurationException("No valid minimum config parameters for ETL Database! Ensure Connection String, User, and Password are configured");
        }
        logger.debug("Setting Target DB: %s", connectionString);
        properties.setProperty("database-vendor", Utility.parseDatabaseVendor(connectionString));
        if( ! "".equals(defaultTable) && isValidDatabaseTableName(defaultTable) ) logger.debug("Default Table set to: %s", defaultTable);
        this.database = new Database( connectionString, user, password, ( defaultTable == null ? "AppDynamics_DefaultTable" : defaultTable));
    }

    private List<String> _tableNameForbiddenWords = null;
    private boolean isValidDatabaseTableName( String tableName ) throws InvalidConfigurationException {
        if(_tableNameForbiddenWords == null) loadTableNameInvalidWords();
        if( tableName.length() > 30 ) throw new InvalidConfigurationException(String.format("Database table name longer than max 30 characters, by like %d characters. change this name: %s",(tableName.length()-30),tableName));
        if(! Character.isAlphabetic(tableName.charAt(0))) throw new InvalidConfigurationException(String.format("Database table name must begin with an alphabetic character, not this thing '%s' change this table: %s",tableName.charAt(0),tableName));
        long countSpecialCharacters = tableName.chars().filter(ch -> ch == '#' || ch == '$' || ch == '_').count();
        long countInvalidCharacters = tableName.chars().filter(ch -> ch != '#' && ch != '$' && ch != '_' && ! Character.isAlphabetic(ch) && ! Character.isDigit(ch)).count();
        if( countSpecialCharacters > 1 || countInvalidCharacters > 0 ) throw new InvalidConfigurationException(String.format("Only one of the special characters ($, #, or _) is allowed and the rest must be alphanumeric only, not my rules, (shrug), change this table name: %s",tableName));
        if( _tableNameForbiddenWords.contains(tableName.toLowerCase()) ) throw new InvalidConfigurationException(String.format("The table name is a reserved word for this database vendor %s, please picking something different than: %s",getProperty("database-vendor", "OOPS, no vendor??"), tableName));
        return true; //if we made it this far without an exception, we are good to go
    }

    private void loadTableNameInvalidWords() throws InvalidConfigurationException {
        String filename = getProperty("database-vendor").toLowerCase() + "-forbidden-words.txt";
        logger.debug("Loading file: %s",filename);
        try (BufferedReader reader = new BufferedReader( new InputStreamReader(getClass().getClassLoader().getResourceAsStream(filename)))) {
            _tableNameForbiddenWords = new ArrayList<>();
            String word;
            while ( (word = reader.readLine()) != null )
                _tableNameForbiddenWords.add(word.toLowerCase());
        } catch (IOException e) {
            logger.warn("Error reading list of forbidden table names from internal file %s, this database may not be supported: %s Exception: %s", filename, getProperty("database-vendor","UNKNOWN DATABASE!"), e.getMessage());
            throw new InvalidConfigurationException(String.format("Error reading list of forbidden table names from internal file %s, this database may not be supported: %s", filename, getProperty("database-vendor","UNKNOWN DATABASE!")));
        }
    }
}
