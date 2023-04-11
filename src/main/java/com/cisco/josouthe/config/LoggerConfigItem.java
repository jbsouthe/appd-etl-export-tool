package com.cisco.josouthe.config;

import com.cisco.josouthe.exceptions.InvalidConfigurationException;
import org.apache.logging.log4j.Level;

public class LoggerConfigItem {
    public String name;
    public Level level;
    public Boolean additivity;

    public LoggerConfigItem(String name, String optionalLevel, String optionalAdditivity ) throws InvalidConfigurationException {
        this.name = name;
        if( optionalLevel == null ) { optionalLevel="INFO"; } //default
        this.level = Level.getLevel(optionalLevel);
        if( this.level == null ) throw new InvalidConfigurationException(String.format("<ETLTool><Logging><Logger><Level> of '%s' for logger '%s' is not a valid setting of TRACE|DEBUG|INFO|WARN|ERROR", optionalLevel, name));
        this.additivity = false; //default
        if( optionalAdditivity != null && optionalAdditivity.equalsIgnoreCase("true") ) {
            this.additivity = true;
        }
    }
}
