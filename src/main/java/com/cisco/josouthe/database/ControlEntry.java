package com.cisco.josouthe.database;

public class ControlEntry {
    public String controller, application, type;
    public long timestamp;
    public boolean notYetProcessed = true;
    public boolean rowExistsInDB = false;
}
