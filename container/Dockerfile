FROM adoptopenjdk/openjdk11:latest
#version and build date for the deployment file, which should be copied to this directory for building
ENV VERSION 1.2
ENV BUILD_DATE 20220714
COPY appdynamics-ETL-Tool-${VERSION}-${BUILD_DATE}-deployment.tar.gz /tmp
RUN tar xzvf /tmp/appdynamics-ETL-Tool-${VERSION}-${BUILD_DATE}-deployment.tar.gz
WORKDIR /appdynamics-ETL-Tool-${VERSION}-${BUILD_DATE}/ETL-Tool
CMD ["java", "-jar", "ETLExportTool.jar", "/config/etl-tool-config.xml"]
