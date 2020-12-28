#!/bin/bash
git pull
mvn package -Dmaven.test.skip=true
jps -l | grep czc | awk '{print $1}' | xargs kill -9
nohup java -jar target/czc.jar --spring.config.location=target/application.yml &
tail -20f nohup.out