package com.cisco.josouthe.print;

public interface IPrintable {
    Object[] toArray();
    String[] getHeader();
    Justification[] getJustifications();
}
