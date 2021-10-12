package com.cisco.josouthe.data.metric;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

public class MetricData {
    private static final Logger logger = LogManager.getFormatterLogger();
    public MetricData() {}

    public MetricData( String name, String urlString, String targetTable) throws MalformedURLException {
        this.url = new URL(urlString);
        this.hostname = url.getHost();
        this.metricName = name;
        this.targetTable = targetTable;
    }

    public long metricId;
    public String metricName, metricPath, frequency, hostname;
    public transient URL url;
    public List<MetricValue> metricValues;
    public String targetTable = null;
    public String controllerHostname;
    public String applicationName;
}
