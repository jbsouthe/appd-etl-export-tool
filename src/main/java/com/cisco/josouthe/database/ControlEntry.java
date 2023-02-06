package com.cisco.josouthe.database;

import com.cisco.josouthe.print.IPrintable;
import com.cisco.josouthe.print.Justification;

import java.util.Date;

public class ControlEntry implements IPrintable {
    public String controller, application, type;
    public long timestamp;
    public boolean notYetProcessed = true;
    public boolean rowExistsInDB = false;

    public String toString() {
        return String.format("%s\t%s\t%s\t%s", controller, application, type, new Date(timestamp) );
    }

    public Object[] toArray() {
        return new Object[] {controller, application, type, new Date(timestamp) };
    }

    public String[] getHeader() {
        return new String[] { "Controller", "Application", "Type", "Last Timestamp" };
    }

    public Justification[] getJustifications() {
        return new Justification[] { Justification.LEFT, Justification.LEFT, Justification.LEFT, Justification.RIGHT };
    }
}
