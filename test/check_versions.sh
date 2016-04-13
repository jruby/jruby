#!/bin/bash

# Checks that the artefacts produced by the JRuby build system have the correct
# names and versions, and have reasonable sizes.

jar_version=`cat VERSION`
gem_version=${jar_version/-/.}

rm -rf maven/*/target/*

./mvnw install -Pbootstrap
./mvnw -Pcomplete
./mvnw -Pdist
./mvnw -Pjruby-jars
./mvnw -Pmain

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
    echo size of $archive expected smaller then $max but got $length
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

# extended from 9 to 10 to accommodate temporary copy of stdlib 2.2.2
check lib/target/jruby-stdlib-$jar_version.jar 10
check maven/jruby-jars/pkg/jruby-jars-$gem_version.gem 30
check maven/jruby-jars/lib/jruby-core-$jar_version-complete.jar 13
# extended from 9 to 10 to accommodate temporary copy of stdlib 2.2.2
check maven/jruby-jars/lib/jruby-stdlib-$jar_version.jar 10
check maven/jruby-complete/target/jruby-complete-$jar_version.jar 27
check maven/jruby/target/jruby-$jar_version.jar 9
check maven/jruby-dist/target/jruby-dist-$jar_version-bin.tar.gz 45 jruby-$jar_version
check maven/jruby-dist/target/jruby-dist-$jar_version-bin200.tar.gz 20 jruby-$jar_version
# extended from 15 to 20 to accommodate temporary copy of stdlib 2.2.2
check maven/jruby-dist/target/jruby-dist-$jar_version-src.zip 20 jruby-$jar_version
check maven/jruby-dist/target/jruby-dist-$jar_version-bin.zip 45 jruby-$jar_version
check core/target/jruby-core-$jar_version.jar 9
check truffle/target/jruby-truffle-$jar_version.jar 15

exit "${failed[0]}"
