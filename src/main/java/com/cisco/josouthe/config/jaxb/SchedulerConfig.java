package com.cisco.josouthe.config.jaxb;

import javax.xml.bind.annotation.*;

@XmlAccessorType(XmlAccessType.FIELD)
public class SchedulerConfig {
    @XmlAttribute
    private boolean enabled = false;

    @XmlElement(name = "PollIntervalMinutes")
    private long pollIntervalMinutes = 60;

    @XmlElement(name = "FirstRunHistoricNumberOfDays")
    private long firstRunHistoricNumberOfDays = 2;

    @XmlElement(name = "FirstRunHistoricNumberOfHours")
    private long firstRunHistoricNumberOfHours = 48;

    @XmlElement(name = "ControllerThreads")
    private long controllerThreads = 50;

    @XmlElement(name = "DatabaseThreads")
    private long databaseThreads = 50;

    @XmlElement(name = "ConfigurationRefreshEveryHours")
    private long configurationRefreshEveryHours = 12;

    @XmlElement(name = "MaxNumberOfDaysToQueryAtATime")
    private long maxNumberOfDaysToQueryAtATime = 14;

    public boolean isEnabled () {
        return enabled;
    }

    public void setEnabled (boolean enabled) {
        this.enabled = enabled;
    }

    public long getPollIntervalMinutes () {
        return pollIntervalMinutes;
    }

    public void setPollIntervalMinutes (long pollIntervalMinutes) {
        this.pollIntervalMinutes = pollIntervalMinutes;
    }

    public long getFirstRunHistoricNumberOfDays () {
        return firstRunHistoricNumberOfDays;
    }

    public void setFirstRunHistoricNumberOfDays (long firstRunHistoricNumberOfDays) {
        this.firstRunHistoricNumberOfDays = firstRunHistoricNumberOfDays;
    }

    public long getFirstRunHistoricNumberOfHours () {
        return firstRunHistoricNumberOfHours;
    }

    public void setFirstRunHistoricNumberOfHours (long firstRunHistoricNumberOfHours) {
        this.firstRunHistoricNumberOfHours = firstRunHistoricNumberOfHours;
    }

    public long getControllerThreads () {
        return controllerThreads;
    }

    public void setControllerThreads (long controllerThreads) {
        this.controllerThreads = controllerThreads;
    }

    public long getDatabaseThreads () {
        return databaseThreads;
    }

    public void setDatabaseThreads (long databaseThreads) {
        this.databaseThreads = databaseThreads;
    }

    public long getConfigurationRefreshEveryHours () {
        return configurationRefreshEveryHours;
    }

    public void setConfigurationRefreshEveryHours (long configurationRefreshEveryHours) {
        this.configurationRefreshEveryHours = configurationRefreshEveryHours;
    }

    public long getMaxNumberOfDaysToQueryAtATime () {
        return maxNumberOfDaysToQueryAtATime;
    }

    public void setMaxNumberOfDaysToQueryAtATime (long maxNumberOfDaysToQueryAtATime) {
        this.maxNumberOfDaysToQueryAtATime = maxNumberOfDaysToQueryAtATime;
    }
}
