#!/bin/bash

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
  java -cp $CP com.cisco.josouthe.ETLTransferMain $2
fi
