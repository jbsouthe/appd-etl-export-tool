# AppDynamics ETL Export Tool

The purpose of this utility is to run an export of AppDynamics Metrics, Events, and Analytics Searches and insert that data into either a database or Comma Separated Value file.
One of the benefits of this utility is that it doesn't require any database schema creation, and will automatically create tables and columns as needed, dynamically during execution.

The execution of this utility requires a Java VM v1.8 or greater, and the following external libraries:

###Google GSON - for JSON conversion to java classes

    <dependency>
        <groupId>com.google.code.gson</groupId>
        <artifactId>gson</artifactId>
        <version>2.8.8</version>
    </dependency>
* asm-3.3.1.jar
* cglib-2.2.2.jar
* gson-2.8.8.jar

###Apache HTTP Client - for restful API requests to the AppDynamics Controller

    <dependency>
        <groupId>org.apache.httpcomponents</groupId>
        <artifactId>httpclient</artifactId>
        <version>4.5.13</version>
    </dependency>

* httpclient-4.5.13.jar
* httpcore-4.4.13.jar

###Apache Commons Digester - for XML Configuration file processing

    <dependency>
        <groupId>org.apache.commons</groupId>
        <artifactId>commons-digester3</artifactId>
        <version>3.2</version>
    </dependency>
* commons-beanutils-1.8.3.jar
* commons-codec-1.11.jar
* commons-digester3-3.2.jar
* commons-logging-1.1.1.jar

###Log4j - for formatted log messages

    <dependency>
        <groupId>org.apache.logging.log4j</groupId>
        <artifactId>log4j-api</artifactId>
        <version>2.14.1</version>
    </dependency>
    <dependency>
        <groupId>org.apache.logging.log4j</groupId>
        <artifactId>log4j-core</artifactId>
        <version>2.14.1</version>
    </dependency>
* log4j-api-2.14.1.jar
* log4j-core-2.14.1.jar

###Oracle JDBC Driver (needed if connecting to an Oracle DB)

    <dependency>
        <groupId>com.oracle.database.jdbc</groupId>
        <artifactId>ojdbc8</artifactId>
        <version>19.3.0.0</version>
    </dependency>
* ojdbc8-19.3.0.0.jar
* ons-19.3.0.0.jar
* oraclepki-19.3.0.0.jar
* osdt_cert-19.3.0.0.jar
* osdt_core-19.3.0.0.jar
* simplefan-19.3.0.0.jar
* ucp-19.3.0.0.jar

##Executing the utility requires:
The libraries above are expected in the <current directory>/lib directory and the jar file has this classpath as a default, if a library is upgraded then the classpath will need to be updated as well or overridden.

The command line to execute is:

`java -jar ETLExportTool-<version>.jar <configfile.xml>`

It is assumed the XML config file is the first and only argument. Also expected that a log4j2.xml is in the current working directory

Here is an example, simple log4j2.xml file as a starting point:

    <?xml version="1.0" encoding="UTF-8"?>
    <Configuration status="WARN">
      <Appenders>
        <Console name="Console" target="SYSTEM_OUT">
          <PatternLayout pattern="%d{HH:mm:ss.SSS} [%t] %-5level %logger{36} - %msg%n"/>
        </Console>
      </Appenders>
      <Loggers>
        <Logger name="com.cisco.josouthe" level="trace" additivity="false">
          <AppenderRef ref="Console"/>
        </Logger>
        <Root level="info">
          <AppenderRef ref="Console"/>
        </Root>
      </Loggers>
    </Configuration>

##Configure the Extraction Tool

For this utility to run, a configuration must be in place that is minimally viable, and if it is not, then we will exit with helpful messages.
This file is XML format with specific sections and options that those sections can use to override defaults and apply to other sections in heiarchy.

The sections are assumed to be in this order:

    <ETLTool>
        <Scheduler></Scheduler>
        <TargetDB></TargetDB>
        <Controller></Controller> <!-- can be repeated -->
        <Analytics></Analytics> <!-- can be repeated -->
    </ETLTool>

#Scheduler Section
This section configures whether the utility runs only one iteration and exits, or sleeps and runs again repeatedly.
Also, we can specify how much historical data to load if no previous run has been detected, otherwise we only load data since that last run.

The default settings are shown:

    <Scheduler enabled="false">
        <PollIntervalMinutes>60</PollIntervalMinutes>
        <FirstRunHistoricNumberOfHours>48</FirstRunHistoricNumberOfHours>
    </Scheduler>

