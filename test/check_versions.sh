#!/bin/bash

# Checks that the artefacts produced by the JRuby build system have the correct
# names and versions, and have reasonable sizes.

jar_version=`cat VERSION`
gem_version=${jar_version/-/.}

rm -rf maven/*/target/*

./mvnw -ntp install -Pbootstrap
./mvnw -ntp -Pcomplete
./mvnw -ntp -Pdist
./mvnw -ntp -Pjruby-jars
./mvnw -ntp -Pmain

declare -a failed
failed[0]=0

function check {
  archive=$1
  max=$2*1024*1024
  unpackaged=$3
  length=`cat $archive | wc -c`

  if [ ! -f $archive ]
  then
    echo $archive was not found - check your version numbers
    failed[0]=1
  fi

  if [[ $length -gt $max  ]]
  then
    echo size of $archive expected smaller than $max but got $length
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

check lib/target/jruby-stdlib-$jar_version.jar 20
check maven/jruby-jars/pkg/jruby-jars-$gem_version.gem 39
check maven/jruby-jars/lib/jruby-core-$jar_version-complete.jar 22
check maven/jruby-jars/lib/jruby-stdlib-$jar_version.jar 20
check maven/jruby-complete/target/jruby-complete-$jar_version.jar 42
check maven/jruby/target/jruby-$jar_version.jar 9
check maven/jruby-dist/target/jruby-dist-$jar_version-bin.tar.gz 45 jruby-$jar_version
check maven/jruby-dist/target/jruby-dist-$jar_version-src.zip 20 jruby-$jar_version
check maven/jruby-dist/target/jruby-dist-$jar_version-bin.zip 48 jruby-$jar_version
check core/target/jruby-base-$jar_version.jar 10
check shaded/target/jruby-core-$jar_version.jar 22

exit "${failed[0]}"
