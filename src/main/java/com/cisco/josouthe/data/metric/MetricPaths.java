package com.cisco.josouthe.data.metric;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MetricPaths {
    private static final Logger logger = LogManager.getFormatterLogger();
    private Map<String, List<String>> metricPathMap;

    public MetricPaths() {
        this("MetricPaths.csv");
    }
    public MetricPaths( String filename ) {
        metricPathMap = new HashMap<>();
        try (BufferedReader reader = new BufferedReader( new InputStreamReader(getClass().getClassLoader().getResourceAsStream(filename)))) {
            boolean headerRead=false;
            String word;
            while( ( word = reader.readLine()) != null ){
                if( headerRead ) {
                    String[] parts = word.split(",", 3);
                    addMetricPath(parts[0], parts[1]);
                }else{
                    headerRead=true;
                }
            }
        } catch (IOException ioException) {
            logger.warn("Could not load metric paths from resource %s Error: %s", filename, ioException.getMessage());
        }
    }

    private void addMetricPath( String group, String metricPath ) {
        if( !metricPathMap.containsKey(group) )
            metricPathMap.put(group, new ArrayList<>());
        metricPathMap.get(group).add(metricPath);
    }

    public List<String> getMetricPaths( String... groups ) {
        if( groups == null || groups.length == 0 )
            groups = metricPathMap.keySet().toArray(new String[0]);
        ArrayList<String> list = new ArrayList<>();
        for( String group : groups )
            list.addAll( getMetricPath( group ) );
        return list;
    }

    public List<String> getMetricPath( String group ) {
        ArrayList<String> list = new ArrayList<>();
        if(metricPathMap.containsKey(group))
            list.addAll( metricPathMap.get(group));
        return list;
    }
}
