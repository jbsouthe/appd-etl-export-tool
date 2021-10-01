package com.cisco.josouthe;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.xml.sax.SAXException;

import java.io.IOException;

public class ETLTransferMain {
    private static final Logger logger = LogManager.getFormatterLogger();

    public static void main( String... args ) {
        logger.info("Initializing ETL Transfer Tool");
        String configFileName = "default-config.xml";
        if( args.length > 0 ) configFileName=args[0];
        Configuration config = null;
        try {
            config = new Configuration(configFileName);
        } catch (IOException e) {
            logger.fatal("Can not read configuration file: %s Exception: %s", configFileName, e.getMessage());
            return;
        } catch (SAXException e) {
            logger.fatal("XML Parser Error reading config file: %s Exception: %s", configFileName, e.getMessage());
            return;
        } catch (Exception e) {
            logger.fatal("A configuration exception was thrown that we can't handle, so we are quiting, Exception: %s",e);
            return;
        }

        Scheduler scheduler = new Scheduler( config );
        scheduler.run();

    }
}