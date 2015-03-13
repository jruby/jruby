#!/bin/bash

# Checks that the artefacts produced by the JRuby build system have the correct
# names and versions.

jar_version=`cat VERSION`
gem_version=${jar_version/-/.}

rm -rf maven/*/target/*

mvn install -Pbootstrap
mvn -Pcomplete
mvn -Pdist
mvn -Pjruby-jars
mvn -Pjruby-tests
mvn -Pmain

declare -a failed
failed[0]=0

function check {
  archive=$1
  unpackaged=$2
  
  if [ ! -f $archive ]
  then
    echo $archive was not found - check your version numbers
    failed[0]=1
  fi

  if [[ $archive == *.tar.gz ]]
  then
    rm -rf $unpackaged
    tar -zxf $archive

    if [ ! -d $unpackaged ]
    then
      echo $archive did not untar to $unpackaged - check your version numbers
      failed[0]=1
    fi
  fi

  if [[ $archive == *.zip ]]
  then
    rm -rf $unpackaged
    unzip -q $archive

    if [ ! -d $unpackaged ]
    then
      echo $archive did not unzip to $unpackaged - check your version numbers
      failed[0]=1
    fi
  fi
}

check test/target/jruby-tests-$jar_version.jar
check maven/jruby-stdlib/target/jruby-stdlib-$jar_version.jar
check maven/jruby-jars/pkg/jruby-jars-$gem_version.gem
check maven/jruby-jars/lib/jruby-core-$jar_version-complete.jar
check maven/jruby-jars/lib/jruby-truffle-$jar_version-complete.jar
check maven/jruby-jars/lib/jruby-stdlib-$jar_version.jar
check maven/jruby-complete/target/jruby-complete-$jar_version.jar
check maven/jruby/target/jruby-$jar_version.jar
check maven/jruby-noasm/target/jruby-noasm-$jar_version.jar
check maven/jruby-dist/target/jruby-dist-$jar_version-bin.tar.gz jruby-$jar_version
check maven/jruby-dist/target/jruby-dist-$jar_version-src.tar.gz jruby-$jar_version
check maven/jruby-dist/target/jruby-dist-$jar_version-src.zip jruby-$jar_version
check maven/jruby-dist/target/jruby-dist-$jar_version-bin.zip jruby-$jar_version
check core/target/jruby-core-$jar_version-noasm.jar
check core/target/jruby-core-$jar_version.jar
check core/target/jruby-core-$jar_version-complete.jar
check truffle/target/jruby-truffle-$jar_version.jar
check truffle/target/jruby-truffle-$jar_version-complete.jar

exit "${failed[0]}"
