package com.cisco.josouthe.database;

import com.cisco.josouthe.util.Utility;
import jdk.internal.net.http.common.Pair;

public class ColumnFeatures implements Cloneable {
    public ColumnFeatures() {}
    public ColumnFeatures( String name, String type, int size, boolean isNull) {
        this.name=name.toLowerCase();
        this.type=type.toLowerCase();
        this.size=size;
        this.isNull=isNull;
    }
    public ColumnFeatures( String name, String isNullable, String typeAndSizeString ) {
        this.name = name.toLowerCase();
        if( isNullable != null && isNullable.toLowerCase().equals("not null") ) this.isNull=false;
        if( typeAndSizeString.contains("(") ) { //has a size
            Pair<String,Integer> typeAndSize = Utility.parseTypeAndSizeString(typeAndSizeString);
            this.type = typeAndSize.first.toLowerCase();
            this.size = typeAndSize.second;
        } else { //just a type
            this.type = typeAndSizeString.toLowerCase();
            this.size = -1;
        }
    }

    public String printConstraints() {
        String constraint = type;
        if( size > 0 ) constraint += "("+size+")";
        if( ! isNull ) constraint += " NOT NULL";
        return constraint;
    }

    public String name, type;
    public int size;
    public boolean isNull = true;
    public boolean isMissing = false, isWrongType = false, isWrongSize = false, isWrongNullable = false;

    public ColumnFeatures clone() {
        return new ColumnFeatures(name, type, size, isNull);
    }

}
