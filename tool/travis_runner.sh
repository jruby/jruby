#!/usr/bin/env bash

set -e
set -x

if [[ -v PHASE ]]
then
  DOWNLOAD_OUTPUT_FILTER='Download|\\[exec\\] [[:digit:]]+/[[:digit:]]+|^[[:space:]]*\\[exec\\][[:space:]]*$'
  if [[ $JAVA_HOME == *"java-8"* ]]
  then
    ./mvnw package -B --projects '!truffle' -Dinvoker.skip=false $PHASE | egrep -v "$DOWNLOAD_OUTPUT_FILTER"
  else
    ./mvnw package -B -Dinvoker.skip=false $PHASE | egrep -v "$DOWNLOAD_OUTPUT_FILTER"
  fi

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

if [[ -v JT ]]
then
  ruby tool/jt.rb $JT
fi
