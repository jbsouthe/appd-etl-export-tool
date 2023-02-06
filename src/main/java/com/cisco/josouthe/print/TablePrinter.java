package com.cisco.josouthe.print;

import java.util.Date;

public class TablePrinter implements IPrintable{

    public String controller="", application="", name="", type="";
    public Long oldestRowTimestamp=0l, newestRowTimestamp=0l, size=0l;

    @Override
    public Object[] toArray() {
        return new Object[] { controller, application, name, type, (oldestRowTimestamp == 0 ? "empty" :new Date(oldestRowTimestamp)), (newestRowTimestamp == 0 ? "empty" : new Date(newestRowTimestamp) ), size };
    }

    @Override
    public String[] getHeader() {
        return new String[] {"Controller", "Application", "Table Name", "Data Type", "Oldest Row", "Newest Row", "# Rows"};
    }

    @Override
    public Justification[] getJustifications() {
        return new Justification[] { Justification.LEFT, Justification.LEFT, Justification.LEFT, Justification.LEFT, Justification.CENTER, Justification.CENTER,Justification.RIGHT };
    }
}
