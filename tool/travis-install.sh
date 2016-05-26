#!/usr/bin/env bash

set -e
set -x

if [[ -v USE_BUILD_PACK ]]
then
  git clone --depth 1 https://github.com/jruby/jruby-build-pack.git
  MAVEN_CLI_OPTS="-Dmaven.repo.local=jruby-build-pack/maven --offline"
fi

if [[ -n "$PHASE" && $JAVA_HOME == *"java-8"* ]]
then
  ./mvnw $MAVEN_CLI_OPTS package -B --projects '!truffle' -Dinvoker.skip -Dmaven.test.skip;
else
  if [ -z "$SKIP_BUILD" ]; then ./mvnw $MAVEN_CLI_OPTS package -B -Dinvoker.skip -Dmaven.test.skip; fi
fi
