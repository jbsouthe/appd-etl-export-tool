package com.cisco.josouthe.database;

import com.cisco.josouthe.exceptions.FailedDataLoadException;

public interface IMetricTable {
    public int insert( Object object ) throws FailedDataLoadException;
}
