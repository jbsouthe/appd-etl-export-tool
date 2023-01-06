package com.cisco.josouthe.data;

import com.cisco.josouthe.data.analytic.Search;
import com.cisco.josouthe.data.auth.AccessToken;
import com.cisco.josouthe.data.event.EventData;
import com.cisco.josouthe.data.metric.Baseline;
import com.cisco.josouthe.data.metric.BaselineData;
import com.cisco.josouthe.data.metric.MetricData;
import com.cisco.josouthe.data.model.Model;
import com.cisco.josouthe.data.model.Node;
import com.cisco.josouthe.data.model.Tier;
import com.cisco.josouthe.data.model.TreeNode;
import com.cisco.josouthe.database.ControlEntry;
import com.cisco.josouthe.database.IControlTable;
import com.cisco.josouthe.exceptions.ControllerBadStatusException;
import com.cisco.josouthe.util.HttpClientFactory;
import com.cisco.josouthe.util.Utility;
import com.cisco.josouthe.util.WorkingStatusThread;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import org.apache.commons.codec.Charsets;
import org.apache.http.*;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;

public class Controller {
    private static final Logger logger = LogManager.getFormatterLogger();

    public String hostname;
    public URL url;
    private String clientId, clientSecret;
    private AccessToken accessToken = null;
    public Application[] applications = null;
    public ApplicationRegex[] applicationRegexes = null;
    public Model controllerModel = null;
    private IControlTable controlTable = null;
    private boolean getAllAnalyticsSearchesFlag = false;
    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    HttpClient client = null;
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

    public Controller( String urlString, String clientId, String clientSecret, Application[] applications, boolean getAllAnalyticsSearchesFlag, ApplicationRegex[] applicationRegexes ) throws MalformedURLException {
        if( !urlString.endsWith("/") ) urlString+="/"; //this simplifies some stuff downstream
        this.url = new URL(urlString);
        this.hostname = this.url.getHost();
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.applications = applications;
        this.getAllAnalyticsSearchesFlag=getAllAnalyticsSearchesFlag;
        this.client = HttpClientFactory.getHttpClient();
        this.applicationRegexes = applicationRegexes;
        if( this.applicationRegexes != null && this.applicationRegexes.length > 0 ) {
            initApplicationIdMap();
            List<Application> applicationsToAdd = new ArrayList<>();
            MAIN: for( String appName : _applicationIdMap.keySet() ){
                if( isApplicationInList(appName) ) continue MAIN;
                for( ApplicationRegex applicationRegex : this.applicationRegexes ) {
                    Application application = applicationRegex.getApplicationIfMatches(appName);
                    if( application != null ) {
                        applicationsToAdd.add(application);
                        continue MAIN;
                    }
                }
            }
            if(!applicationsToAdd.isEmpty()) {
                if( this.applications != null )
                    for( Application application : this.applications) {
                        applicationsToAdd.add(application);
                    }
                this.applications = applicationsToAdd.toArray( new Application[0] );
            }
        }
    }

    public boolean isApplicationInList( String name ) {
        if( this.applications == null ) return false;
        for( Application application : this.applications ) {
            if( name.equalsIgnoreCase(application.name) ) return true;
        }
        return false;
    }

    public boolean isGetAllAnalyticsSearchesFlag() { return getAllAnalyticsSearchesFlag; }

    public void setControlTable( IControlTable table ) { this.controlTable=table; }

    public String getBearerToken() {
        if( isAccessTokenExpired() && !refreshAccessToken()) return null;
        return "Bearer "+ accessToken.access_token;
    }

    private boolean isAccessTokenExpired() {
        long now = new Date().getTime();
        if( accessToken == null || accessToken.expires_at < now ) return true;
        return false;
    }

