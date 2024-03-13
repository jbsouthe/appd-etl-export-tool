package com.cisco.josouthe.config.jaxb;

import javax.xml.bind.annotation.*;

@XmlAccessorType(XmlAccessType.FIELD)
public class OptionalLoggerConfig {

    @XmlElement(name = "Name")
    private String name;

    @XmlElement(name = "Level")
    private String level;

    @XmlElement(name = "Additivity")
    private boolean additivity = false;

    public String getName () {
        return name;
    }

    public void setName (String name) {
        this.name = name;
    }

    public String getLevel () {
        return level;
    }

    public void setLevel (String level) {
        this.level = level;
    }

    public boolean isAdditivity () {
        return additivity;
    }

    public void setAdditivity (boolean additivity) {
        this.additivity = additivity;
    }
}
