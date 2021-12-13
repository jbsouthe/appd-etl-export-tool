package com.cisco.josouthe.data.metric;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.util.List;

public class ApplicationMetricFile {
    private static final Logger logger = LogManager.getFormatterLogger();

    private File file;
    private BufferedWriter writer = null;
    private BufferedReader reader = null;

    public ApplicationMetricFile( String application, String directory) {
        this.file = new File(String.format("%s/Metrics-%s.txt",directory,application));
    }

    public boolean exists() { return this.file.exists(); }
    public boolean hasData() { return this.file.length() > 0; }

    public MetricGraph readMetrics() throws IOException {
        MetricGraph metricGraph = new MetricGraph(null);
        synchronized (file) {
            openFile();
            while( reader.ready() ) {
                metricGraph.addMetricName(reader.readLine());
            }
            close();
        }
        return metricGraph;
    }

    public int writeMetrics( List<String> metricNames ) throws IOException {
        return writeMetrics( metricNames.toArray( new String[0]));
    }
    public int writeMetrics( String... metricNames ) throws IOException {
        int counter=0;
        synchronized (file) {
            openFile();
            for( String metricName : metricNames ) {
                writer.write(metricName);
                writer.newLine();
                counter++;
            }
            close();
        }
        return counter;
    }

    private void openFile() throws IOException {
        if( writer == null ) {
            this.writer = new BufferedWriter( new FileWriter(file) );
        }
        if( reader == null) {
            this.reader = new BufferedReader( new FileReader(file) );
        }
    }

    public void close() throws IOException {
        if( writer != null ) {
            writer.flush();
            writer.close();
            writer=null;
        }
        if( reader != null ) {
            reader.close();
            reader=null;
        }
    }

}
