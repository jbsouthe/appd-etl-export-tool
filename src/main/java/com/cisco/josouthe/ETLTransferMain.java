package com.cisco.josouthe;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ETLTransferMain {
    private static final Logger logger = LogManager.getFormatterLogger();

    public static void main( String... args ) {
        logger.trace("Initializing ETL Transfer Tool");
        String configFileName = "default-config.xml";
        if( args.length > 0 ) configFileName=args[0];
        Configuration config = new Configuration(configFileName);
    }
}