# AppDynamics ETL Export Tool

The purpose of this utility is to run an export of AppDynamics Metrics, Events, and Analytics Searches and insert that data into either a database or Comma Separated Value file.
One of the benefits of this utility is that it doesn't require any database schema creation, and will automatically create tables and columns as needed, dynamically during execution.

Supported Databases:
* Oracle
* MySQL
* PostgreSQL
* CSV Files

The execution of this utility requires a Java VM v1.11 or greater

## Executing the utility:

The command line to execute is:

`java -jar ETLExportTool-<version>.jar <configfile.xml>`

It is assumed the XML config file is the first and only argument. Also expected that a log4j2.xml is in the current working directory

### Running in Kubernetes

This utility can be run as a kubernetes pod by either using the public image "johnsoutherland/appdynamics-etl-tool:1.2", or creating a custom image in your own hosting environment. Check the examples in the ./container directory, or use this information. https://hub.docker.com/r/johnsoutherland/appdynamics-etl-tool/tags

#### Create local Docker image, Optional

Dockerfile:

    FROM adoptopenjdk/openjdk11:latest
    #version and build date for the deployment file, which should be copied to this directory for building
    ENV VERSION 1.2
    ENV BUILD_DATE 20220714
    ENV CONFIG_FILE /config/etl-tool-config.xml
    COPY appdynamics-ETL-Tool-${VERSION}-${BUILD_DATE}-deployment.tar.gz /tmp
    RUN tar xzvf /tmp/appdynamics-ETL-Tool-${VERSION}-${BUILD_DATE}-deployment.tar.gz
    WORKDIR /appdynamics-ETL-Tool-${VERSION}-${BUILD_DATE}/ETL-Tool
    ENTRYPOINT java ${JAVA_OPT} -jar ETLExportTool.jar ${CONFIG_FILE}


Create the image, and publish it to your repo. Of course the ./target/appdynamics-ETL-Tool-${VERSION}-${BUILD_DATE}-deployment.tar.gz file must be copied to your docker build dir, the release tar.gz is the image we are talking about in this section.

#### ConfigMap of XML Config File

Prepare a ConfigMap of the XML configuration file by creating a configuration and then naming it, for example, "etl-tool-config.xml" and create a configmap from file named for your configuration

    kubectl create configmap csv-etl-config --from-file=etl-tool-config.xml

#### Deployment Descriptor, with public image

now make a deployment for this, using this example and modifying it to your liking:

    apiVersion: v1
    kind: Pod
    metadata:
     name: appd-etl-tool
    spec:
        containers:
        - name: appd-etl-tool
          image: johnsoutherland/appdynamics-etl-tool:1.3
        env:
        - name: CONFIG_FILE 
          value: "/config/etl-tool-config.xml"
        - name: JAVA_OPT
          value: "-XX:+TraceClassLoading -verbose:class"
          imagePullPolicy: Always
        resources:
            requests:
                memory: "1024Mi"
                cpu: "2"
        volumeMounts:
        - name: my-config
          mountPath: /config
        volumes:
        - name: my-config
          configMap:
          name: csv-etl-config

Make sure the config file name is correct to your ConfigMap
If JAVA_OPTS are needed for something like Proxy Configuration, as below

### Proxy Configuration

If proxy support is required, set the following arguments before the -jar arguement:
    
     -Djava.net.useSystemProxies=true

or, to manually specify the host and port:
     
     -Dhttp.proxyHost=PROXY_HOST
     -Dhttp.proxyPort=PROXY_PORT

or, to manually specify the host, port, and basic user and password:

     -Dhttp.proxyHost=PROXY_HOST
     -Dhttp.proxyPort=PROXY_PORT
     -Dhttp.proxyUser=USERNAME
     -Dhttp.proxyPassword=PASSWORD

or, to manually specify the host, port, and NTLM authentication:

     -Dhttp.proxyHost=PROXY_HOST
     -Dhttp.proxyPort=PROXY_PORT
     -Dhttp.proxyUser=USERNAME
     -Dhttp.proxyPassword=PASSWORD
     -Dhttp.proxyWorkstation=HOSTNAME
     -Dhttp.proxyDomain=NT_DOMAIN

## Configure Logging

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

