package com.cisco.josouthe.data;

import com.cisco.josouthe.data.analytic.Result;
import com.cisco.josouthe.data.analytic.Search;
import com.cisco.josouthe.database.ControlEntry;
import com.cisco.josouthe.database.Database;
import com.cisco.josouthe.database.IControlTable;
import com.cisco.josouthe.exceptions.ControllerBadStatusException;
import com.cisco.josouthe.http.HttpClientFactory;
import com.cisco.josouthe.util.Utility;
import com.cisco.josouthe.http.WorkingStatusThread;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

public class Analytics {
    private static final Logger logger = LogManager.getFormatterLogger();

    public String APIAccountName, APIKey, tableNamePrefix="AppDynamics_Analytics_";
    public URL url;
    private Database database;
    private int minutesToAdjustEndTimestampBy = 5;
    ArrayList<Search> searches = new ArrayList<>();
    HttpClient client = null;
    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    IControlTable controlTable = null;
    private ResponseHandler<String> responseHandler;

    public Analytics( String urlString, String APIAccountName, String APIKey, String tableNamePrefix, Database database ) throws MalformedURLException {
        if( !urlString.endsWith("/") ) urlString+="/";
        this.url = new URL(urlString);
        this.APIAccountName = APIAccountName;
        this.APIKey = APIKey;
        if( tableNamePrefix != null )
            this.tableNamePrefix=tableNamePrefix;
        this.database = database;
        this.client = HttpClientFactory.getHttpClient();
        this.responseHandler = HttpClientFactory.getStringResponseHandler("analytics");
    }

    public Analytics(String urlString, String accountName, String apiKey, String tableNamePrefix, Database database, ArrayList<Search> searches, int minutesToAdjustEndTimestampBy ) throws MalformedURLException{
        this(urlString, accountName, apiKey, tableNamePrefix, database);
        this.searches=searches;
        this.minutesToAdjustEndTimestampBy=minutesToAdjustEndTimestampBy;
    }

    public void setControlTable(IControlTable controlTable) {
        this.controlTable=controlTable;
    }

    public List<String> getAllSearchTables() {
        List<String> tableNames = new ArrayList<>();
        for( Search search : searches )
            tableNames.add( database.convertToAcceptableTableName( this.tableNamePrefix + search.searchName ) );
        return tableNames;
    }

    public Result[] getAllSearches() {
        return getAllSearches(null);
    }

    public Result[] getAllSearches(LinkedBlockingQueue<Object[]> dataToInsertLinkedBlockingQueue) {
        ArrayList<Result> results = new ArrayList<>();
        ControlEntry controlEntry = this.controlTable.getLastRunTimestamp(url.getHost(), this.APIAccountName, "AnalyticsData");
        long startTimestamp = controlEntry.timestamp;
        long endTimestamp = Utility.now(this.minutesToAdjustEndTimestampBy*-60000 ); //going to try setting the end time to now()-5 minutes to see if this is enough to allow the backend time to finish collecting all data for a period
        if( database.getConfiguration().isTooLongATime( endTimestamp - startTimestamp ) )
            endTimestamp = startTimestamp + database.getConfiguration().getMaxQueryDurationInMS();
        logger.trace("Adjustment, if enabled: minutes %d (negated)mil: %d", this.minutesToAdjustEndTimestampBy,this.minutesToAdjustEndTimestampBy*-60000 );
        if( endTimestamp <= startTimestamp ) {
            logger.warn("While trying to set the end timestamp to 5 minutes before now(), we have reached a situation where the end time is less than or equal to the start time, this means we are going to skip this run");
            return null;
        }
        for( Search search : searches ) {
            //Transaction serviceEndPoint = AppdynamicsAgent.startTransactionAndServiceEndPoint("Analytics Search", null, "Analytics Search "+ search.name, EntryTypes.POJO, false);
            //serviceEndPoint.collectData("Search Query", search.query, Utility.getSnapshotDatascope());
            logger.info("Running Analytics Search %s query: '%s'", search.getName(), search.getQuery());
            Result[] searchResults = runAnalyticsQuery(search, startTimestamp, endTimestamp, dataToInsertLinkedBlockingQueue);
            //serviceEndPoint.end();
            if( dataToInsertLinkedBlockingQueue == null && searchResults != null ) {
                for( Result result : searchResults )
                    results.add(result);
            }
        }
        controlEntry.timestamp = endTimestamp;
        this.controlTable.setLastRunTimestamp(controlEntry);
        return results.toArray(new Result[0]);
    }

