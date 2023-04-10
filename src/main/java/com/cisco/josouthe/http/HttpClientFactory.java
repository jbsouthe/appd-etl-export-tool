package com.cisco.josouthe.http;

import com.cisco.josouthe.exceptions.ControllerBadStatusException;
import org.apache.http.*;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.NTCredentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.ProxyAuthenticationStrategy;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class HttpClientFactory {
    private static final Logger logger = LogManager.getFormatterLogger();
    private static HttpClient httpClient = null;

    public static HttpClient getHttpClient() {
        return getHttpClient(false);
    }

    public static HttpClient getHttpClient( boolean forceRebuild ) {
        if( httpClient == null || forceRebuild == true ) {
            logger.debug("Creating new HttpClient instance, forceRebuild=%s",forceRebuild);
            PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager(20, TimeUnit.MINUTES);
            connectionManager.setValidateAfterInactivity(1000);
            HttpClientBuilder httpClientBuilder = HttpClientBuilder
                .create()
                .useSystemProperties()
                .setConnectionManager(connectionManager)
                .setConnectionManagerShared(true);
            if( isProxyHostDefined() ) {
                logger.debug("Proxy host is defined, setting proxy to: %s:%s",System.getProperty("http.proxyHost"),System.getProperty("http.proxyPort") );
                HttpHost proxyHost = new HttpHost( System.getProperty("http.proxyHost"), Integer.parseInt(System.getProperty("http.proxyPort")));
                httpClientBuilder.setProxy(proxyHost);
                if( isProxyAuthDefined() ) {
                    CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
                    Credentials credentials = null;
                    if( isProxyNTLMDefined() ) {
                        logger.debug("Using NTLM Proxy Authentication, user=%s, pass=****, workstation=%s, domain=%s",System.getProperty("http.proxyUser"), System.getProperty("http.proxyWorkstation"), System.getProperty("http.proxyDomain"));
                        credentials = new NTCredentials(System.getProperty("http.proxyUser"), System.getProperty("http.proxyPassword"), System.getProperty("http.proxyWorkstation"), System.getProperty("http.proxyDomain"));
                    } else {
                        logger.debug("Using Basic Proxy Authentication, user=%s, pass=****",System.getProperty("http.proxyUser"));
                        credentials = new UsernamePasswordCredentials( System.getProperty("http.proxyUser"), System.getProperty("http.proxyPassword") );
                    }
                    credentialsProvider.setCredentials( new AuthScope(proxyHost), credentials );
                    httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
                    httpClientBuilder.setProxyAuthenticationStrategy( new ProxyAuthenticationStrategy());
                }
            }
            if( isAcceptSelfSignedDefined() ) {
                HostnameVerifier hv = new HostnameVerifier() { public boolean verify(String urlHostname, SSLSession session) { return true; }};
                HttpsURLConnection.setDefaultHostnameVerifier(hv);
                try {
                    SSLContextBuilder sslContextBuilder = new SSLContextBuilder();
                    sslContextBuilder.loadTrustMaterial(null, new TrustSelfSignedStrategy());
                    SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslContextBuilder.build());
                    httpClientBuilder.setSSLSocketFactory(sslsf);
                } catch (Exception exception) {
                    logger.warn("Exception raised trying to accept self signed keys, Exception: %s", exception.getMessage());
                }

            }
            httpClient = httpClientBuilder.build();
        }
        return httpClient;
    }

    public static boolean isProxyHostDefined() {
        return !System.getProperty("http.proxyHost", "unset").equals("unset")
                && !System.getProperty("http.proxyPort", "unset").equals("unset");
    }

    public static boolean isProxyAuthDefined() {
        return !System.getProperty("http.proxyUser", "unset").equals("unset")
                && !System.getProperty("http.proxyPassword", "unset").equals("unset");
    }

    public static boolean isProxyNTLMDefined() {
        return !System.getProperty("http.proxyWorkstation", "unset").equals("unset")
                && !System.getProperty("http.proxyDomain", "unset").equals("unset");
    }

    public static boolean isAcceptSelfSignedDefined() {
        return System.getProperty("allow.self.signed.certs", "false").equals("true");
    }

    public static ResponseHandler<String> getStringResponseHandler( String wireToTrace ) {
        return getStringResponseHandler(isWireTraceEnabled(wireToTrace));
    }

    public static boolean isWireTraceEnabled( String wireToTrace ) {
        return System.getProperty("wireTrace") != null && System.getProperty("wireTrace").toLowerCase().contains(wireToTrace);
    }

    public static ResponseHandler<String> getStringResponseHandler( boolean enableWireTrace ) {
        return new ResponseHandler<String>() {
            private String uri = "Unset";
            public void setUri( String uri ) { this.uri=uri; }
            private boolean wireTraceEnabled = enableWireTrace;
            public void setWireTraceEnabled( boolean b ) { this.wireTraceEnabled=b; }

            @Override
            public String handleResponse( final HttpResponse response) throws IOException {
                final int status = response.getStatusLine().getStatusCode();
                if (status >= HttpStatus.SC_OK && status < HttpStatus.SC_TEMPORARY_REDIRECT) {
                    final HttpEntity entity = response.getEntity();
                    try {
                        String json =entity != null ? EntityUtils.toString(entity) : null;
                        if( wireTraceEnabled ) logger.info("JSON returned: '%s'",json);
                        return json;
                    } catch (final ParseException ex) {
                        throw new ClientProtocolException(ex);
                    }
                } else {
                    throw new ControllerBadStatusException(response.getStatusLine().toString(), EntityUtils.toString(response.getEntity()), uri);
                }
            }

        };
    }
}
