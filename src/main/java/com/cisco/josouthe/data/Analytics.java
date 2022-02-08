package com.cisco.josouthe.data;

import com.cisco.josouthe.data.analytic.Result;
import com.cisco.josouthe.data.analytic.Search;
import com.cisco.josouthe.database.ControlEntry;
import com.cisco.josouthe.database.ControlTable;
import com.cisco.josouthe.database.Database;
import com.cisco.josouthe.exceptions.ControllerBadStatusException;
import com.cisco.josouthe.util.Utility;
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
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;

/*
curl -X POST "https://analytics.api.appdynamics.com/events/query"
-H"X-Events-API-AccountName:fico-prod_a604fd85-5055-433d-bbaa-5255064880a9"
-H"X-Events-API-Key:4c67a50d-5f8b-406c-a9bb-c1b09358d2fd"
-H"Content-type: application/vnd.appd.events+json;v=2"
-d ' SELECT segments.userData.`BT - getSsBureauName`,
        count(segments.userData.`BT - getSsBureauName`) AS "BCOUNT",
        toFloat(segments.userData.`BT - getSsBureauScore`) AS "BSCORE"
    FROM transactions
        WHERE application = "Originations-OM_4.8-Core-T-Mobile-PROD"
        and transactionName REGEXP "OM-APM-WebServices-Application.*"'

When executed, the following data is returned:

[
{"fields":[
    {"label":"segments.userData.BT - getSsBureauName","field":"segments.userData.BT - getSsBureauName","type":"string"},
    {"label":"BCOUNT","field":"segments.userData.BT - getSsBureauName","type":"integer","aggregation":"count"},
    {"label":"BSCORE","field":"toFloat(segments.userData.BT - getSsBureauScore)","type":"float"}
],
"results":[
    [null,3348079,null],
    ["ID Analytics US Consumer ID Score",85342,null],
    ["IDAnalytics",85342,null],
    ["Lexis Nexis ADL",84982,null],
    ["Lexis Nexis US Consumer Accurint DataLink",84982,null],
    ["TransUnion US Consumer",22685,null],
    ["TransUnionUSConsumer41",22685,null],
    ["LexisNexisRiskView",22117,null],
    ["Lexis Nexis US Consumer Risk View Thick",16613,null],
    ["Equifax US Consumer",6055,null]
],
"moreData":true,
"schema":"biz_txn_v1"
}
]

 */
public class Analytics {
    private static final Logger logger = LogManager.getFormatterLogger();

    public String APIAccountName, APIKey, tableNamePrefix="AppDynamics_Analytics_";
    public URL url;
    public boolean ignoreNullsInFirstColumnOfReturnedData = true;
    private Database database;
    ArrayList<Search> searches = new ArrayList<>();
    HttpClient client = null;
    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    ControlTable controlTable = null;
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
        this.client = HttpClientBuilder
                .create()
                .useSystemProperties()
                .setConnectionManager(new PoolingHttpClientConnectionManager())
                .setConnectionManagerShared(true)
                .build();
    }

    public Analytics(String urlString, String accountName, String apiKey, String tableNamePrefix, Database database, ArrayList<Search> searches) throws MalformedURLException{
        this(urlString, accountName, apiKey, tableNamePrefix, database);
        this.searches=searches;
    }

    public void setControlTable(ControlTable controlTable) {
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
            if( searchResults != null ) {
                for (Result result : searchResults)
                    if (result != null) results.add(result);
            }
            //serviceEndPoint.end();
        }
        if( dataToInsertLinkedBlockingQueue != null ) {
            dataToInsertLinkedBlockingQueue.add(results.toArray( new Result[0]));
            results.clear();
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
            try{
                json = this.client.execute(request, this.responseHandler);
                succeeded=true;
            } catch (ControllerBadStatusException controllerBadStatusException) {
                tries++;
                logger.warn("Error on try %d while trying to get Analytics Query Results for %s Error: %s", tries, query, controllerBadStatusException.getMessage());
            } catch (IOException ioException) {
                tries++;
                logger.warn("IOException: %s",ioException.getMessage());
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

    public static void main( String... args ) throws MalformedURLException {
        Analytics analytics = new Analytics("https://analytics.api.appdynamics.com/", "southerland-test_65322e21-efed-4126-8827-920141a9ac21", "ae7a6973-dd00-4ebb-8bff-b9404d21bb74", null, null);
        Result[] results = analytics.runAnalyticsQuery("UniqueTransactionCount","SELECT transactionName, count(*) FROM transactions");
        for( Result result : results)
            logger.info(result);

        //SELECT transactionName, eventTimestamp, application, segments.tier FROM transactions
        results = analytics.runAnalyticsQuery("TransactionDetails","SELECT transactionName, eventTimestamp, application, segments.tier, count(*) FROM transactions");
        for( Result result : results)
            logger.info(result);
    }

}
