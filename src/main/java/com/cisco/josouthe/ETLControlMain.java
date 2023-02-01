package com.cisco.josouthe;

import com.cisco.josouthe.config.Configuration;
import com.cisco.josouthe.data.Analytics;
import com.cisco.josouthe.data.Controller;
import com.cisco.josouthe.database.ControlEntry;
import com.cisco.josouthe.database.Database;
import com.cisco.josouthe.print.IPrintable;
import com.cisco.josouthe.print.Printer;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ETLControlMain {
    private static final Logger logger = LogManager.getFormatterLogger();

    public static void main( String[] args ) {

        ArgumentParser parser = ArgumentParsers.newFor("ETLControl")
                .singleMetavar(true)
                .build()
                .defaultHelp(true)
                .version(String.format("ETL Control Tool version %s build date %s", MetaData.VERSION, MetaData.BUILDTIMESTAMP))
                .description("Manage ETL Export by Manipulating the Database Control Table.");
        parser.addArgument("-v", "--version").action(Arguments.version());
        parser.addArgument("-c", "--config")
                .setDefault("default-config.xml")
                .metavar("./config-file.xml")
                .help("Use this specific XML config file.");
        parser.addArgument("--controller")
                .metavar("Controller")
                .help("Only show rows for a specific controller");
        parser.addArgument("--application")
                .metavar("Application")
                .help("Only show rows for a specific application");
        parser.addArgument("-t", "--type")
                .metavar("Type")
                .choices("MetricData", "EventData", "AnalyticsData")
                .help("Only show rows for a specific data type: {\"MetricData\", \"EventData\", \"AnalyticsData\"}");
        parser.addArgument("command").nargs("*").setDefault("status");

        Namespace namespace = null;
        try {
            namespace = parser.parseArgs(args);
            logger.info("parser: %s", namespace);
        } catch (ArgumentParserException e) {
            parser.handleError(e);
            System.exit(1);
        }

        String configFileName = namespace.getString("config");
        Configuration config = null;
        try {
            config = new Configuration(configFileName, true);
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

        Database database = config.getDatabase();

        List<String> commands = namespace.getList("command");
        System.out.println(String.format("command: '%s' controller: '%s' application: '%s' type: '%s'",
                commands, namespace.getString("controller"), namespace.getString("application"), namespace.getString("type")));
        for( String command : commands )
            switch(command) {
                case "status": {
                    List<ControlEntry> entries = database.getControlTable().getControlEntries();
                    List<IPrintable> printables = new ArrayList<>();
                    for( ControlEntry controlEntry : entries ) {
                        if( namespace.getString("controller") != null )
                            if( !controlEntry.controller.equals(namespace.getString("controller"))) continue;
                        if( namespace.getString("application") != null )
                            if( !controlEntry.controller.equals(namespace.getString("application"))) continue;
                        if( namespace.getString("type") != null )
                            if( !controlEntry.controller.equals(namespace.getString("type"))) continue;
                        printables.add((IPrintable) controlEntry);
                    }
                    System.out.println( Printer.print( printables ) );
                    break;
                }
                case "tables": {
                    List<IPrintable> tableStatistics = new ArrayList<>();
                    for(Analytics analytics : config.getAnalyticsList())
                        tableStatistics.addAll(database.getAnalyticsTableStatistics(analytics.getAllSearchTables(), analytics, namespace.getString("controller"), namespace.getString("application"), namespace.getString("type")));
                    for(Controller controller : config.getControllerList() )
                        tableStatistics.addAll(database.getControllerTableStatistics(controller.getAllApplicationTables(), namespace.getString("controller"), namespace.getString("application"), namespace.getString("type")));
                    System.out.println( Printer.print(tableStatistics) );
                }
            }

    }
}