## Configure the Extraction Tool

For this utility to run, a configuration must be in place that is minimally viable, and if it is not, then we will exit with helpful messages.
This file is XML format with specific sections and options that those sections can use to override defaults and apply to other sections in heiarchy.

The sections are assumed to be in this order:

    <ETLTool>
        <Scheduler></Scheduler>
        <TargetDB></TargetDB>
        <Controller></Controller> <!-- can be repeated -->
        <Analytics></Analytics> <!-- can be repeated -->
    </ETLTool>

### Scheduler Section

This section configures whether the utility runs only one iteration and exits, or sleeps and runs again repeatedly.
Also, we can specify how much historical data to load if no previous run has been detected, otherwise we only load data since that last run.

The default settings are shown:

    <Scheduler enabled="false">
        <PollIntervalMinutes>60</PollIntervalMinutes>
        <FirstRunHistoricNumberOfHours>48</FirstRunHistoricNumberOfHours>
        <ControllerThreads>50</ControllerThreads>
        <DatabaseThreads>50</DatabaseThreads>
        <ConfigurationRefreshEveryHours>12</ConfigurationRefreshEveryHours>
    </Scheduler>

* enabled=false causes the Scheduler to exit after one run, true is the default, when this option is missing.
* PollIntervalMinutes 60, if enabled, causes the Scheduler to sleep for 60 minutes and run again, continuously
* FirstRunHistoricNumberOfHours 48, causes the extract to default in pulling the last 48 hours of data for a given query, if no previous run is detected in the control table, specified in the database section later
* ControllerThreads 50, the number of threads to run pulling data from the controllers concurrently
* DatabaseThreads 50, the number of worker threads watching the data queue to insert into the database
* ConfigurationRefreshEveryHours 12, after this many hours, all the applications with the configuration setting to pull all metrics, will refresh the metrics in case new ones are registered since start.

### TargetDB Section

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
        <DefaultBaselineTable>AppDynamics_BaselineTable</DefaulBaselineTable>
    </TargetDB>

* ConnectionString is the JDBC connection string, or a string like the following for CSV file creation: "csv:none:\<data directory to create files in>"
* User and Password are needed for the database connection, this is ignored for CSV file creation
* ControlTable is the table name to use for tracking last run timestamp for the different data being extracted
* DefaultMetricTable is used when the Metric section below doesn't specify another more specific table for that data or application
* DefaultEventTable is used for events when no other table is specified, similar to the DefaultMetricTable above
* DefaultBaselineTable is used when the Application has not specified a table for baseline data

### Controller Section

Multiple Controller Sections can be defined, but the url must be unique.

    <Controller getAllAnalyticsSearches="false">
        <URL>https://southerland-test.saas.appdynamics.com/</URL>
        <ClientID>ETLClient@southerland-test</ClientID>
        <ClientSecret>the generated client secret</ClientSecret>
        <Application getAllAvailableMetrics="true" getAllEvents="false"> <!-- these are the default options in default values -->
            <Name>Agent Proxy</Name>
            <Defaults>
                <DisableDataRollup>true</DisableDataRollup>
                <MetricTable>ProxyAppMetrics</MetricTable>
                <EventTable>ProxyAppEvents</EventTable>
                <BaselineTable>ProxyBaseLines</BaselineTable>
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

### Analytics Section

Multiple Analytics Sections can be defined, but the Global Account Name must be unique for each.

    <Analytics>
        <URL>https://analytics.api.appdynamics.com/</URL> <!-- north america Saas -->
        <GlobalAccountName>southerland-test_65322e21-efed-4126-8827-920141a9ac21</GlobalAccountName>
        <APIKey>generate your own key</APIKey> <!-- API Key is created in analytics configuration settings -->
        <TableNamePrefix>AppDynamics_Analytics_</TableNamePrefix> <!-- this is the prefix table name for data extracted, final table is <PrefixTableName><Search name> -->
        <LinkToControllerHostname>southerland-test.saas.appdynamics.com</LinkToControllerHostname>
        <Search name="UniqueTransactionCount" limit="20000">SELECT transactionName, count(*) FROM transactions</Search> <!--limit is optional and defaults to 20000, name must be unique for this section -->
    </Analytics>
