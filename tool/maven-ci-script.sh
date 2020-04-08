#!/usr/bin/env bash

set -e
set -x

if [[ -v PHASE ]]
then
  DOWNLOAD_OUTPUT_FILTER='Download|\\[exec\\] [[:digit:]]+/[[:digit:]]+|^[[:space:]]*\\[exec\\][[:space:]]*$'
  ./mvnw $MAVEN_CLI_OPTS package -B -Dinvoker.skip=false $PHASE | egrep -v "$DOWNLOAD_OUTPUT_FILTER"

  MVN_STATUS=${PIPESTATUS[0]}

  if [ $MVN_STATUS != 0 ]
  then
    exit $MVN_STATUS
  fi
fi

if [[ -v COMMAND ]]
then
  $COMMAND
fi