    private boolean refreshAccessToken() { //returns true on successful refresh, false if an error occurs
        CredentialsProvider provider = new BasicCredentialsProvider();
        UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(clientId, clientSecret);
        logger.trace("credentials configured: %s",credentials.toString());
        provider.setCredentials(AuthScope.ANY, credentials);
        logger.trace("provider configured: %s",provider.toString());
        HttpPost request = new HttpPost(url.toString()+"/controller/api/oauth/access_token");
        //request.addHeader(HttpHeaders.CONTENT_TYPE,"application/vnd.appd.cntrl+protobuf;v=1");
        ArrayList<NameValuePair> postParameters = new ArrayList<NameValuePair>();
        postParameters.add( new BasicNameValuePair("grant_type","client_credentials"));
        postParameters.add( new BasicNameValuePair("client_id",clientId));
        postParameters.add( new BasicNameValuePair("client_secret",clientSecret));
        try {
            request.setEntity(new UrlEncodedFormEntity(postParameters,"UTF-8"));
        } catch (UnsupportedEncodingException e) {
            logger.warn("Unsupported Encoding Exception in post parameter encoding: %s",e.getMessage());
        }

        if( logger.isTraceEnabled()){
            logger.trace("Request to run: %s",request.toString());
            for( Header header : request.getAllHeaders())
                logger.trace("with header: %s",header.toString());
        }

        HttpResponse response = null;
        int tries=0;
        boolean succeeded=false;
        while( !succeeded && tries < 3 ) {
            try {
                response = client.execute(request);
                succeeded=true;
                logger.trace("Response Status Line: %s", response.getStatusLine());
            } catch (IOException e) {
                logger.error("Exception in attempting to get access token, Exception: %s", e.getMessage());
                tries++;
            } catch (java.lang.IllegalStateException illegalStateException) {
                tries++;
                this.client = HttpClientFactory.getHttpClient(true);
                logger.warn("Caught exception on connection, building a new connection for retry, Exception: %s", illegalStateException.getMessage());
            }
        }
        if( !succeeded ) return false;
        HttpEntity entity = response.getEntity();
        Header encodingHeader = entity.getContentEncoding();
        Charset encoding = encodingHeader == null ? StandardCharsets.UTF_8 : Charsets.toCharset(encodingHeader.getValue());
        String json = null;
        try {
            json = EntityUtils.toString(entity, StandardCharsets.UTF_8);
            logger.trace("JSON returned: %s",json);
        } catch (IOException e) {
            logger.warn("IOException parsing returned encoded string to json text: "+ e.getMessage());
            return false;
        }
        if( response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
            logger.warn("Access Key retreival returned bad status: %s message: %s", response.getStatusLine(), json);
            return false;
        }
        this.accessToken = gson.fromJson(json, AccessToken.class); //if this doesn't work consider creating a custom instance creator
        this.accessToken.expires_at = new Date().getTime() + (accessToken.expires_in*1000); //hoping this is enough, worry is the time difference
        return true;
    }

    public MetricData[] getMetricValue(Application application, String metricName, long startTimestamp, long endTimestamp ) {
        MetricData[] metrics = null;

        int tries=0;
        boolean succeeded = false;
        while (! succeeded && tries < 3 ) {
            try {
                metrics = getMetricValue(String.format("%scontroller/rest/applications/%s/metric-data?metric-path=%s&time-range-type=BETWEEN_TIMES&start-time=%d&end-time=%d&output=JSON&rollup=false",
                        this.url, Utility.encode(application.name), Utility.encode(metricName), startTimestamp, endTimestamp)
                );
                succeeded=true;
            } catch (ControllerBadStatusException controllerBadStatusException) {
                tries++;
                logger.warn("Attempt number %d failed, status returned: %s for request %s",tries,controllerBadStatusException.getMessage(), controllerBadStatusException.urlRequestString);
            }
        }
        if( !succeeded)
            logger.warn("Gave up after %d tries, not getting %s back", tries, metricName);
        return metrics;
    }

    public MetricData[] getMetricValue( String urlString ) throws ControllerBadStatusException {
        MetricData[] metricData = null;
        if( urlString == null ) return null;
        logger.trace("metric url: %s",urlString);
        if( ! urlString.contains("output=JSON") ) urlString += "&output=JSON";
        HttpGet request = new HttpGet(urlString);
        request.addHeader(HttpHeaders.AUTHORIZATION, getBearerToken());
        logger.trace("HTTP Method: %s",request);
        String json = null;
        try {
            json = client.execute(request, this.responseHandler);
        } catch (ControllerBadStatusException controllerBadStatusException) {
            controllerBadStatusException.setURL(urlString);
            throw controllerBadStatusException;
        } catch (IOException e) {
            logger.error("Exception in attempting to get url, Exception: %s", e.getMessage());
            return null;
        }
        metricData = gson.fromJson(json, MetricData[].class);
        return metricData;
    }

