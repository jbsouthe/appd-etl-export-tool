#!/bin/sh  -x
kubectl create configmap csv-etl-config --from-file=etl-tool-config.xml
