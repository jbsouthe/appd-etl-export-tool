package com.cisco.josouthe.data;

public class ApplicationMetric {
    String timeRangeType = "BEFORE_NOW";
    String durationInMins = "60";
    String name = null;
    public ApplicationMetric(String timeRangeType, String durationInMins, String name) {
        if( timeRangeType != null ) this.timeRangeType=timeRangeType;
        if( durationInMins != null ) this.durationInMins=durationInMins;
        this.name=name;
    }
}
