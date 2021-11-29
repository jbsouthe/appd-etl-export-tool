package com.cisco.josouthe.data.metric;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Baseline {
    private static final Logger logger = LogManager.getFormatterLogger();
    public long id, applicationId;
    public String name, seasonality;
    public int numberOfDays;
    public boolean nameUnique, fixed, allData, defaultBaseline;
}
