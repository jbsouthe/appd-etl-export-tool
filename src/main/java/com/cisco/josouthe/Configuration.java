package com.cisco.josouthe;

import org.apache.commons.digester3.Digester;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.security.DigestException;
import java.util.Properties;

public class Configuration {
    private static final Logger logger = LogManager.getFormatterLogger();
    private Properties properties = null;

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

    public Configuration( String configFileName) {
        logger.info("Processing Config File: %s", configFileName);
        this.properties = new Properties();
        Digester digester = new Digester();
        digester.push(this);
        //scheduler config section default enabled with 10 minute run intervals
        digester.addCallMethod("ETLTool/Scheduler", "setSchedulerProperties", 6 );
        digester.addCallParam("ETLTool/Scheduler/Enabled", 0 );
        digester.addCallParam("ETLTool/Scheduler/PollIntervalMinutes", 1 );

        //database configuration section
        digester.addCallMethod("ETLTool/TargetDB", "setTargetDBProperties", 4);
        digester.addCallParam("ETLTool/TargetDB/ConnectionString", 0);
        digester.addCallParam("ETLTool/TargetDB/User", 1);
        digester.addCallParam("ETLTool/TargetDB/Password", 2);
        digester.addCallParam("ETLTool/TargetDB/DefaultTable", 3);

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

    public void setTargetDBProperties( String connectionString, String user, String password, String defaultTable ) throws Exception {
        if( connectionString == null || user == null || password == null ) {
            logger.warn("No valid minimum config parameters for ETL Database! Ensure Connection String, User, and Password are configured");
            throw new Exception("No valid minimum config parameters for ETL Database! Ensure Connection String, User, and Password are configured");
        }
        logger.debug("Setting Target DB: %s", connectionString);
        if( ! "".equals(defaultTable) ) logger.debug("Default Table set to: %s", defaultTable);
        this.properties.setProperty("database-connectionString", connectionString);
        this.properties.setProperty("database-user",user);
        this.properties.setProperty("database-password",password);
        this.properties.setProperty("database-defaultTable",( defaultTable == null ? "AppDynamics-DefaultTable" : defaultTable));
    }
}
