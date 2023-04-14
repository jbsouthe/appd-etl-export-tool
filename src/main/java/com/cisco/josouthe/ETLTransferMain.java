package com.cisco.josouthe;

import com.cisco.josouthe.config.Configuration;
import com.cisco.josouthe.scheduler.MainControlScheduler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.xml.sax.SAXException;

import javax.management.ObjectName;
import java.io.IOException;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.Arrays;

public class ETLTransferMain {
    private static final Logger logger = LogManager.getFormatterLogger();

    public static void main( String... args ) {
        logger.warn("USING A DEPRECATED MAIN COMMAND LINE, PLEASE RUN WITH NEW COMMAND VERSION (check usage java -jar <the jar file you built or downloaded>)");
        logger.info("Initializing ETL Transfer Tool version %s build date %s", MetaData.VERSION, MetaData.BUILDTIMESTAMP);
        logger.info("Report issues and concerns to: %s", MetaData.GITHUB);

        try {
            Object flags = ManagementFactory.getPlatformMBeanServer().invoke(ObjectName.getInstance("com.sun.management:type=DiagnosticCommand"), "vmFlags", new Object[] { null }, new String[] { "[Ljava.lang.String;" });
            for (String f : ((String) flags).split("\\s+"))
                logger.info("GC Config Setting: %s", f);
            for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans())
                logger.info("GC Config Setting: %-20s%s%n", gc.getName(), Arrays.toString(gc.getMemoryPoolNames()));
        } catch (Exception e) {
            logger.warn("Error when trying to probe GC Configuration: %s",e,e);
        }

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
            logger.fatal("A configuration exception was thrown that we can't handle, so we are quiting, Exception: ",e);
            return;
        }

        MainControlScheduler mainControlScheduler = new MainControlScheduler( config );
        mainControlScheduler.run();

    }
}