    public TreeNode[] getApplicationMetricFolders(Application application, String path) {
        String json = null;

        int tries=0;
        boolean succeeded=false;
        while( !succeeded && tries < 3 ) {
            try {
                if ("".equals(path)) {
                    json = getRequest(String.format("controller/rest/applications/%s/metrics?output=JSON", Utility.encode(application.name)));
                } else {
                    json = getRequest(String.format("controller/rest/applications/%s/metrics?metric-path=%s&output=JSON", Utility.encode(application.name), Utility.encode(path)));
                }
                succeeded=true;
            } catch (ControllerBadStatusException controllerBadStatusException) {
                tries++;
                logger.warn("Try %d failed for request to get app application metric folders for %s with error: %s",tries,application.name,controllerBadStatusException.getMessage());
            }
        }
        if(!succeeded) logger.warn("Failing on get of application metric folder, controller may be down");

        TreeNode[] treeNodes = null;
        try {
            treeNodes = gson.fromJson(json, TreeNode[].class);
        } catch (JsonSyntaxException jsonSyntaxException) {
            logger.warn("Error in parsing returned text, this may be a bug JSON '%s' Exception: %s",json, jsonSyntaxException.getMessage());
        }
        return treeNodes;
    }

    public MetricData[] getAllMetricsForAllApplications() {
        ArrayList<MetricData> metrics = new ArrayList<>();
        for( Application application : this.applications ) {
            metrics.addAll(getAllMetrics(application, null));
        }
        return metrics.toArray( new MetricData[0] );
    }

    public ArrayList<MetricData> getAllMetrics( Application application, LinkedBlockingQueue<Object[]> dataQueue ) {
        ArrayList<MetricData> metrics = new ArrayList<>();
        //Transaction serviceEndPoint = AppdynamicsAgent.startTransactionAndServiceEndPoint("Get Application Metrics", null, "Get Application Metrics", EntryTypes.POJO, false);
        ControlEntry controlEntry = this.controlTable.getLastRunTimestamp(hostname, application.name, "MetricData" );
            /*
            serviceEndPoint.collectData("Controller", controlEntry.controller, Utility.getSnapshotDatascope());
            serviceEndPoint.collectData("Application", controlEntry.application, Utility.getSnapshotDatascope());
            serviceEndPoint.collectData("Datatype", controlEntry.type, Utility.getSnapshotDatascope());
            serviceEndPoint.collectData("Start-Timestamp", String.valueOf(controlEntry.timestamp), Utility.getSnapshotDatascope());
             */
        long startTimestamp = controlEntry.timestamp;
        long endTimestamp = Utility.now();
        WorkingStatusThread workingStatusThread = new WorkingStatusThread("Get Controller Metrics", application.name, logger);
        workingStatusThread.start();
        for( String applicationMetricName : application.metricGraph.getUniqueCompressedMetricNames() ) {
            for( MetricData metricData : getMetricValue( application, applicationMetricName, startTimestamp, endTimestamp )) {
                if( "METRIC DATA NOT FOUND".equals(metricData.metricName) ) continue;
                metricData.controllerHostname = this.hostname;
                metricData.applicationName = application.name;
                metricData.targetTable = application.defaultMetricTableName;
                metrics.add(metricData);
                getBaselineValue( metricData, application, startTimestamp, endTimestamp, dataQueue);
            }
            if( dataQueue != null ) {
                dataQueue.add(metrics.toArray(new MetricData[0]));
                metrics.clear();
            }
        }
        workingStatusThread.cancel();
        controlEntry.timestamp = endTimestamp;
        //serviceEndPoint.collectData("End-Timestamp", String.valueOf(endTimestamp), Utility.getSnapshotDatascope());
        //serviceEndPoint.end();
        this.controlTable.setLastRunTimestamp(controlEntry);
        return metrics;
    }

    public List<BaselineData> getBaselineValue( MetricData metricData, Application application, long startTimestamp, long endTimestamp, LinkedBlockingQueue<Object[]> dataQueue ) {
        ArrayList<BaselineData> baselines = new ArrayList<>();
        if( application.baselines == null || application.baselines.size() == 0 ) {
            logger.warn("No baselines found for app %s(%d)", application.getName(), application.id);
            return baselines;
        }
        boolean succeeded=false;
        int tries=0;
        String json = "";
        while( !succeeded && tries < 3 ) {
            tries++;
            try {
                json = postRequest(
                        "controller/restui/metricBrowser/getMetricBaselineData?granularityMinutes=1",
                        String.format("{\"metricDataQueries\":[{\"metricId\":%d,\"entityId\":%d,\"entityType\":\"APPLICATION\"}],\"timeRangeSpecifier\":{\"type\":\"BETWEEN_TIMES\",\"durationInMinutes\":null,\"endTime\":%d,\"startTime\":%d,\"timeRange\":null,\"timeRangeAdjusted\":false},\"metricBaseline\":%d,\"maxSize\":1440}",
                                metricData.metricId, application.id, endTimestamp, startTimestamp, application.getBaseline().id));
                succeeded=true;
            } catch (ControllerBadStatusException controllerBadStatusException) {
                logger.warn("Error in request to pull baseline metrics using an undocumented, internal, api. This is retriable, attempt %d Error: %s", tries, controllerBadStatusException.getMessage());
            }
        }
        if( !succeeded ) {
            logger.error("Giving up on attempt to get Baseline metrics, the controller isn't responding properly");
            return null;
        }
        long totalPurgeCount=0;
        for( BaselineData baselineData : gson.fromJson(json, BaselineData[].class)) {
            baselineData.metricName = metricData.metricName; //this is blank on my test data, not sure why it isn't set
            baselineData.controllerHostname = this.hostname;
            baselineData.applicationName = application.name;
            baselineData.targetTable = application.defaultBaselineTableName;
            baselineData.baseline = application.getBaseline();
            long purgeCount = baselineData.purgeNullBaselineTimeslices();
            if( purgeCount > 0) logger.trace("Purged %d Baselines that contained no data",purgeCount);
            totalPurgeCount += purgeCount;
            if( baselineData.hasData() )
                baselines.add(baselineData);
        }
        logger.debug("Purged a total of %d baseline datasets that were empty");
        if( dataQueue != null && !baselines.isEmpty() ) {
            dataQueue.add( baselines.toArray( new BaselineData[0]));
            baselines.clear();
        }
        return baselines;
    }

