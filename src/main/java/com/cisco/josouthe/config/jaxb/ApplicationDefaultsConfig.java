package com.cisco.josouthe.config.jaxb;

import javax.xml.bind.annotation.*;

@XmlAccessorType(XmlAccessType.FIELD)
public class ApplicationDefaultsConfig {
    @XmlElement(name = "DisableDataRollup")
    private boolean disableDataRollup = true;

    @XmlElement(name = "MetricTable")
    private String metricTable;

    @XmlElement(name = "EventTable")
    private String eventTable;

    @XmlElement(name = "BaselineTable")
    private String baselineTable;

    @XmlElement(name = "GranularityMinutes")
    private int granularityMinutes;
    @XmlElement(name = "OnlyGetDefaultBaseline")
    private boolean onlyGetDefaultBaseline;

    public boolean isDisableDataRollup () {
        return disableDataRollup;
    }

    public void setDisableDataRollup (boolean disableDataRollup) {
        this.disableDataRollup = disableDataRollup;
    }

    public String getMetricTable () {
        return metricTable;
    }

    public void setMetricTable (String metricTable) {
        this.metricTable = metricTable;
    }

    public String getEventTable () {
        return eventTable;
    }

    public void setEventTable (String eventTable) {
        this.eventTable = eventTable;
    }

    public String getBaselineTable () {
        return baselineTable;
    }

    public void setBaselineTable (String baselineTable) {
        this.baselineTable = baselineTable;
    }

    public int getGranularityMinutes () {
        return granularityMinutes;
    }

    public void setGranularityMinutes (int granularityMinutes) {
        this.granularityMinutes = granularityMinutes;
    }

    public boolean isOnlyGetDefaultBaseline () {
        return onlyGetDefaultBaseline;
    }

    public void setOnlyGetDefaultBaseline (boolean onlyGetDefaultBaseline) {
        this.onlyGetDefaultBaseline = onlyGetDefaultBaseline;
    }
}
