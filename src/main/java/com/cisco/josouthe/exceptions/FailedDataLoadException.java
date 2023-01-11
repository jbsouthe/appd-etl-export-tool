package com.cisco.josouthe.exceptions;

public class FailedDataLoadException  extends Exception {
    Object[] data;
    public FailedDataLoadException( String s, Object[] someData ) {
        super(s);
        this.data = someData;
    }
    public FailedDataLoadException( String s, Object atomicData ) {
        super(s);
        this.data = new Object[]{ atomicData };
    }
    public Object[] getData() { return data; }
}
