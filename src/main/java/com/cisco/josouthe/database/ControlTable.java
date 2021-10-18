package com.cisco.josouthe.database;

public interface ControlTable {
    void setDefaultLoadNumberOfHoursIfControlRowMissing(int hours);

    ControlEntry getLastRunTimestamp(String controller, String application, String dataType);

    int setLastRunTimestamp(ControlEntry controlEntry);
}
