package com.cisco.josouthe.data;

import com.cisco.josouthe.data.analytic.Result;
import com.cisco.josouthe.data.analytic.Search;
import com.cisco.josouthe.database.ControlEntry;
import com.cisco.josouthe.database.IControlTable;
import com.cisco.josouthe.database.Database;
import com.cisco.josouthe.exceptions.ControllerBadStatusException;
import com.cisco.josouthe.util.HttpClientFactory;
import com.cisco.josouthe.util.Utility;
import com.cisco.josouthe.util.WorkingStatusThread;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;

public class Analytics {
    private static final Logger logger = LogManager.getFormatterLogger();

    public String APIAccountName, APIKey, tableNamePrefix="AppDynamics_Analytics_";
    public URL url;
    public boolean ignoreNullsInFirstColumnOfReturnedData = true;
    private Database database;
    ArrayList<Search> searches = new ArrayList<>();
    HttpClient client = null;
    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    IControlTable controlTable = null;
    final ResponseHandler<String> responseHandler = new ResponseHandler<String>() {
        private String uri = "Unset";
        public void setUri( String uri ) { this.uri=uri; }

        @Override
        public String handleResponse( final HttpResponse response) throws IOException {
            final int status = response.getStatusLine().getStatusCode();
            if (status >= HttpStatus.SC_OK && status < HttpStatus.SC_TEMPORARY_REDIRECT) {
                final HttpEntity entity = response.getEntity();
                try {
                    return entity != null ? EntityUtils.toString(entity) : null;
                } catch (final ParseException ex) {
                    throw new ClientProtocolException(ex);
                }
            } else {
                throw new ControllerBadStatusException(response.getStatusLine().toString(), EntityUtils.toString(response.getEntity()), uri);
            }
        }

    };

    public Analytics( String urlString, String APIAccountName, String APIKey, String tableNamePrefix, Database database ) throws MalformedURLException {
        if( !urlString.endsWith("/") ) urlString+="/";
        this.url = new URL(urlString);
        this.APIAccountName = APIAccountName;
        this.APIKey = APIKey;
        if( tableNamePrefix != null )
            this.tableNamePrefix=tableNamePrefix;
        this.database = database;
        this.client = HttpClientFactory.getHttpClient();
    }

    public Analytics(String urlString, String accountName, String apiKey, String tableNamePrefix, Database database, ArrayList<Search> searches) throws MalformedURLException{
        this(urlString, accountName, apiKey, tableNamePrefix, database);
        this.searches=searches;
    }

    public void setControlTable(IControlTable controlTable) {
        this.controlTable=controlTable;
    }

    public Result[] getAllSearches() {
        return getAllSearches(null);
    }

    public Result[] getAllSearches(LinkedBlockingQueue<Object[]> dataToInsertLinkedBlockingQueue) {
        ArrayList<Result> results = new ArrayList<>();
        ControlEntry controlEntry = this.controlTable.getLastRunTimestamp(url.getHost(), this.APIAccountName, "AnalyticsData");
        long startTimestamp = controlEntry.timestamp;
        long endTimestamp = Utility.now();
        for( Search search : searches ) {
            //Transaction serviceEndPoint = AppdynamicsAgent.startTransactionAndServiceEndPoint("Analytics Search", null, "Analytics Search "+ search.name, EntryTypes.POJO, false);
            //serviceEndPoint.collectData("Search Query", search.query, Utility.getSnapshotDatascope());
            logger.info("Running Analytics Search %s query: '%s'", search.getName(), search.getQuery());
            Result[] searchResults = runAnalyticsQuery(search, startTimestamp, endTimestamp);
            //serviceEndPoint.end();
            if( dataToInsertLinkedBlockingQueue != null && searchResults != null ) {
                logger.info("Adding Analytics Search %s data to database queue, elements %d", search.getName(), searchResults.length);
                dataToInsertLinkedBlockingQueue.add(searchResults);
            } else if( searchResults != null ) {
                for( Result result : searchResults )
                    results.add(result);
            }
        }
        controlEntry.timestamp = endTimestamp;
        this.controlTable.setLastRunTimestamp(controlEntry);
        return results.toArray(new Result[0]);
    }

    public Result[] runAnalyticsQuery(Search search) {
        return runAnalyticsQuery(search.getName(), search.getQuery(),(Utility.now())-3600000, Utility.now(), search.limit);
    }

    public Result[] runAnalyticsQuery(Search search, long startTimestamp, long endTimestamp) {
        return runAnalyticsQuery(search.getName(), search.getQuery(), startTimestamp, endTimestamp, search.limit);
    }

    public Result[] runAnalyticsQuery(String name, String query) {
        return runAnalyticsQuery(name, query, (Utility.now())-3600000, Utility.now(), 20000);
    }

    public Result[] runAnalyticsQuery(String name, String query, long startTimestamp, long endTimestamp, int limit ) {
        if( query == null || startTimestamp < 1 ) return null;
        HttpPost request = new HttpPost( String.format("%sevents/query?start=%s&end=%s&limit=%d", this.url.toString(), Utility.getEncodedDateString(startTimestamp), Utility.getEncodedDateString(endTimestamp), limit));
        request.addHeader("X-Events-API-AccountName", this.APIAccountName);
        request.addHeader("X-Events-API-Key", this.APIKey);
        request.addHeader("Content-type","application/vnd.appd.events+json;v=2");
        try {
            request.setEntity(new StringEntity(query));
        } catch (UnsupportedEncodingException e) {
            logger.error("Query could not be encoded in the body of the request: '%s' Exception: %s",query,e.getMessage());
            return null;
        }
        logger.trace("Request: %s with query: %s", request.toString(), query);
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
            }
        return results;
    }

}
