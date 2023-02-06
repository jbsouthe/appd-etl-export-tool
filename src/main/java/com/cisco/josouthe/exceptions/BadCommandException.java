package com.cisco.josouthe.exceptions;

public class BadCommandException extends Exception{
    public BadCommandException(String message, String part) {
        super(String.format("%s, bad command part: '%s'",message,part));
    }
    public BadCommandException( String message ) {
        super(message);
    }
}
