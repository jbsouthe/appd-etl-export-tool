package com.cisco.josouthe.config.jaxb;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name = "ETLTool")
@XmlAccessorType(XmlAccessType.FIELD)
public class ETLTool {

    @XmlElement(name = "Logging")
    private LoggingConfig logging = new LoggingConfig();

    @XmlElement(name = "Scheduler")
    private SchedulerConfig scheduler;

    @XmlElement(name = "TargetDB")
    private TargetDBConfig targetDB;

    @XmlElement(name = "Controller")
    private List<ControllerConfig> controllerList = new ArrayList<>();

    @XmlElement(name = "Analytics")
    private List<AnalyticsConfig> analyticsList = new ArrayList<>();

    public LoggingConfig getLogging () {
        return logging;
    }

    public void setLogging (LoggingConfig logging) {
        this.logging = logging;
    }

    public SchedulerConfig getScheduler () {
        return scheduler;
    }

    public void setScheduler (SchedulerConfig scheduler) {
        this.scheduler = scheduler;
    }

    public TargetDBConfig getTargetDB () {
        return targetDB;
    }

    public void setTargetDB (TargetDBConfig targetDB) {
        this.targetDB = targetDB;
    }

    public List<ControllerConfig> getControllerList () {
        return controllerList;
    }

    public void setControllerList (List<ControllerConfig> controllerList) {
        this.controllerList = controllerList;
    }

    public List<AnalyticsConfig> getAnalyticsList () {
        return analyticsList;
    }

    public void setAnalyticsList (List<AnalyticsConfig> analyticsList) {
        this.analyticsList = analyticsList;
    }
}
