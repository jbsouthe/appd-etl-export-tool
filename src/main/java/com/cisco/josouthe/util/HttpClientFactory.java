package com.cisco.josouthe.util;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.NTCredentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.ProxyAuthenticationStrategy;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class HttpClientFactory {
    private static final Logger logger = LogManager.getFormatterLogger();
    private static HttpClient httpClient = null;

    public static HttpClient getHttpClient() {
        return getHttpClient(false);
    }

    public static HttpClient getHttpClient( boolean forceRebuild ) {
        if( httpClient == null || forceRebuild == true ) {
            logger.debug("Creating new HttpClient instance, forceRebuild=%s",forceRebuild);
            HttpClientBuilder httpClientBuilder = HttpClientBuilder
                .create()
                .useSystemProperties()
                .setConnectionManager(new PoolingHttpClientConnectionManager())
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
}
