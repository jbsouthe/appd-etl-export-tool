package com.cisco.josouthe.database;

import com.cisco.josouthe.util.Utility;

public class ColumnFeatures implements Cloneable, Comparable {
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
            TypeAndSize typeAndSize = Utility.parseTypeAndSizeString(typeAndSizeString);
            this.type = typeAndSize.type.toLowerCase();
            this.size = typeAndSize.size;
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

    /**
     * Compares this object with the specified object for order.  Returns a
     * negative integer, zero, or a positive integer as this object is less
     * than, equal to, or greater than the specified object.
     *
     * <p>The implementor must ensure
     * {@code sgn(x.compareTo(y)) == -sgn(y.compareTo(x))}
     * for all {@code x} and {@code y}.  (This
     * implies that {@code x.compareTo(y)} must throw an exception iff
     * {@code y.compareTo(x)} throws an exception.)
     *
     * <p>The implementor must also ensure that the relation is transitive:
     * {@code (x.compareTo(y) > 0 && y.compareTo(z) > 0)} implies
     * {@code x.compareTo(z) > 0}.
     *
     * <p>Finally, the implementor must ensure that {@code x.compareTo(y)==0}
     * implies that {@code sgn(x.compareTo(z)) == sgn(y.compareTo(z))}, for
     * all {@code z}.
     *
     * <p>It is strongly recommended, but <i>not</i> strictly required that
     * {@code (x.compareTo(y)==0) == (x.equals(y))}.  Generally speaking, any
     * class that implements the {@code Comparable} interface and violates
     * this condition should clearly indicate this fact.  The recommended
     * language is "Note: this class has a natural ordering that is
     * inconsistent with equals."
     *
     * <p>In the foregoing description, the notation
     * {@code sgn(}<i>expression</i>{@code )} designates the mathematical
     * <i>signum</i> function, which is defined to return one of {@code -1},
     * {@code 0}, or {@code 1} according to whether the value of
     * <i>expression</i> is negative, zero, or positive, respectively.
     *
     * @param object the object to be compared.
     * @return a negative integer, zero, or a positive integer as this object
     * is less than, equal to, or greater than the specified object.
     * @throws NullPointerException if the specified object is null
     * @throws ClassCastException   if the specified object's type prevents it
     *                              from being compared to this object.
     */
    @Override
    public int compareTo(Object object) {
        if( object == null ) throw new NullPointerException("Can't compare a null object to this ColumnFeatures object");
        if( object instanceof ColumnFeatures ) {
            ColumnFeatures otherColumnFeatures = (ColumnFeatures) object;
            if (
                this.name.equals(otherColumnFeatures.name) &&
                this.size == otherColumnFeatures.size &&
                this.type.equals(otherColumnFeatures.type) && this.isNull == otherColumnFeatures.isNull
            )
                return 0;
        } else {
            throw new ClassCastException("compare needs another class of type ColumnFeatures");
        }
        return -1;
    }

    public String toString() { return name +" "+ printConstraints(); }
}