* enabled=false causes the scheduler to exit after one run, true is the default, when this option is missing.
* PollIntervalMinutes 60, if enabled, causes the scheduler to sleep for 60 minutes and run again, continuously
* FirstRunHistoricNumberOfHours 48, causes the extract to default in pulling the last 48 hours of data for a given query, if no previous run is detected in the control table, specified in the database section later

#TargetDB Section
This section configures the destination for the extracted data. 
We support only Oracle and CSV Files as of this document's writing.
Other databases will need some development for SQL compatibility and of course different JDBC drivers to be included on the class path.
Only one target database is allowed, and it is required for execution. 
If not available for testing please configure a CSV output directory as per the notes below.

An example config is shown with some default options specified:

    <TargetDB>
        <ConnectionString>jdbc:oracle:thin:@localhost:1521:orcl</ConnectionString>
        <User>username</User>
        <Password>password</Password>
        <ControlTable>AppDynamics_SchedulerControl</ControlTable>
        <DefaultMetricTable>AppDynamics_DefaultTable</DefaultMetricTable>
        <DefaultEventTable>AppDynamics_EventTable</DefaultEventTable>
    </TargetDB>

* ConnectionString is the JDBC connection string, or a string like the following for CSV file creation: "csv:none:\<data directory to create files in>"
* User and Password are needed for the database connection, this is ignored for CSV file creation
* ControlTable is the table name to use for tracking last run timestamp for the different data being extracted
* DefaultMetricTable is used when the Metric section below doesn't specify another more specific table for that data or application
* DefaultEventTable is used for events when no other table is specified, similar to the DefaultMetricTable above

