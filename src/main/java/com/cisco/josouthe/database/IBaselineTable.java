package com.cisco.josouthe.database;

import com.cisco.josouthe.exceptions.FailedDataLoadException;

public interface IBaselineTable {
    public int insert( Object object ) throws FailedDataLoadException;
}
