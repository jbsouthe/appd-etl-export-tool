package com.cisco.josouthe.data.metric;

public class ApplicationMetric {
    public boolean disableDataRollup = true;
    public String name = null;
    public ApplicationMetric( String disableDataRollup, String name) {
        if( disableDataRollup != null ) this.disableDataRollup= Boolean.valueOf(disableDataRollup);
        this.name=name;
    }
}