#Controller Section
Multiple Controller Sections can be defined, but the url must be unique.

    <Controller>
        <URL>https://southerland-test.saas.appdynamics.com/</URL>
        <ClientID>ETLClient@southerland-test</ClientID>
        <ClientSecret>the generated client secret</ClientSecret>
        <Application getAllAvailableMetrics="true" getAllEvents="false"> <!-- these are the default options in default values -->
            <Name>Agent Proxy</Name>
            <Defaults>
                <DisableDataRollup>true</DisableDataRollup>
                <MetricTable>ProxyAppMetrics</MetricTable>
                <EventTable>ProxyAppEvents</EventTable>
            </Defaults>
            <!-- if so desired, the events can be filtered by including or excluding events from the internal event list,
            and by limitting the severity. The default is shown here, to just get all event types and severities, leave these blank or missing
            <Events>
                <Include>ACTIVITY_TRACE,ADJUDICATION_CANCELLED,AGENT_ADD_BLACKLIST_REG_LIMIT_REACHED,AGENT_ASYNC_ADD_REG_LIMIT_REACHED,AGENT_CONFIGURATION_ERROR,APPLICATION_CRASH,AGENT_DIAGNOSTICS,AGENT_ERROR_ADD_REG_LIMIT_REACHED,AGENT_EVENT,AGENT_METRIC_BLACKLIST_REG_LIMIT_REACHED,AGENT_METRIC_REG_LIMIT_REACHED,AGENT_STATUS,ALREADY_ADJUDICATED,APPDYNAMICS_DATA,APPDYNAMICS_INTERNAL_DIAGNOSTICS,APPLICATION_CONFIG_CHANGE,APPLICATION_DEPLOYMENT,APPLICATION_DISCOVERED,APPLICATION_ERROR,APP_SERVER_RESTART,AZURE_AUTO_SCALING,BACKEND_DISCOVERED,BT_DISCOVERED,BUSINESS_ERROR,CLR_CRASH,CONTROLLER_AGENT_VERSION_INCOMPATIBILITY,CONTROLLER_ASYNC_ADD_REG_LIMIT_REACHED,CONTROLLER_COLLECTIONS_ADD_REG_LIMIT_REACHED,CONTROLLER_ERROR_ADD_REG_LIMIT_REACHED,CONTROLLER_EVENT_UPLOAD_LIMIT_REACHED,CONTROLLER_MEMORY_ADD_REG_LIMIT_REACHED,CONTROLLER_METADATA_REGISTRATION_LIMIT_REACHED,CONTROLLER_METRIC_DATA_BUFFER_OVERFLOW,CONTROLLER_METRIC_REG_LIMIT_REACHED,CONTROLLER_PSD_UPLOAD_LIMIT_REACHED,CONTROLLER_RSD_UPLOAD_LIMIT_REACHED,CONTROLLER_SEP_ADD_REG_LIMIT_REACHED,CONTROLLER_STACKTRACE_ADD_REG_LIMIT_REACHED,CONTROLLER_TRACKED_OBJECT_ADD_REG_LIMIT_REACHED,CUSTOM,CUSTOM_ACTION_END,CUSTOM_ACTION_FAILED,CUSTOM_ACTION_STARTED,CUSTOM_EMAIL_ACTION_END,CUSTOM_EMAIL_ACTION_FAILED,CUSTOM_EMAIL_ACTION_STARTED,DEADLOCK,DEV_MODE_CONFIG_UPDATE,DIAGNOSTIC_SESSION,DISK_SPACE,EMAIL_ACTION_FAILED,EMAIL_SENT,EUM_CLOUD_BROWSER_EVENT,EUM_CLOUD_SYNTHETIC_BROWSER_EVENT,EUM_CLOUD_SYNTHETIC_ERROR_EVENT,EUM_CLOUD_SYNTHETIC_CONFIRMED_ERROR_EVENT,EUM_CLOUD_SYNTHETIC_ONGOING_ERROR_EVENT,EUM_CLOUD_SYNTHETIC_HEALTHY_EVENT,EUM_CLOUD_SYNTHETIC_WARNING_EVENT,EUM_CLOUD_SYNTHETIC_CONFIRMED_WARNING_EVENT,EUM_CLOUD_SYNTHETIC_ONGOING_WARNING_EVENT,EUM_CLOUD_SYNTHETIC_PERF_WARNING_EVENT,EUM_CLOUD_SYNTHETIC_PERF_CONFIRMED_WARNING_EVENT,EUM_CLOUD_SYNTHETIC_PERF_ONGOING_WARNING_EVENT,EUM_CLOUD_SYNTHETIC_PERF_HEALTHY_EVENT,EUM_CLOUD_SYNTHETIC_PERF_CRITICAL_EVENT,EUM_CLOUD_SYNTHETIC_PERF_CONFIRMED_CRITICAL_EVENT,EUM_CLOUD_SYNTHETIC_PERF_ONGOING_CRITICAL_EVENT,EUM_INTERNAL_ERROR,HTTP_REQUEST_ACTION_END,HTTP_REQUEST_ACTION_FAILED,HTTP_REQUEST_ACTION_STARTED,INFO_INSTRUMENTATION_VISIBILITY,INTERNAL_UI_EVENT,KUBERNETES,LICENSE,MACHINE_AGENT_LOG,MACHINE_DISCOVERED,MEMORY,MEMORY_LEAK_DIAGNOSTICS,MOBILE_CRASH_IOS_EVENT,MOBILE_CRASH_ANDROID_EVENT,NETWORK,NODE_DISCOVERED,NORMAL,OBJECT_CONTENT_SUMMARY,POLICY_CANCELED_CRITICAL,POLICY_CANCELED_WARNING,POLICY_CLOSE_CRITICAL,POLICY_CLOSE_WARNING,POLICY_CONTINUES_CRITICAL,POLICY_CONTINUES_WARNING,POLICY_DOWNGRADED,POLICY_OPEN_CRITICAL,POLICY_OPEN_WARNING,POLICY_UPGRADED,RESOURCE_POOL_LIMIT,RUN_LOCAL_SCRIPT_ACTION_END,RUN_LOCAL_SCRIPT_ACTION_FAILED,RUN_LOCAL_SCRIPT_ACTION_STARTED,SERVICE_ENDPOINT_DISCOVERED,SLOW,SMS_SENT,STALL,SYSTEM_LOG,THREAD_DUMP_ACTION_END,THREAD_DUMP_ACTION_FAILED,THREAD_DUMP_ACTION_STARTED,TIER_DISCOVERED,VERY_SLOW,WARROOM_NOTE</Include>
                <Exclude></Exclude>
                <Severities>INFO,WARN,ERROR</Severities>
            </Events>
            -->
           <!-- Metrics can be included manually, and wildcards are supported, the getAllAvailable metrics flag for the application must be false to specify individual metrics: 
            <Metric>Business Transaction Performance|Business Transactions|ProxyTier|*|Average Response Time (ms)</Metric>
            -->
        </Application>
    </Controller>

#Analytics Section
Multiple Analytics Sections can be defined, but the Global Account Name must be unique for each.

    <Analytics>
        <URL>https://analytics.api.appdynamics.com/</URL> <!-- north america Saas -->
        <GlobalAccountName>southerland-test_65322e21-efed-4126-8827-920141a9ac21</GlobalAccountName>
        <APIKey>generate your own key</APIKey> <!-- API Key is created in analytics configuration settings -->
        <TableNamePrefix>AppDynamics_Analytics_</TableNamePrefix> <!-- this is the prefix table name for data extracted, final table is <PrefixTableName><Search name> -->
        <Search name="UniqueTransactionCount" limit="20000">SELECT transactionName, count(*) FROM transactions</Search> <!--limit is optional and defaults to 20000, name must be unique for this section -->
    </Analytics>