package com.cisco.josouthe.data;

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

public class Controller {
    private static final Logger logger = LogManager.getFormatterLogger();

    public String hostname;
    public URL url;
    private String clientId, clientSecret;
    private AccessToken accessToken = null;
    public Application[] applications = null;

    public Controller( String urlString, String clientId, String clientSecret, Application[] applications ) throws MalformedURLException {
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

        HttpPost request = new HttpPost(url.toString()+"controller/api/oauth/access_token");
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
        return getMetricValue( String.format("%s/controller/rest/applications/%s/metric-data?metric-path=%s&time-range-type=%s&duration-in-mins=%s&output=JSON",this.url, Utility.encode(application.name), Utility.encode(metric.name),metric.timeRangeType,metric.durationInMins));
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

    public MetricOrFolder[] getApplicationMetricFolders(Application application, String path) {
        HttpGet request = null;
        if( "".equals(path)) {
            request = new HttpGet(String.format("%scontroller/rest/applications/%s/metrics?output=JSON", this.url.toString(), Utility.encode(application.name)));
        } else {
            request = new HttpGet(String.format("%scontroller/rest/applications/%s/metrics?metric-path=%s&output=JSON", this.url.toString(), Utility.encode(application.name), Utility.encode(path)));
        }
        request.addHeader(HttpHeaders.AUTHORIZATION, getBearerToken());
        logger.debug("HTTP Method: %s",request);
        HttpClient client = HttpClientBuilder.create()
                .build();
        HttpResponse response = null;
        try {
            response = client.execute(request);
            logger.debug("Response Status Line: %s",response.getStatusLine());
        } catch (IOException e) {
            logger.error("Exception in attempting to get metric list, Exception: %s",e.getMessage());
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
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.fromJson(json, MetricOrFolder[].class);
    }

    public MetricData[] getAllMetricsForAllApplications() {
        ArrayList<MetricData> metrics = new ArrayList<>();
        for( Application application : this.applications ) {
            for( ApplicationMetric applicationMetric : application.metrics ) {
                for( MetricData metricData : getMetricValue( application, applicationMetric )) {
                    metricData.controllerHostname = this.hostname;
                    metricData.applicationName = application.name;
                    metrics.add(metricData);
                }
            }
        }
        return metrics.toArray( new MetricData[0] );
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
