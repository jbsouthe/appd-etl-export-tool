FROM adoptopenjdk/openjdk11:latest
#version and build date for the deployment file, which should be copied to this directory for building
ENV VERSION 1.12
ENV BUILD_DATE 20230207
ENV CONFIG_FILE /config/etl-tool-config.xml
COPY appdynamics-ETL-Tool-${VERSION}-${BUILD_DATE}-deployment.tar.gz /tmp
RUN tar xzvf /tmp/appdynamics-ETL-Tool-${VERSION}-${BUILD_DATE}-deployment.tar.gz
WORKDIR /appdynamics-ETL-Tool-${VERSION}-${BUILD_DATE}/ETL-Tool
ENTRYPOINT java ${JAVA_OPT} -jar ETLExportTool.jar -c ${CONFIG_FILE}
#CMD ["/config/etl-tool-config.xml"]
