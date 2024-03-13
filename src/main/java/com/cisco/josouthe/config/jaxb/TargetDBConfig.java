package com.cisco.josouthe.config.jaxb;

import javax.xml.bind.annotation.*;

@XmlAccessorType(XmlAccessType.FIELD)
public class TargetDBConfig {

    @XmlElement(name = "ConnectionString")
    private String connectionString;

    @XmlElement(name = "User")
    private String user;

    @XmlElement(name = "Password")
    private String password;

    @XmlElement(name = "DefaultMetricTable")
    private String defaultMetricTable = "AppDynamics_DefaultTable";

    @XmlElement(name = "ControlTable")
    private String controlTable = "AppDynamics_SchedulerControl";

    @XmlElement(name = "DefaultEventTable")
    private String defaultEventTable = "AppDynamics_EventTable";

    @XmlElement(name = "DefaultBaselineTable")
    private String defaultBaselineTable = "AppDynamics_BaselineTable";

    @XmlElement(name = "MaximumColumnNameLength")
    private long maximumColumnNameLength = 30;

    public String getConnectionString () {
        return connectionString;
    }

    public void setConnectionString (String connectionString) {
        this.connectionString = connectionString;
    }

    public String getUser () {
        return user;
    }

    public void setUser (String user) {
        this.user = user;
    }

    public String getPassword () {
        return password;
    }

    public void setPassword (String password) {
        this.password = password;
    }

    public String getDefaultMetricTable () {
        return defaultMetricTable;
    }

    public void setDefaultMetricTable (String defaultMetricTable) {
        this.defaultMetricTable = defaultMetricTable;
    }

    public String getControlTable () {
        return controlTable;
    }

    public void setControlTable (String controlTable) {
        this.controlTable = controlTable;
    }

    public String getDefaultEventTable () {
        return defaultEventTable;
    }

    public void setDefaultEventTable (String defaultEventTable) {
        this.defaultEventTable = defaultEventTable;
    }

    public String getDefaultBaselineTable () {
        return defaultBaselineTable;
    }

    public void setDefaultBaselineTable (String defaultBaselineTable) {
        this.defaultBaselineTable = defaultBaselineTable;
    }

    public long getMaximumColumnNameLength () {
        return maximumColumnNameLength;
    }

    public void setMaximumColumnNameLength (long maximumColumnNameLength) {
        this.maximumColumnNameLength = maximumColumnNameLength;
    }
}
