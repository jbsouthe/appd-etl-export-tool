package com.cisco.josouthe.config.jaxb;

import javax.xml.bind.annotation.*;

@XmlAccessorType(XmlAccessType.FIELD)
public class ApplicationName {
    @XmlAttribute
    private boolean regex = false;

    @XmlValue
    private String name;

    public boolean isRegex () {
        return regex;
    }

    public void setRegex (boolean regex) {
        this.regex = regex;
    }

    public String getName () {
        return name;
    }

    public void setName (String name) {
        this.name = name;
    }
}
