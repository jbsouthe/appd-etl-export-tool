package com.cisco.josouthe;

import com.cisco.josouthe.config.Configuration;
import com.cisco.josouthe.data.Analytics;
import com.cisco.josouthe.data.Controller;
import com.cisco.josouthe.database.ControlEntry;
import com.cisco.josouthe.database.Database;
import com.cisco.josouthe.exceptions.BadCommandException;
import com.cisco.josouthe.print.IPrintable;
import com.cisco.josouthe.print.Printer;
import com.cisco.josouthe.scheduler.MainControlScheduler;
import com.cisco.josouthe.util.Utility;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
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
                .description("Manage ETL Export by Manipulating the Database Control Table and Data Tables.");
        parser.addArgument("-v", "--version").action(Arguments.version());
        parser.addArgument("-c", "--config")
                .setDefault("default-config.xml")
                .metavar("./config-file.xml")
                .help("Use this specific XML config file.");
        parser.addArgument("--controller")
                .metavar("Controller")
                .help("Only manage a specific controller");
        parser.addArgument("--application")
                .metavar("Application")
                .help("Only manage a specific application");
        parser.addArgument("--type")
                .metavar("Type")
                .choices("MetricData", "EventData", "AnalyticsData")
                .help("Only manage a specific data type: {\"MetricData\", \"EventData\", \"AnalyticsData\"}");
        parser.addArgument("command")
                .nargs("*")
                .help("Commands are probably too flexible, some examples include: {\"show [status|tables]\", \"[select|drop|delete|update] <rest of sql statement with \\* escaped>\", \"purge [tableName] [newer|older] than [yyyy-MM-dd_HH:mm:ss_z]\", \"executeScheduler\" }")
                .setDefault("executeScheduler");

        Namespace namespace = null;
        try {
            namespace = parser.parseArgs(args);
            logger.info("parser: %s", namespace);
        } catch (ArgumentParserException e) {
            parser.handleError(e);
            System.exit(1);
        }
        List<String> commands = Utility.getCommands(namespace);
        logger.info(String.format("command: '%s' controller: '%s' application: '%s' type: '%s'",
                commands, namespace.getString("controller"), namespace.getString("application"), namespace.getString("type")));
        String configFileName = namespace.getString("config");
        Configuration config = null;
        try {
            config = new Configuration(configFileName, "executeScheduler".equals(commands.get(0)));
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

        boolean forceExit=true;
        try {
            switch (commands.get(0).toLowerCase()) {
                case "executescheduler": {
                    forceExit=false;
                    MainControlScheduler mainControlScheduler = new MainControlScheduler( config );
                    mainControlScheduler.run();
                    break;
                }
                case "show": {
                    parseShowCommand(namespace, config);
                    break;
                }
                case "select": {
                    try {
                        config.getDatabase().executeQuery(namespace);
                    } catch (SQLException sqlException ) {
                        logger.warn(sqlException,sqlException);
                    }
                    break;
                }
                case "drop":
                case "delete":
                case "update":
                {
                    try {
                        config.getDatabase().executeUpdate(namespace);
                    } catch (SQLException sqlException ) {
                        logger.warn(sqlException,sqlException);
                    }
                    break;
                }
                case "purge": {
                    parsePurgeCommand(namespace, config);
                    break;
                }
                case "set": {
                    throw new BadCommandException("Set command not yet implemented", commands.get(0));
                }
                default: {
                    throw new BadCommandException("Unknown root command", commands.get(0));
                }
            }
        } catch (BadCommandException badCommandException ) {
            logger.warn("Bad Command Exception: %s", badCommandException.getMessage(), commands.get(0));
            printHelpAndExit(parser, -1);
        }
        if( forceExit ) System.exit(0);
    }

    //purge [tableName] [newer|older] than [yyyy-MM-dd_HH:mm:ss_z]
    private static void parsePurgeCommand(Namespace namespace, Configuration configuration) throws BadCommandException {
        Database database = configuration.getDatabase();
        List<String> commands = Utility.getCommands(namespace);
        if( commands.size() < 5 ) throw new BadCommandException(String.format("command %s needs 5 arguments", commands.get(0)), null);
        String tableName = commands.get(1);
        if( !database.doesTableExist( tableName, configuration ) ) throw new BadCommandException(String.format("command %s table doesn\'t exist or it is just empty", commands.get(0)), commands.get(1));
        Boolean purgeOlderData;
        switch( commands.get(2).toLowerCase() ) {
            case "newer": { purgeOlderData=false; break; }
            case "older": { purgeOlderData=true; break; }
            default: { throw new BadCommandException(String.format("command %s direction invalid, must be either newer or older than", commands.get(0)), commands.get(2)); }
        }
        if( !commands.get(3).toLowerCase().equals("than") )
            throw new BadCommandException(String.format("command %s bad format, this is the easy part, just type \"than\" that is all i can accept", commands.get(0)), commands.get(3));
        long purgeTimestamp = 0l;
        try {
            purgeTimestamp = Utility.parseControlDateString( commands.get(4) );
        } catch (ParseException e) {
            throw new BadCommandException(String.format("Error trying to parse date string '%s' remember to use this format '%s'", commands.get(4), Utility.controlDateFormatString));
        }
        long purgedCount = database.purgeData( tableName, purgeOlderData, purgeTimestamp, configuration);
        System.out.println(String.format("Purged %d rows from %s table", purgedCount, tableName));
    }

    private static void parseShowCommand(Namespace namespace, Configuration configuration) throws BadCommandException {
        Database database = configuration.getDatabase();
        List<String> commands = Utility.getCommands(namespace);
        if( commands.size() < 2 ) throw new BadCommandException(String.format("command %s needs an argument", commands.get(0)), null);
        switch (commands.get(1).toLowerCase()) {
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
                System.out.println(String.format("Control Table: %s", database.getControlTable().getName() ));
                System.out.println( Printer.print( printables ) );
                break;
            }
            case "tables": {
                List<IPrintable> tableStatistics = new ArrayList<>();
                for(Analytics analytics : configuration.getAnalyticsList())
                    tableStatistics.addAll(database.getAnalyticsTableStatistics(analytics.getAllSearchTables(), analytics, namespace.getString("controller"), namespace.getString("application"), namespace.getString("type")));
                for(Controller controller : configuration.getControllerList() )
                    tableStatistics.addAll(database.getControllerTableStatistics(controller.getAllApplicationTables(), namespace.getString("controller"), namespace.getString("application"), namespace.getString("type")));
                System.out.println( Printer.print(tableStatistics) );
                break;
            }
            default: {
                throw new BadCommandException("Unknown sub command: "+ commands, commands.get(1));
            }
        }
    }

    private static void printHelpAndExit( ArgumentParser parser, int exitCode ) {
        parser.printHelp();
        System.exit(exitCode);
    }
}