#!/bin/bash
export JAVA_HOME=/Users/jonathan/.sdkman/candidates/java/current
export THEOLOGY_DB_PATH=/Users/jonathan/projects/theology-tracker-java/data/theology.db
exec /Users/jonathan/.sdkman/candidates/maven/3.9.9/bin/mvn spring-boot:run
