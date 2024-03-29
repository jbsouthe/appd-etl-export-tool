package com.cisco.josouthe.database;

import com.cisco.josouthe.config.Configuration;
import com.cisco.josouthe.data.Analytics;
import com.cisco.josouthe.data.Controller;
import com.cisco.josouthe.data.analytic.Result;
import com.cisco.josouthe.data.event.EventData;
import com.cisco.josouthe.data.metric.BaselineData;
import com.cisco.josouthe.data.metric.MetricData;
import com.cisco.josouthe.exceptions.BadCommandException;
import com.cisco.josouthe.exceptions.FailedDataLoadException;
import com.cisco.josouthe.exceptions.InvalidConfigurationException;
import com.cisco.josouthe.print.IPrintable;
import com.cisco.josouthe.print.Printer;
import com.cisco.josouthe.print.ResultSetPrinter;
import com.cisco.josouthe.print.TablePrinter;
import com.cisco.josouthe.util.Utility;
import com.cisco.josouthe.http.WorkingStatusThread;
import net.sourceforge.argparse4j.inf.Namespace;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class Database {
    private static final Logger logger = LogManager.getFormatterLogger();

    public static final String STRING_TYPE = "varchar2";
    public static final int STRING_SIZE = 120;
    public static final String INTEGER_TYPE = "number";
    public static final int INTEGER_SIZE = 22;
    public static final String FLOAT_TYPE = "number";
    public static final int FLOAT_SIZE = 22;
    public static final String DATE_TYPE = "date";
    public static final int DATE_SIZE = -1;
    public static final String BOOLEAN_TYPE = "number";
    public static final int BOOLEAN_SIZE = 1;
    public static final int MAX_CONNECTION_RETRY = 5;

    protected Configuration configuration;
    protected String connectionString, user, password, vendorName;
    protected Table defaultMetricTable, controlTable, defaulEventTable, defaultBaselineTable;
    protected Map<String,Table> tablesMap = new HashMap<>();

    public Database( Configuration configuration, String connectionString, String user, String password ) {
        this.configuration = configuration;
        this.connectionString = connectionString;
        this.user = user;
        this.password = password;
        this.vendorName = Utility.parseDatabaseVendor(connectionString);
    }

    public Map<String,Table> getTablesMap() { return this.tablesMap; }

    public IControlTable getControlTable() { return (IControlTable) controlTable; }
    protected abstract IAnalyticTable getAnalyticTable(Result result );
    protected abstract IEventTable getEventTable(String name );
    protected abstract IMetricTable getMetricTable(String name );
    protected abstract IBaselineTable getBaselineTable(String name );

    public abstract boolean isDatabaseAvailable();

    public void importData( Object[] someData ) throws FailedDataLoadException {
        if( someData == null || someData.length == 0 ) return;
        if( someData instanceof MetricData[] ) {
            importMetricData((MetricData[]) someData);
        } else if( someData instanceof EventData[] ) {
            importEventData((EventData[]) someData);
        } else if( someData instanceof Result[] ) {
            importAnalyticData((Result[]) someData);
        } else if( someData instanceof BaselineData[] ) {
            importBaselineData((BaselineData[]) someData);
        } else {
            logger.warn("Could not determine the datatype for %s %d records",someData,someData.length);
        }
    }

    public void importMetricData(MetricData[] metricData) throws FailedDataLoadException {
        logger.trace("Beginning of import metric data method");
        if( metricData == null || metricData.length == 0 ) {
            logger.debug("Nothing to import, leaving quickly");
            return;
        }
        int cntStarted = 0;
        int cntFinished = 0;
        int cntAverageCalc = 0;
        long startTimeOverall = Utility.now();
        long maxDurationTime = -1;
        long minDurationTime = Long.MAX_VALUE;
        for( MetricData metric : metricData ) { //TODO pop a data element off the array on success, otherwise leave the data in the array, in case of exception
            if( "METRIC DATA NOT FOUND".equals(metric.metricName) ) continue;
            cntStarted+=metric.metricValues.size();
            IMetricTable table = (IMetricTable) getMetricTable(metric.targetTable);
            long startTimeTransaction = Utility.now();
            cntFinished += table.insert(metric);
            cntAverageCalc++;
            long durationTimeTransaction = Utility.now() - startTimeTransaction;
            if( durationTimeTransaction > maxDurationTime ) maxDurationTime = durationTimeTransaction;
            if( durationTimeTransaction < minDurationTime ) minDurationTime = durationTimeTransaction;
            logger.debug("Loaded %d metrics into the database in time %d(ms)", metric.metricValues.size(), durationTimeTransaction);
        }
        long durationTimeOverallMS = Utility.now() - startTimeOverall;
        if( cntStarted > 0 )
            logger.info("Attempted to load %d metrics, succeeded in loading %d metrics. Total Time %d(ms)",cntStarted,cntFinished,durationTimeOverallMS);

    }

    public void importBaselineData(BaselineData[] baselineData) throws FailedDataLoadException {
        logger.trace("Beginning of import metric data method");
        if( baselineData == null || baselineData.length == 0 ) {
            logger.debug("Nothing to import, leaving quickly");
            return;
        }
        int cntStarted = 0;
        int cntFinished = 0;
        int cntAverageCalc = 0;
        long startTimeOverall = Utility.now();
        long maxDurationTime = -1;
        long minDurationTime = Long.MAX_VALUE;
        for( BaselineData baseline : baselineData ) {
            cntStarted+=baseline.dataTimeslices.size();
            IBaselineTable table = (IBaselineTable) getBaselineTable(baseline.targetTable);
            long startTimeTransaction = Utility.now();
            cntFinished += table.insert(baseline);
            cntAverageCalc++;
            long durationTimeTransaction = Utility.now() - startTimeTransaction;
            if( durationTimeTransaction > maxDurationTime ) maxDurationTime = durationTimeTransaction;
            if( durationTimeTransaction < minDurationTime ) minDurationTime = durationTimeTransaction;
            logger.debug("Loaded %d baseline metrics into the database in time %d(ms)", baseline.dataTimeslices.size(), durationTimeTransaction);
        }
        long durationTimeOverallMS = Utility.now() - startTimeOverall;
        if( cntStarted > 0 )
            logger.info("Attempted to load %d baseline metrics, succeeded in loading %d baseline metrics. Total Time %d(ms)",cntStarted,cntFinished,durationTimeOverallMS);

    }

    public void importEventData(EventData[] events) throws FailedDataLoadException{
        logger.trace("Beginning of import event data method");
        if( events == null || events.length == 0 ) {
            logger.debug("Nothing to import, leaving quickly");
            return;
        }
        int cntStarted = 0;
        int cntFinished = 0;
        long startTimeOverall = Utility.now();
        long maxDurationTime = -1;
        long minDurationTime = Long.MAX_VALUE;
        for( EventData event : events ) {
            cntStarted++;
            long startTimeTransaction = Utility.now();
            IEventTable table = (IEventTable) getEventTable(event.targetTable);
            cntFinished += table.insert(event);
            long durationTimeTransaction = Utility.now() - startTimeTransaction;
            if( durationTimeTransaction > maxDurationTime ) maxDurationTime = durationTimeTransaction;
            if( durationTimeTransaction < minDurationTime ) minDurationTime = durationTimeTransaction;
            logger.debug("Loaded %d event into the database in time %d(ms)", 1, durationTimeTransaction);
        }
        long durationTimeOverallMS = Utility.now() - startTimeOverall;
        logger.info("Attempted to load %d events, succeeded in loading %d events. Total Time %d(ms)",cntStarted,cntFinished,durationTimeOverallMS);
    }

    public void importAnalyticData(Result[] results) throws FailedDataLoadException{
        logger.trace("Begining of import analytics search results method");
        if( results == null || results.length == 0 ) {
            logger.debug("Nothing to import, leaving quickly");
            return;
        }
        int cntStarted = 0;
        int cntFinished = 0;
        long startTimeOverall = Utility.now();
        long maxDurationTime = -1;
        long minDurationTime = Long.MAX_VALUE;
        for( Result result : results ) {
            if( result.results == null ) continue;
            cntStarted+=result.results.length;
            long startTimeTransaction = Utility.now();
            IAnalyticTable table = (IAnalyticTable) getAnalyticTable(result);
            cntFinished += table.insert(result);
            long durationTimeTransaction = Utility.now() - startTimeTransaction;
            if( durationTimeTransaction > maxDurationTime ) maxDurationTime = durationTimeTransaction;
            if( durationTimeTransaction < minDurationTime ) minDurationTime = durationTimeTransaction;
            logger.debug("Loaded %d analytic search results into the database in time %d(ms)", result.results.length, durationTimeTransaction);
        }
        long durationTimeOverallMS = Utility.now() - startTimeOverall;
        logger.info(
                "Attempted to load %d analytic search results, succeeded in loading %d rows. Total Time %d(ms)",
                cntStarted, cntFinished, durationTimeOverallMS);
    }

    public abstract Connection getConnection() throws SQLException;

    public abstract String convertToAcceptableColumnName(String label, Collection<ColumnFeatures> existingColumns);

    public abstract boolean isValidDatabaseTableName( String tableName ) throws InvalidConfigurationException;

    public abstract String convertToAcceptableTableName(String tableName );

    public List<TablePrinter> getControllerTableStatistics(List<String> tableNames, String controllerFilter, String applicationFilter, String typeFilter) {
        if( typeFilter != null ) logger.warn("We do not yet support filtering by type so you are getting everything, enjoy...");
        List<TablePrinter> tablePrinters = new ArrayList<>();
        for( String tableName : tableNames ) {
            try (Connection conn = getConnection(); Statement statement = conn.createStatement();) {
                String query = String.format(" select controller,application,min(starttimeinmillis) as minTime,max(starttimeinmillis) as maxTime, count(*) as rowSize from %s group by controller, application", tableName);
                ResultSet resultSet = statement.executeQuery(query);
                while (resultSet.next()) {
                    if( controllerFilter != null && !controllerFilter.equals(resultSet.getString("controller"))) continue;
                    if( applicationFilter != null && !applicationFilter.equals(resultSet.getString("application"))) continue;
                    TablePrinter tablePrinter = new TablePrinter();
                    tablePrinter.controller = resultSet.getString("controller");
                    tablePrinter.application = resultSet.getString("application");
                    tablePrinter.name=tableName;
                    tablePrinter.type = "ControllerData";
                    tablePrinter.oldestRowTimestamp = resultSet.getLong("minTime");
                    tablePrinter.newestRowTimestamp = resultSet.getLong("maxTime");
                    tablePrinter.size = resultSet.getLong("rowSize");
                    tablePrinters.add(tablePrinter);
                }
                resultSet.close();
            } catch (SQLException exception) {
                logger.warn("Error calculating statistics for table %s, Exception: %s", tableName, exception.toString());
            }
        }
        return tablePrinters;
    }

    public List<TablePrinter> getAnalyticsTableStatistics(List<String> tableNames, Analytics analytics, String controllerFilter, String applicationFilter, String typeFilter) {
        if( typeFilter != null ) logger.warn("We do not yet support filtering by type so you are getting everything, enjoy...");
        List<TablePrinter> tablePrinters = new ArrayList<>();
        if( controllerFilter != null && !controllerFilter.equals(analytics.url.getHost())) return tablePrinters;
        if( applicationFilter != null && !applicationFilter.equals(analytics.APIAccountName)) return tablePrinters;
        for( String tableName : tableNames ) {
            try (Connection conn = getConnection(); Statement statement = conn.createStatement();) {
                String query = String.format(" select min(starttimestamp) as minTime, max(endtimestamp) as maxTime, count(*) as rowSize from %s", tableName);
                ResultSet resultSet = statement.executeQuery(query);
                while (resultSet.next()) {
                    TablePrinter tablePrinter = new TablePrinter();
                    tablePrinter.controller = analytics.url.getHost();
                    tablePrinter.application = analytics.APIAccountName;
                    tablePrinter.name=tableName;
                    tablePrinter.type = "AnalyticsData";
                    java.sql.Date date = resultSet.getDate("minTime");
                    if( date != null ) tablePrinter.oldestRowTimestamp = date.getTime();
                    date = resultSet.getDate("maxTime");
                    if( date != null ) tablePrinter.newestRowTimestamp = date.getTime();
                    tablePrinter.size = resultSet.getLong("rowSize");
                    tablePrinters.add(tablePrinter);
                }
                resultSet.close();
            } catch (SQLException exception) {
                logger.warn("Error calculating statistics for table %s, Exception: %s", tableName, exception.toString());
                TablePrinter tablePrinter = new TablePrinter();
                tablePrinter.controller = analytics.url.getHost();
                tablePrinter.application = analytics.APIAccountName;
                tablePrinter.name=tableName;
                tablePrinter.type = "AnalyticsData";
                tablePrinters.add(tablePrinter);
            }
        }
        return tablePrinters;
    }

    public void executeUpdate(Namespace namespace) throws SQLException {
        StringBuilder query = new StringBuilder();
        for( Object part : namespace.getList("command") ) {
            query.append(part.toString().replace("\\", "")).append(" ");
        }
        Connection conn = getConnection();
        Statement statement = conn.createStatement();
        WorkingStatusThread workingStatusThread = new WorkingStatusThread("Database Execute", query.toString(), logger);
        workingStatusThread.start();
        try {
            statement.execute(query.toString());
        } finally {
            workingStatusThread.cancel();
        }
        statement.close();
        conn.close();
    }

    public void executeQuery(Namespace namespace) throws SQLException {
        StringBuilder query = new StringBuilder();
        for( Object part : namespace.getList("command") ) {
            query.append(part.toString().replace("\\", "")).append(" ");
        }
        Connection conn = getConnection();
        Statement statement = conn.createStatement();
        ResultSet resultSet = null;
        WorkingStatusThread workingStatusThread = new WorkingStatusThread("Database Query", query.toString(), logger);
        workingStatusThread.start();
        try {
            resultSet = statement.executeQuery(query.toString());
        } finally {
            workingStatusThread.cancel();
        }
        List<IPrintable> resultSetPrinters = new ArrayList<>();
        while( resultSet.next() ) {
            resultSetPrinters.add(new ResultSetPrinter(resultSet));
        }
        System.out.println(Printer.print(resultSetPrinters));
        resultSet.close();
        statement.close();
        conn.close();
    }

    public boolean doesTableExist(String tableName, Configuration configuration) {
        boolean wellDoesIt = false;
        try (Connection conn = getConnection(); Statement statement = conn.createStatement();) {
            String query = String.format(" select count(*) from %s", tableName);
            ResultSet resultSet = statement.executeQuery(query);
            if (resultSet.next()) {
                wellDoesIt=true;
            }
            resultSet.close();
        } catch (SQLException exception) {
            wellDoesIt=false;
        }
        return wellDoesIt;
    }

    public long purgeData(String tableName, Boolean purgeOlderData, long purgeTimestamp, Configuration configuration) throws BadCommandException {
        for( Analytics analytics : configuration.getAnalyticsList() )
            for( String analTable : analytics.getAllSearchTables() )
                if( analTable.equalsIgnoreCase(tableName) ) return purgeAnalData( tableName, purgeOlderData, purgeTimestamp);
        for(Controller controller : configuration.getControllerList() )
            for (String controllerTable : controller.getAllApplicationTables() )
                if( controllerTable.equalsIgnoreCase(tableName) ) return purgeControllerData( tableName, purgeOlderData, purgeTimestamp);
        throw new BadCommandException("Not sure how we got this far, but the table no longer exists between confirming it does and purging data");
    }

    private long purgeAnalData(String tableName, Boolean purgeOlderData, long purgeTimestamp) throws BadCommandException {
        String query = String.format("delete from %s where starttimestamp %s ? or endtimestamp %s ?", tableName, ( purgeOlderData?"<":">"), ( purgeOlderData?"<":">"));
        WorkingStatusThread workingStatusThread = new WorkingStatusThread("Database Execute", query, logger);
        try (Connection conn = getConnection(); PreparedStatement statement = conn.prepareStatement(query);) {
            workingStatusThread.start();
            statement.setDate(1, new java.sql.Date(purgeTimestamp) );
            statement.setDate(2, new java.sql.Date(purgeTimestamp));
            return statement.executeLargeUpdate();
        } catch (SQLException e) {
            throw new BadCommandException(String.format("Error trying to purge Analytics Table '%s', Exception: %s",tableName,e));
        } finally {
            workingStatusThread.cancel();
        }
    }

    private long purgeControllerData(String tableName, Boolean purgeOlderData, long purgeTimestamp) throws BadCommandException {
        String query = String.format("delete from %s where starttimeinmillis %s ? ", tableName, ( purgeOlderData?"<":">"));
        WorkingStatusThread workingStatusThread = new WorkingStatusThread("Database Execute", query, logger);
        try (Connection conn = getConnection(); PreparedStatement statement = conn.prepareStatement(query);) {
            workingStatusThread.start();
            statement.setLong(1, purgeTimestamp);
            return statement.executeLargeUpdate();
        } catch (SQLException e) {
            throw new BadCommandException(String.format("Error trying to purge Controller Table '%s', Exception: %s",tableName,e));
        } finally {
            workingStatusThread.cancel();
        }
    }

    public Configuration getConfiguration() {
        return this.configuration;
    }
}
