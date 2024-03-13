package com.cisco.josouthe.config.jaxb;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
public class AnalyticsConfig {
    @XmlElement(name = "URL")
    private String url;

    @XmlElement(name = "GlobalAccountName")
    private String globalAccountName;

    @XmlElement(name = "APIKey")
    private String aPIKey;

    @XmlElement(name = "TableNamePrefix")
    private String tableNamePrefix;

    @XmlElement(name = "LinkToControllerHostname")
    private String linkToControllerHostname;

    @XmlElement(name = "AdjustEndTimeMinutes")
    private long adjustEndTimeMinutes = 5;

    @XmlElement(name = "Search")
    private List<AnalyticsSearchConfig> searchList = new ArrayList<>();

    public String getUrl () {
        return url;
    }

    public void setUrl (String url) {
        this.url = url;
    }

    public String getGlobalAccountName () {
        return globalAccountName;
    }

    public void setGlobalAccountName (String globalAccountName) {
        this.globalAccountName = globalAccountName;
    }

    public String getaPIKey () {
        return aPIKey;
    }

    public void setaPIKey (String aPIKey) {
        this.aPIKey = aPIKey;
    }

    public String getTableNamePrefix () {
        return tableNamePrefix;
    }

    public void setTableNamePrefix (String tableNamePrefix) {
        this.tableNamePrefix = tableNamePrefix;
    }

    public String getLinkToControllerHostname () {
        return linkToControllerHostname;
    }

    public void setLinkToControllerHostname (String linkToControllerHostname) {
        this.linkToControllerHostname = linkToControllerHostname;
    }

    public long getAdjustEndTimeMinutes () {
        return adjustEndTimeMinutes;
    }

    public void setAdjustEndTimeMinutes (long adjustEndTimeMinutes) {
        this.adjustEndTimeMinutes = adjustEndTimeMinutes;
    }

    public List<AnalyticsSearchConfig> getSearchList () {
        return searchList;
    }

    public void setSearchList (List<AnalyticsSearchConfig> searchList) {
        this.searchList = searchList;
    }
}
