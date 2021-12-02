package com.cisco.josouthe.data.metric;

import java.util.List;

public class BaselineData {
    public long metricId;
    public String metricName, frequency;
    public int granularityMinutes;
    public List<BaselineTimeslice> dataTimeslices;
    public String targetTable = null;
    public String controllerHostname;
    public String applicationName;
    public Baseline baseline;

    public void purgeNullBaselineTimeslices() {
        if( dataTimeslices == null || dataTimeslices.isEmpty() ) return;
        for( BaselineTimeslice baselineTimeslice : dataTimeslices )
            if( baselineTimeslice.metricValue == null ) dataTimeslices.remove(baselineTimeslice);
    }

    public boolean hasData() {
        return dataTimeslices != null && dataTimeslices.size() > 0;
    }
}
