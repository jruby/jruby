#!/bin/bash

# Runs findbugs for the Truffle classes only. By default writes to stdout. Use
# with --report to get a HTML report.

if [[ $* == *--report* ]]
then
  ui_options="-html -output truffle-findbugs-report.html"
else
  ui_options=""
fi

mvn package || exit $?

version=3.0.0
shasum=30bdcee2c909a0eef61a4f60f4489cd2f5a4d21c
tarball=findbugs-noUpdateChecks-$version.tar.gz

if [ ! -e findbugs-$version ]
then
  wget http://prdownloads.sourceforge.net/findbugs/$tarball || exit $?
  echo "$shasum  $tarball" | shasum -c || exit $?
  tar -xf $tarball || exit $?
fi

findbugs-3.0.0/bin/findbugs \
  -maxHeap 2000 \
  -textui $ui_options \
  -exclude tool/truffle-findbugs-exclude.xml \
  -exitcode \
  -low \
  truffle/target/classes/org/jruby/truffle/

exit_code=$?
if [ $exit_code -eq 2 ]
then
  exit 0 # just some classes missing
else
  exit $exit_code
fi
