package com.cisco.josouthe.exceptions;

public class BadCommandException extends Exception{
    public BadCommandException(String s, String c) {
        super(String.format("%s, bad command part: '%s'",s,c));
    }
}
