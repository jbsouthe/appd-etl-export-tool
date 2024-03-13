package com.cisco.josouthe.config.jaxb;

import javax.xml.bind.annotation.*;

@XmlAccessorType(XmlAccessType.FIELD)
public class ApplicationEventsConfig {
    @XmlElement(name = "Include")
    private String include;

    @XmlElement(name = "Exclude")
    private String exclude;

    @XmlElement(name = "Severities")
    private String severities;

    public String getInclude () {
        return include;
    }

    public void setInclude (String include) {
        this.include = include;
    }

    public String getExclude () {
        return exclude;
    }

    public void setExclude (String exclude) {
        this.exclude = exclude;
    }

    public String getSeverities () {
        return severities;
    }

    public void setSeverities (String severities) {
        this.severities = severities;
    }
}
