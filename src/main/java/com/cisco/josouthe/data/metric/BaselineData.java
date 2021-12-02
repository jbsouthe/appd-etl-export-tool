package com.cisco.josouthe.data.metric;

import java.util.ArrayList;
import java.util.Iterator;
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

    public long purgeNullBaselineTimeslices() {
        if( dataTimeslices == null || dataTimeslices.isEmpty() ) return 0;
        long counter=0;
        synchronized (this.dataTimeslices) {
            List<BaselineTimeslice> newDataTimeslices = new ArrayList<>();
            Iterator<BaselineTimeslice> it = dataTimeslices.iterator();
            while(it.hasNext()) {
                BaselineTimeslice baselineTimeslice = it.next();
                if( baselineTimeslice.metricValue != null ) {
                    newDataTimeslices.add(baselineTimeslice);
                } else {
                    counter++;
                }
            }
            this.dataTimeslices.clear();
            this.dataTimeslices.addAll(newDataTimeslices);
        }
        return counter;
    }

    public boolean hasData() {
        return dataTimeslices != null && dataTimeslices.size() > 0;
    }
}