    public Result[] runAnalyticsQuery(Search search) {
        return runAnalyticsQuery(search.getName(), search.getQuery(), Utility.now(), Utility.now(this.minutesToAdjustEndTimestampBy*-60000), search.limit, null);
    }

    public Result[] runAnalyticsQuery(Search search, long startTimestamp, long endTimestamp, LinkedBlockingQueue<Object[]> dataToInsertLinkedBlockingQueue) {
        return runAnalyticsQuery(search.getName(), search.getQuery(), startTimestamp, endTimestamp, search.limit, dataToInsertLinkedBlockingQueue);
    }

    public Result[] runAnalyticsQuery(String name, String query) {
        return runAnalyticsQuery(name, query, Utility.now(), Utility.now(-3600000), 10000, null);
    }

    public Result[] runAnalyticsQuery(String name, String query, long startTimestamp, long endTimestamp, int limit, LinkedBlockingQueue<Object[]> dataToInsertLinkedBlockingQueue ) {
        if( query == null || startTimestamp < 1 ) return null;
        HttpPost request = new HttpPost( String.format("%sevents/query?start=%s&end=%s&limit=%d", this.url.toString(), Utility.getEncodedDateString(startTimestamp), Utility.getEncodedDateString(endTimestamp), limit));
        request.addHeader("X-Events-API-AccountName", this.APIAccountName);
        request.addHeader("X-Events-API-Key", this.APIKey);
        request.addHeader("Content-type","application/vnd.appd.events+json;v=2");
        request.addHeader("Accept","application/vnd.appd.events+json;v=2");
        try {
            request.setEntity(new StringEntity(query, "UTF-8"));
        } catch (Exception e) {
            logger.error("Query could not be encoded in the body of the request: '%s' Exception: %s",query,e.getMessage());
            return null;
        }
        logger.trace("Request: %s with query: %s", request.toString(), query);
        if( HttpClientFactory.isWireTraceEnabled("analytics") ) {
            logger.info("Wire Trace Request: '%s' with Body: '%s'",request.toString(), query);
        }
        int tries=0;
        boolean succeeded=false;
        String json = "";
        while (!succeeded && tries < 3) {
            WorkingStatusThread workingStatusThread = new WorkingStatusThread(String.format("Analytics Query %s",name), query, logger);
            workingStatusThread.start();
            try{
                json = this.client.execute(request, this.responseHandler);
                succeeded=true;
            } catch (ControllerBadStatusException controllerBadStatusException) {
                tries++;
                logger.warn("Error on try %d while trying to get Analytics Query Results for %s Error: %s", tries, query, controllerBadStatusException.getMessage());
            } catch (IOException ioException) {
                tries++;
                logger.warn("IOException: %s",ioException.getMessage());
            } finally {
                workingStatusThread.cancel();
            }
        }
        if( !succeeded ) {
            logger.error("Could not retrieve Analytic Search Results for '%s' Query: '%s' Message: '%s'", name, query, json);
            return null;
        }
        Result[] results =  gson.fromJson(json, Result[].class);
        if( results != null )
            for( Result result : results ) {
                result.name = name;
                result.query = query;
                if( database == null ) {
                    result.targetTable = this.tableNamePrefix + name;
                } else {
                    result.targetTable = database.convertToAcceptableTableName(this.tableNamePrefix + name);
                }
                result.startTimestamp = startTimestamp;
                result.endTimestamp = endTimestamp;
                if( result.isError() ) {
                    logger.error("Query %s '%s' had Error fetching search results: %s", name, query, result.error);
                    return null;
                }
                if( result.isMoreDataSet() ) {
                    //we need to run again with scroll method
                    logger.info("Going to have to try again with scroll for query %s", name);
                    runAnalyticsQueryWithScroll(name, query, startTimestamp, endTimestamp, new ArrayList<>(), null, dataToInsertLinkedBlockingQueue);
                    return null;
                }
            }

        if( dataToInsertLinkedBlockingQueue != null ) {
            dataToInsertLinkedBlockingQueue.add(results);
            return null;
        } else {
            return results;
        }
    }

