package com.cisco.josouthe.data.model;

import java.util.ArrayList;
import java.util.List;

public class ApplicationListing {
    public Application analyticsApplication, cloudMonitoringApplication, dbMonApplication, simApplication;
    public List<Application> apiMonitoringApplications, apmApplications, eumWebApplications, iotApplications, mobileAppContainers, _allApplications;

    public List<Application> getApplications() {
        if( _allApplications == null ) {
            _allApplications = new ArrayList<>();
            if( analyticsApplication != null ) _allApplications.add(analyticsApplication);
            if( apiMonitoringApplications != null ) _allApplications.addAll(apiMonitoringApplications);
            if( apmApplications != null ) _allApplications.addAll(apmApplications);
            if( cloudMonitoringApplication != null ) _allApplications.add(cloudMonitoringApplication);
            if( dbMonApplication != null ) _allApplications.add(dbMonApplication);
            if( eumWebApplications != null ) _allApplications.addAll(eumWebApplications);
            if( mobileAppContainers != null ) _allApplications.addAll(mobileAppContainers);
            if( simApplication != null ) _allApplications.add(simApplication);
        }
        return _allApplications;
    }
}
