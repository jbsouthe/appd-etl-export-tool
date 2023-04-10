#!/bin/bash

#java options
export JAVA_OPTS="-XX:+UseParallelGC -XX:ParallelGCThreads=10 -XX:MaxGCPauseMillis=1000 -XX:MaxRAMPercentage=75 "
#no options: real	13m47.677s user	0m27.232s sys	0m3.827s
#"-XX:+UseParallelGC -XX:ParallelGCThreads=10 -XX:MaxGCPauseMillis=1000 -XX:MaxRAMPercentage=75 " options: real	13m33.691s user	0m27.780s sys	0m3.613s


#Trace analytics
#export DEBUGARGS="-DwireTrace=analytics"
#export DEBUGARGS="-DwireTrace=controller"

CP=".:$1"
for i in ./lib/*.jar
do
  export CP=${i}:$CP
done

set -x
if [[ "$2" == "test" ]]; then
#  java -cp $CP com.cisco.josouthe.data.Controller https://diners-nonprod.saas.appdynamics.com/ ETLAPIClient@diners-nonprod 4d42e388-01ef-40a3-acac-bca549608266
  java -cp $CP com.cisco.josouthe.data.Analytics
#  java -cp $CP com.cisco.josouthe.data.metric.graph.MetricGraph $3
else
  time java $DEBUGARGS $JAVA_OPTS -cp $CP com.cisco.josouthe.ETLTransferMain $2
fi
