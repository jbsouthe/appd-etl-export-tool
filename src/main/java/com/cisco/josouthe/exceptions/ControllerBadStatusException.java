package com.cisco.josouthe.exceptions;

public class ControllerBadStatusException extends Exception{
    public String urlRequestString, responseJSON;
    public ControllerBadStatusException( String message, String json, String url) {
        super(message);
        this.responseJSON = json;
        this.urlRequestString = url;
    }
}
