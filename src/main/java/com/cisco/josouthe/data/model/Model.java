package com.cisco.josouthe.data.model;

public class Model {
    private Application[] applications;
    public Model( Application[] apps ) { this.applications=apps; }
    public Application getApplication( String name ) {
        for( Application application : applications )
            if( application.name.equals(name)) return application;
        return null;
    }

    public Application[] getApplications() { return applications; }
}
