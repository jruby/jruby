#! /bin/sh

# simple script for use with 'git bisect run'
# e.g., git bisect run tool/git_bisect_run_mspec.sh <args>
# arguments will be passed on to jruby

cd `dirname $0`/..
if [ ! -e spec/mspec ]; then
  ant fetch-stable-specs
fi

ant clean jar
if [ $? -gt 0 ]; then
  exit 125
fi
./bin/jruby $*
