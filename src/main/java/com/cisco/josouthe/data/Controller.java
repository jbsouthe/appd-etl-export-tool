package com.cisco.josouthe.data;

import com.cisco.josouthe.data.model.Model;
import com.cisco.josouthe.data.model.Node;
import com.cisco.josouthe.data.model.Tier;
import com.cisco.josouthe.util.Utility;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.codec.Charsets;
import org.apache.http.*;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class Controller {
    private static final Logger logger = LogManager.getFormatterLogger();

    public String hostname;
    public URL url;
    private String clientId, clientSecret;
    private AccessToken accessToken = null;
    public Application[] applications = null;
    public Model controllerModel = null;

    public Controller( String urlString, String clientId, String clientSecret, Application[] applications ) throws MalformedURLException {
        if( !urlString.endsWith("/") ) urlString+="/"; //this simplifies some stuff downstream
        this.url = new URL(urlString);
        this.hostname = this.url.getHost();
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.applications = applications;
    }

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
        logger.debug("credentials configured: %s",credentials.toString());
        provider.setCredentials(AuthScope.ANY, credentials);
        logger.debug("provider configured: %s",provider.toString());

        HttpClient client = HttpClientBuilder.create()
                .setDefaultCredentialsProvider(provider)
                .build();

        logger.trace("HttpClient created");

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

        if( logger.isDebugEnabled()){
            logger.debug("Request to run: %s",request.toString());
            for( Header header : request.getAllHeaders())
                logger.debug("with header: %s",header.toString());
        }

        HttpResponse response = null;
        try {
            response = client.execute(request);
            logger.debug("Response Status Line: %s",response.getStatusLine());
        } catch (IOException e) {
            logger.error("Exception in attempting to get access token, Exception: %s",e.getMessage());
            return false;
        }
        if( response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
            logger.warn("Access Key retreival returned bad status: %s", response.getStatusLine());
            return false;
        }
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
        Gson gson = new Gson();
        this.accessToken = gson.fromJson(json, AccessToken.class); //if this doesn't work consider creating a custom instance creator
        this.accessToken.expires_at = new Date().getTime() + (accessToken.expires_in*60000); //hoping this is enough, worry is the time difference
        return true;
    }

    public MetricData[] getMetricValue( Application application, ApplicationMetric metric ) {
        MetricData[] metrics = getMetricValue( String.format("%s/controller/rest/applications/%s/metric-data?metric-path=%s&time-range-type=%s&duration-in-mins=%s&output=JSON&rollup=%s",
                this.url, Utility.encode(application.name), Utility.encode(metric.name),metric.timeRangeType,metric.durationInMins,
                (metric.disableDataRollup.toLowerCase().equals("true") ?"false":"true")
        ));
        return metrics;
    }

    public MetricData[] getMetricValue( String urlString ) {
        MetricData[] metricData = null;
        if( urlString == null ) return null;
        logger.debug("metric url: %s",urlString);
        if( ! urlString.contains("output=JSON") ) urlString += "&output=JSON";
        HttpGet request = new HttpGet(urlString);
        request.addHeader(HttpHeaders.AUTHORIZATION, getBearerToken());
        logger.debug("HTTP Method: %s",request);
        HttpClient client = HttpClientBuilder.create()
                .build();
        HttpResponse response = null;
        try {
            response = client.execute(request);
            logger.debug("Response Status Line: %s",response.getStatusLine());
        } catch (IOException e) {
            logger.error("Exception in attempting to get access token, Exception: %s",e.getMessage());
            return null;
        }
        if( response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
            logger.warn("Access Key retreival returned bad status: %s", response.getStatusLine());
            return null;
        }
        HttpEntity entity = response.getEntity();
        Header encodingHeader = entity.getContentEncoding();
        Charset encoding = encodingHeader == null ? StandardCharsets.UTF_8 : Charsets.toCharset(encodingHeader.getValue());
        String json = null;
        try {
            json = EntityUtils.toString(entity, StandardCharsets.UTF_8);
            logger.trace("JSON returned: %s",json);
        } catch (IOException e) {
            logger.warn("IOException parsing returned encoded string to json text: "+ e.getMessage());
            return null;
        }
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        metricData = gson.fromJson(json, MetricData[].class);
        return metricData;
    }

    public TreeNode[] getApplicationMetricFolders(Application application, String path) {
        String json = null;
        if( "".equals(path)) {
            json = getRequest(String.format("controller/rest/applications/%s/metrics?output=JSON", Utility.encode(application.name)));
        } else {
            json = getRequest(String.format("controller/rest/applications/%s/metrics?metric-path=%s&output=JSON", Utility.encode(application.name), Utility.encode(path)));
        }

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.fromJson(json, TreeNode[].class);
    }

    public MetricData[] getAllMetricsForAllApplications() {
        ArrayList<MetricData> metrics = new ArrayList<>();
        for( Application application : this.applications ) {
            for( ApplicationMetric applicationMetric : application.metrics ) {
                for( MetricData metricData : getMetricValue( application, applicationMetric )) {
                    metricData.controllerHostname = this.hostname;
                    metricData.applicationName = application.name;
                    metricData.targetTable = application.defaultMetricTableName;
                    metrics.add(metricData);
                }
            }
        }
        return metrics.toArray( new MetricData[0] );
    }

    public EventData[] getAllEventsForAllApplications() {
        ArrayList<EventData> events = new ArrayList<>();
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        for( Application application : this.applications ) {
            if( application.getAllEvents ) {
                String json = getRequest("controller/rest/applications/%s/events?time-range-type=%s&duration-in-mins=%s&event-types=%s&severities=%s&output=JSON",
                        Utility.encode(application.name), application.defaultTimeRangeType, application.defaultDurationInMinutes, application.eventTypeList, application.eventSeverities);
                for (EventData event : gson.fromJson(json, EventData[].class)) {
                    event.controllerHostname = this.hostname;
                    event.applicationName = application.name;
                    event.targetTable = application.defaultEventTableName;
                    events.add(event);
                }
            }
        }
        return events.toArray( new EventData[0]);
    }

    private String getRequest( String formatOrURI, Object... args ) {
        if( args == null || args.length == 0 ) return getRequest(formatOrURI);
        return getRequest( String.format(formatOrURI,args));
    }

    private String getRequest( String uri ) {
        HttpGet request = new HttpGet(String.format("%s/%s", this.url.toString(), uri));
        request.addHeader(HttpHeaders.AUTHORIZATION, getBearerToken());
        logger.debug("HTTP Method: %s",request);
        HttpClient client = HttpClientBuilder.create()
                .build();
        HttpResponse response = null;
        try {
            response = client.execute(request);
            logger.debug("Response Status Line: %s",response.getStatusLine());
        } catch (IOException e) {
            logger.error("Exception in attempting to get controller data, Exception: %s",e.getMessage());
            return null;
        }
        if( response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
            logger.warn("request returned bad status: %s", response.getStatusLine());
            return null;
        }
        HttpEntity entity = response.getEntity();
        Header encodingHeader = entity.getContentEncoding();
        Charset encoding = encodingHeader == null ? StandardCharsets.UTF_8 : Charsets.toCharset(encodingHeader.getValue());
        String json = null;
        try {
            json = EntityUtils.toString(entity, StandardCharsets.UTF_8);
            logger.trace("JSON returned: %s",json);
        } catch (IOException e) {
            logger.warn("IOException parsing returned encoded string to json text: "+ e.getMessage());
            return null;
        }
        return json;
    }

    Map<String,Integer> _applicationIdMap = null;
    public int getApplicationId( String name ) {
        if( _applicationIdMap == null ) { //go get em
            String json = getRequest("controller/rest/applications?output=JSON");
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            Application[] apps = gson.fromJson(json, Application[].class);
            _applicationIdMap = new HashMap<>();
            for( Application app : apps )
                _applicationIdMap.put(app.name, app.id);
        }
        return _applicationIdMap.get(name);
    }

    public Model getModel() {
        if( this.controllerModel == null ) {
            String json = getRequest("controller/rest/applications");
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            this.controllerModel = new Model( gson.fromJson(json, com.cisco.josouthe.data.model.Application[].class));
            for(com.cisco.josouthe.data.model.Application application : this.controllerModel.getApplications() ) {
                json = getRequest("controller/rest/applications/%d/tiers",application.id);
                application.tiers = gson.fromJson(json, Tier[].class);
                json = getRequest("controller/rest/applications/%d/nodes",application.id);
                application.nodes = gson.fromJson(json, Node[].class);
            }
        }
        return this.controllerModel;
    }

    public static void main( String... args ) throws MalformedURLException {
        Controller controller = new Controller("https://southerland-test.saas.appdynamics.com/", "ETLClient@southerland-test", "869b6e71-230c-4e6f-918d-6713fb73b3ad", null);
        System.out.printf("%s Test 1: %s\n", Controller.class, controller.getBearerToken());
        MetricData[] metricData = controller.getMetricValue("https://southerland-test.saas.appdynamics.com/controller/rest/applications/Agent%20Proxy/metric-data?metric-path=Application%20Infrastructure%20Performance%7C*%7CJVM%7CProcess%20CPU%20Usage%20%25&time-range-type=BEFORE_NOW&duration-in-mins=60");
        System.out.printf("%s Test 2: %d elements\n", Controller.class, metricData.length);
        metricData = controller.getMetricValue("https://southerland-test.saas.appdynamics.com/controller/rest/applications/Agent%20Proxy/metric-data?metric-path=Business%20Transaction%20Performance%7CBusiness%20Transactions%7C*%7C*%7CAverage%20Response%20Time%20%28ms%29&time-range-type=BEFORE_NOW&duration-in-mins=60");
        System.out.printf("%s Test 3: %d elements\n", Controller.class, metricData.length);
    }
}
