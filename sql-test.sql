create table AppDynamics_DefaultTable (
    occurrences number NOT NULL,
    controller varchar2(50) NOT NULL,
    max number NOT NULL,
    count number NOT NULL,
    sum number NOT NULL,
    standarddeviation number NOT NULL,
    frequency varchar2(50) NOT NULL,
    currentValue number NOT NULL,
    min number NOT NULL,
    application varchar2(50) NOT NULL,
    metricid number NOT NULL,
    userange number NOT NULL,
    starttimeinmillis number NOT NULL,
    metricname varchar2(200) NOT NULL,
    metricpath varchar2(200) NOT NULL,
    value number NOT NULL
)