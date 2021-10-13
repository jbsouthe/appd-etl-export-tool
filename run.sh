#!/bin/bash

CP=".:$1"
for i in ./lib/*.jar
do
  export CP=${i}:$CP
done

set -x
if [[ "$2" == "test" ]]; then
#  java -cp $CP com.cisco.josouthe.data.Controller
  java -cp $CP com.cisco.josouthe.data.Analytics
else
  java -cp $CP com.cisco.josouthe.ETLTransferMain $2
fi
