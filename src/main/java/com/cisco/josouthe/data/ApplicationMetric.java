package com.cisco.josouthe.data;

public class ApplicationMetric {
    String disableDataRollup = "false";
    String name = null;
    public ApplicationMetric( String disableDataRollup, String name) {
        if( disableDataRollup != null ) this.disableDataRollup=disableDataRollup;
        this.name=name;
    }
}
