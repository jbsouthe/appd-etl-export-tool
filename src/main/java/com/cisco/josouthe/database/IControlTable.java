package com.cisco.josouthe.database;

import java.util.List;

public interface IControlTable {
    void setDefaultLoadNumberOfHoursIfControlRowMissing(int hours);

    ControlEntry getLastRunTimestamp(String controller, String application, String dataType);

    int setLastRunTimestamp(ControlEntry controlEntry);

    List<ControlEntry> getControlEntries();
}
