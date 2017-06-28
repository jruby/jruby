#!/usr/bin/env bash

set -e
set -x

if [[ -n "$PHASE" && $JAVA_HOME == *"java-8"* ]]
then
  ./mvnw $MAVEN_CLI_OPTS package -B -Dinvoker.skip -Dmaven.test.skip;
else
  if [ -z "$SKIP_BUILD" ]; then ./mvnw $MAVEN_CLI_OPTS package -B -Dinvoker.skip -Dmaven.test.skip; fi
fi