    public EventData[] getAllEventsForAllApplications() {
        ArrayList<EventData> events = new ArrayList<>();
        for( Application application : this.applications ) {
            events.addAll( getAllEvents(application, null) );
        }
        return events.toArray( new EventData[0]);
    }

    public ArrayList<EventData> getAllEvents( Application application, LinkedBlockingQueue<Object[]> dataQueue ) {
        ArrayList<EventData> events = new ArrayList<>();
        //Transaction serviceEndPoint = AppdynamicsAgent.startTransactionAndServiceEndPoint("Get Application Events", null, "Get Application Events", EntryTypes.POJO, false);
        ControlEntry controlEntry = this.controlTable.getLastRunTimestamp(hostname, application.name, "EventData" );
            /* serviceEndPoint.collectData("Controller", controlEntry.controller, Utility.getSnapshotDatascope());
            serviceEndPoint.collectData("Application", controlEntry.application, Utility.getSnapshotDatascope());
            serviceEndPoint.collectData("Datatype", controlEntry.type, Utility.getSnapshotDatascope());
            serviceEndPoint.collectData("Start-Timestamp", String.valueOf(controlEntry.timestamp), Utility.getSnapshotDatascope());

             */
        long startTimestamp = controlEntry.timestamp;
        long endTimestamp = Utility.now();
        if( application.getAllEvents ) {
            int tries=0;
            boolean succeeded=false;
            String json = null;
            while( !succeeded && tries < 3 ) {
                WorkingStatusThread workingStatusThread = new WorkingStatusThread("Controller Events", application.name, logger);
                workingStatusThread.start();
                try {
                    json = getRequest("controller/rest/applications/%s/events?time-range-type=BETWEEN_TIMES&start-time=%d&end-time=%d&event-types=%s&severities=%s&output=JSON",
                            Utility.encode(application.name), startTimestamp, endTimestamp, application.eventTypeList, application.eventSeverities);
                    succeeded=true;
                } catch (ControllerBadStatusException controllerBadStatusException) {
                    tries++;
                    logger.warn("Error on try %d while trying to get Events for application %s Error: %s", tries, application.name, controllerBadStatusException.getMessage());
                } finally {
                   workingStatusThread.cancel();
                }
            }
            if( !succeeded ) {
                logger.error("Too many errors trying to get events from the controller, giving up for this scheduled run!");
                return null;
            }
            EventData[] eventsReturned = gson.fromJson(json, EventData[].class);
            if( eventsReturned != null ) {
                for (EventData event : eventsReturned) {
                    event.controllerHostname = this.hostname;
                    event.applicationName = application.name;
                    event.targetTable = application.defaultEventTableName;
                    events.add(event);
                }
                if (dataQueue != null) {
                    dataQueue.add(events.toArray(new EventData[0]));
                    events.clear();
                }
            }
        }
        controlEntry.timestamp = endTimestamp;
        //serviceEndPoint.collectData("End-Timestamp", String.valueOf(endTimestamp), Utility.getSnapshotDatascope());
        //serviceEndPoint.end();
        this.controlTable.setLastRunTimestamp(controlEntry);
        return events;
    }

