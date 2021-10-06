package com.cisco.josouthe.data;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

/*
Events Reference: https://docs.appdynamics.com/21.10/en/appdynamics-essentials/monitor-events/events-reference
Retreive Events: https://docs.appdynamics.com/21.10/en/extend-appdynamics/appdynamics-apis/alert-and-respond-api/events-and-action-suppression-api#EventsandActionSuppressionAPI-RetrieveEventData
 */
public class EventData {
    private static final Logger logger = LogManager.getFormatterLogger();
    public EventData() {}

    public String targetTable = null;
    public String controllerHostname;
    public String applicationName;

    public long id, eventTime;
    public String type, subType, severity, summary;
    public EntityDefinition triggeredEntity;
    public List<EntityDefinition> affectedEntities;

}
