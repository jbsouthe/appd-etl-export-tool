package com.cisco.josouthe.config.jaxb;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
public class ControllerConfig {
    @XmlAttribute
    private boolean getAllAnalyticsSearches = false;

    @XmlElement(name = "URL")
    private String url;

    @XmlElement(name = "ClientID")
    private String clientID;

    @XmlElement(name = "ClientSecret")
    private String clientSecret;

    @XmlElement(name = "AdjustEndTimeMinutes")
    private long adjustEndTimeMinutes = 5;

    @XmlElement(name = "Application")
    private List<ApplicationConfig> applicationList = new ArrayList<>();

    public boolean isGetAllAnalyticsSearches () {
        return getAllAnalyticsSearches;
    }

    public void setGetAllAnalyticsSearches (boolean getAllAnalyticsSearches) {
        this.getAllAnalyticsSearches = getAllAnalyticsSearches;
    }

    public String getUrl () {
        return url;
    }

    public void setUrl (String url) {
        this.url = url;
    }

    public String getClientID () {
        return clientID;
    }

    public void setClientID (String clientID) {
        this.clientID = clientID;
    }

    public String getClientSecret () {
        return clientSecret;
    }

    public void setClientSecret (String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public long getAdjustEndTimeMinutes () {
        return adjustEndTimeMinutes;
    }

    public void setAdjustEndTimeMinutes (long adjustEndTimeMinutes) {
        this.adjustEndTimeMinutes = adjustEndTimeMinutes;
    }

    public List<ApplicationConfig> getApplicationList () {
        return applicationList;
    }

    public void setApplicationList (List<ApplicationConfig> applicationList) {
        this.applicationList = applicationList;
    }
}
