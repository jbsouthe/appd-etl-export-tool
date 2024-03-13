package com.cisco.josouthe.config.jaxb;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlValue;

@XmlAccessorType(XmlAccessType.FIELD)
public class AnalyticsSearchConfig {
    @XmlAttribute(name = "name")
    private String name;

    @XmlAttribute(name = "limit")
    private long limit = 20000;

    @XmlAttribute(name = "vizualization")
    private String vizualization = "TABLE";

    @XmlValue
    private String value;

    public String getName () {
        return name;
    }

    public void setName (String name) {
        this.name = name;
    }

    public long getLimit () {
        return limit;
    }

    public void setLimit (long limit) {
        this.limit = limit;
    }

    public String getVizualization () {
        return vizualization;
    }

    public void setVizualization (String vizualization) {
        this.vizualization = vizualization;
    }

    public String getValue () {
        return value;
    }

    public void setValue (String value) {
        this.value = value;
    }
}
