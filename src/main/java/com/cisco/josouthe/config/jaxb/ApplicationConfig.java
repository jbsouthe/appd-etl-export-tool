package com.cisco.josouthe.config.jaxb;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
public class ApplicationConfig {

    @XmlAttribute
    private boolean getAllAvailableMetrics = false;

    @XmlAttribute
    private boolean getAllEvents = false;

    @XmlAttribute
    private boolean getAllHealthRuleViolations = false;

    @XmlElement(name = "Name")
    private ApplicationName name = new ApplicationName();

    @XmlElement(name = "Defaults")
    private ApplicationDefaultsConfig defaults = new ApplicationDefaultsConfig();

    @XmlElement(name = "Events")
    private ApplicationEventsConfig events = new ApplicationEventsConfig();

    @XmlElement(name = "Metric")
    private List<ApplicationMetricConfig> metricList = new ArrayList<>();

    public boolean isGetAllAvailableMetrics () {
        return getAllAvailableMetrics;
    }

    public void setGetAllAvailableMetrics (boolean getAllAvailableMetrics) {
        this.getAllAvailableMetrics = getAllAvailableMetrics;
    }

    public boolean isGetAllEvents () {
        return getAllEvents;
    }

    public void setGetAllEvents (boolean getAllEvents) {
        this.getAllEvents = getAllEvents;
    }

    public boolean isGetAllHealthRuleViolations () {
        return getAllHealthRuleViolations;
    }

    public void setGetAllHealthRuleViolations (boolean getAllHealthRuleViolations) {
        this.getAllHealthRuleViolations = getAllHealthRuleViolations;
    }

    public ApplicationName getName () {
        return name;
    }

    public void setName (ApplicationName name) {
        this.name = name;
    }

    public ApplicationDefaultsConfig getDefaults () {
        return defaults;
    }

    public void setDefaults (ApplicationDefaultsConfig defaults) {
        this.defaults = defaults;
    }

    public ApplicationEventsConfig getEvents () {
        return events;
    }

    public void setEvents (ApplicationEventsConfig events) {
        this.events = events;
    }

    public List<ApplicationMetricConfig> getMetricList () {
        return metricList;
    }

    public void setMetricList (List<ApplicationMetricConfig> metricList) {
        this.metricList = metricList;
    }
}