    //sometimes: https://docs.appdynamics.com/appd/22.x/latest/en/extend-appdynamics/appdynamics-apis/analytics-events-api#FirstRequest
    private void runAnalyticsQueryWithScroll(String name, String query, long startTimestamp, long endTimestamp, List<Result> resultList, String scrollId, LinkedBlockingQueue<Object[]> dataToInsertLinkedBlockingQueue ) {
        if( query == null || startTimestamp < 1 ) return ;
        logger.trace("Search '%s' ScrollId: %s", name, scrollId);
        HttpPost request = new HttpPost( String.format("%sevents/query?start=%s&end=%s", this.url.toString(), Utility.getEncodedDateString(startTimestamp), Utility.getEncodedDateString(endTimestamp)));
        request.addHeader("X-Events-API-AccountName", this.APIAccountName);
        request.addHeader("X-Events-API-Key", this.APIKey);
        request.addHeader("Content-type","application/vnd.appd.events+json;v=2");
        request.addHeader("Accept","application/vnd.appd.events+json;v=2");
        StringBuilder stringBuilder = new StringBuilder(String.format("[{\"query\": \"%s\",\"mode\": \"scroll\"", Utility.escapeQuotes(query)));
        if( scrollId != null ) stringBuilder.append(String.format(",\"scrollid\": \"%s\"",scrollId) );
        stringBuilder.append("}]");
        logger.trace("JSON BODY: '%s'", stringBuilder.toString());
        try {
            request.setEntity(new StringEntity(stringBuilder.toString(), "UTF-8"));
        } catch (Exception e) {
            logger.error("Query could not be encoded in the body of the request: '%s' Exception: %s",query,e.getMessage());
            return ;
        }
        logger.trace("Request: %s with query: %s", request.toString(), query);
        if( HttpClientFactory.isWireTraceEnabled("analytics") ) {
            logger.info("Wire Trace Request: '%s' with Body: '%s'",request.toString(), stringBuilder.toString());
        }
        int tries=0;
        boolean succeeded=false;
        String json = "";
        while (!succeeded && tries < 3) {
            WorkingStatusThread workingStatusThread = new WorkingStatusThread(String.format("Analytics Query %s",name), query, logger);
            workingStatusThread.start();
            try{
                json = this.client.execute(request, this.responseHandler);
                succeeded=true;
            } catch (ControllerBadStatusException controllerBadStatusException) {
                tries++;
                logger.warn("Error on try %d while trying to get Analytics Query Results for %s Error: %s", tries, query, controllerBadStatusException.getMessage());
            } catch (IOException ioException) {
                tries++;
                logger.warn("IOException: %s",ioException.getMessage());
            } finally {
                workingStatusThread.cancel();
            }
        }
        if( !succeeded ) {
            logger.error("Could not retrieve Analytic Search Results for '%s' Query: '%s' Message: '%s'", name, query, json);
            return ;
        }
        Result[] results =  gson.fromJson(json, Result[].class);
        if( results != null ) {
            for( Result result : results ) {
                result.name = name;
                result.query = query;
                if (database == null) {
                    result.targetTable = this.tableNamePrefix + name;
                } else {
                    result.targetTable = database.convertToAcceptableTableName(this.tableNamePrefix + name);
                }
                result.startTimestamp = startTimestamp;
                result.endTimestamp = endTimestamp;
                if (result.isError()) {
                    logger.error("Query %s '%s' had Error fetching search results: %s", name, query, result.error);
                    return ;
                }
            }
            dataToInsertLinkedBlockingQueue.add(results);
            for( Result result : results ) {
                if( result.isMoreDataSet() ) {
                    runAnalyticsQueryWithScroll(name, query, startTimestamp, endTimestamp, resultList, result.scrollid, dataToInsertLinkedBlockingQueue);
                }
            }
        }
    }

}
