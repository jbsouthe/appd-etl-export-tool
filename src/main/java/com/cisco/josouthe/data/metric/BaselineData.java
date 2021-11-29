package com.cisco.josouthe.data.metric;

import java.util.List;

public class BaselineData {
    public long metricId;
    public String metricName, frequency;
    public int granularityMinutes;
    public List<BaselineTimeslice> dataTimeslices;
}
