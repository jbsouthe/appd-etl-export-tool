package com.cisco.josouthe.exceptions;

import java.io.IOException;

public class ControllerBadStatusException extends IOException {
    public String urlRequestString, responseJSON;
    public ControllerBadStatusException( String message, String json, String url) {
        super(message);
        this.responseJSON = json;
        this.urlRequestString = url;
    }

    public void setURL(String uri) {
        this.urlRequestString=uri;
    }
}
