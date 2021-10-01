package com.cisco.josouthe.data;

public class ApplicationMetric {
    String timeRangeType = "BEFORE_NOW";
    String durationInMins = "60";
    String disableDataRollup = "false";
    String name = null;
    public ApplicationMetric(String timeRangeType, String durationInMins, String disableDataRollup, String name) {
        if( timeRangeType != null ) this.timeRangeType=timeRangeType;
        if( durationInMins != null ) this.durationInMins=durationInMins;
        if( disableDataRollup != null ) this.disableDataRollup=disableDataRollup;
        this.name=name;
    }
}
