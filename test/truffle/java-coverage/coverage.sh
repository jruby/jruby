#!/bin/bash

curl -L -o jacoco-agent.jar 'http://search.maven.org/remotecontent?filepath=org/jacoco/org.jacoco.agent/0.7.4.201502262128/org.jacoco.agent-0.7.4.201502262128-runtime.jar'
curl -L -O 'http://central.maven.org/maven2/org/ow2/asm/asm-all/5.0.4/asm-all-5.0.4.jar'
curl -L -O 'http://central.maven.org/maven2/org/jacoco/org.jacoco.core/0.7.4.201502262128/org.jacoco.core-0.7.4.201502262128.jar'
curl -L -O 'http://central.maven.org/maven2/org/jacoco/org.jacoco.ant/0.7.4.201502262128/org.jacoco.ant-0.7.4.201502262128.jar'
curl -L -O 'http://central.maven.org/maven2/org/jacoco/org.jacoco.report/0.7.4.201502262128/org.jacoco.report-0.7.4.201502262128.jar'

JACOCO_AGENT=`pwd`/jacoco-agent.jar
JACOCO_LOG=`pwd`/jacoco.exec

export VERIFY_JRUBY=1
export JAVA_OPTS="-javaagent:$JACOCO_AGENT=destfile=$JACOCO_LOG"

rm jacoco.exec
tool/jt.rb test
test/truffle/integration-tests.sh
ant -f test/truffle/java-coverage/build.xml