    private String postRequest( String requestUri, String body ) throws ControllerBadStatusException {
        HttpPost request = new HttpPost(String.format("%s%s", this.url.toString(), requestUri));
        request.addHeader(HttpHeaders.AUTHORIZATION, getBearerToken());
        logger.trace("HTTP Method: %s with body: '%s'",request, body);
        String json = null;
        try {
            request.setEntity( new StringEntity(body, "UTF8"));
            request.setHeader("Accept", "application/json");
            request.setHeader("Content-Type", "application/json");
            json = client.execute( request, this.responseHandler);
            logger.trace("Data Returned: '%s'", json);
        } catch (ControllerBadStatusException controllerBadStatusException) {
            controllerBadStatusException.setURL(request.getURI().toString());
            throw controllerBadStatusException;
        } catch (IOException e) {
            logger.warn("Exception: %s",e.getMessage());
        }
        return json;
    }

    private String getRequest( String formatOrURI, Object... args ) throws ControllerBadStatusException {
        if( args == null || args.length == 0 ) return getRequest(formatOrURI);
        return getRequest( String.format(formatOrURI,args));
    }

    private String getRequest( String uri ) throws ControllerBadStatusException {
        HttpGet request = new HttpGet(String.format("%s%s", this.url.toString(), uri));
        request.addHeader(HttpHeaders.AUTHORIZATION, getBearerToken());
        logger.trace("HTTP Method: %s",request);
        String json = null;
        try {
            json = client.execute(request, this.responseHandler);
            logger.trace("Data Returned: '%s'",json);
        } catch (ControllerBadStatusException controllerBadStatusException) {
            controllerBadStatusException.setURL(request.getURI().toString());
            throw controllerBadStatusException;
        } catch (IOException e) {
            logger.warn("Exception: %s",e.getMessage());
        }
        return json;
    }

    Map<String,Long> _applicationIdMap = null;
    public long getApplicationId( String name ) {
        logger.trace("Get Application id for %s",name);
        if( _applicationIdMap == null ) { //go get em
            initApplicationIdMap();
        }
        if( !_applicationIdMap.containsKey(name) ) return -1;
        return _applicationIdMap.get(name);
    }

    private void initApplicationIdMap() {
        try {
            String json = getRequest("controller/restui/applicationManagerUiBean/getApplicationsAllTypes?output=json");
            com.cisco.josouthe.data.model.ApplicationListing applicationListing = gson.fromJson(json, com.cisco.josouthe.data.model.ApplicationListing.class);
            _applicationIdMap = new HashMap<>();
            for (com.cisco.josouthe.data.model.Application app : applicationListing.getApplications() )
                if( app.active ) _applicationIdMap.put(app.name, app.id);
        } catch (ControllerBadStatusException controllerBadStatusException) {
            logger.warn("Giving up on getting application id, not even going to retry");
        }
    }

    public Model getModel() {
        if( this.controllerModel == null ) {
            try {
                String json = getRequest("controller/rest/applications?output=json");
                this.controllerModel = new Model(gson.fromJson(json, com.cisco.josouthe.data.model.Application[].class));
                for (com.cisco.josouthe.data.model.Application application : this.controllerModel.getApplications()) {
                    json = getRequest("controller/rest/applications/%d/tiers?output=json", application.id);
                    application.tiers = gson.fromJson(json, Tier[].class);
                    json = getRequest("controller/rest/applications/%d/nodes?output=json", application.id);
                    application.nodes = gson.fromJson(json, Node[].class);
                }
            } catch (ControllerBadStatusException controllerBadStatusException) {
                logger.warn("Giving up on getting controller model, not even going to retry");
            }
        }
        return this.controllerModel;
    }

    public Search[] getAllSavedSearchesFromController() { //yup, using an undocumented api of the ui ;)
        try {
            String json = getRequest("controller/restui/analyticsSavedSearches/getAllAnalyticsSavedSearches?output=json");
            return gson.fromJson(json, Search[].class);
        } catch (ControllerBadStatusException controllerBadStatusException) {
            logger.warn("Error using undocumented api to pull back listing of all saved analytics searches");
        }
        return null;
    }

    public Baseline[] getAllBaselines( Application application ) {
        if( application == null ) return null;
        if( application.id == -1 ) application.id = getApplicationId(application.getName());
        if( application.id == -1 ) return null;
        return getAllBaselines(application.id);
    }

    public Baseline[] getAllBaselines(long applicationId ) {
        try {
            String json = getRequest("controller/restui/baselines/getAllBaselines/%d?output=json", applicationId);
            Baseline[] baselines = gson.fromJson(json, Baseline[].class);
            return baselines;
        } catch (ControllerBadStatusException controllerBadStatusException) {
            logger.warn("Error using undocumented api to pull back listing of all application baselines, application '%s'", applicationId);
        }
        return null;
    }

    public void discardToken() {
        this.accessToken=null;
    }
}
