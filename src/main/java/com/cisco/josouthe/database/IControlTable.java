package com.cisco.josouthe.database;

public interface IControlTable {
    void setDefaultLoadNumberOfHoursIfControlRowMissing(int hours);

    ControlEntry getLastRunTimestamp(String controller, String application, String dataType);

    int setLastRunTimestamp(ControlEntry controlEntry);
}
