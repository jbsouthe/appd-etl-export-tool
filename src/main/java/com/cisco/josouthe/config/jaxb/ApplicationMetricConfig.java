package com.cisco.josouthe.config.jaxb;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlValue;

@XmlAccessorType(XmlAccessType.FIELD)
public class ApplicationMetricConfig {
    @XmlAttribute(name = "time-range-type")
    private String timeRangeType;

    @XmlAttribute(name = "duration-in-mins")
    private int durationInMins;

    @XmlValue
    private String value;

    public String getTimeRangeType () {
        return timeRangeType;
    }

    public void setTimeRangeType (String timeRangeType) {
        this.timeRangeType = timeRangeType;
    }

    public int getDurationInMins () {
        return durationInMins;
    }

    public void setDurationInMins (int durationInMins) {
        this.durationInMins = durationInMins;
    }

    public String getValue () {
        return value;
    }

    public void setValue (String value) {
        this.value = value;
    }
}
