package com.cisco.josouthe.data;

public class MetricOrFolder {
    public String name, type;
    public boolean isFolder() { return "folder".equals(type);}
    public boolean isMetric() { return !isFolder(); }
}
