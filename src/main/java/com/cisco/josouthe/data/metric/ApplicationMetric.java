package com.cisco.josouthe.data.metric;

public class ApplicationMetric {
    public String disableDataRollup = "true";
    public String name = null;
    public ApplicationMetric( String disableDataRollup, String name) {
        if( disableDataRollup != null ) this.disableDataRollup=disableDataRollup;
        this.name=name;
    }
}
