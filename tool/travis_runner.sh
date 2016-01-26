#!/usr/bin/env bash

set -e
set -x

./mvnw install -B -Dinvoker.skip=false $PHASE | egrep -v 'Download|\\[exec\\] [[:digit:]]+/[[:digit:]]+|^[[:space:]]*\\[exec\\][[:space:]]*$'

MVN_STATUS=${PIPESTATUS[0]}

if [ $MVN_STATUS != 0 ]
then
  exit $MVN_STATUS
fi

if [[ -v COMMAND ]]
then
  $COMMAND
fi

if [[ -v JT ]]
then
  ruby tool/jt.rb $JT
fi
