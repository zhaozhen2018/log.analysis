#!/bin/bash

process=$(ps -ef | grep bdap.pushagent-r0.6.0.jar | grep -v grep)
if [ -z "$process" ];
then
	echo "Starting push agent." 
	java -jar bdap.pushagent-r0.6.0.jar file://$PWD/config.json >> log.log 2>&1 &
	echo "Push agent is started."
else
	echo "Push agent already running."
	exit 1
fi