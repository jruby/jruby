#! /bin/sh

# simple script for use with 'git bisect run'
# e.g., git bisect run tool/git_bisect_run_mspec.sh -T--1.9 spec/ruby/language/defined_spec.rb
# arguments will be passed on to mspec.

cd `dirname $0`/..
if [ ! -e spec/mspec ]; then
  ant fetch-stable-specs
fi

ant clean jar
jruby spec/mspec/bin/mspec $*
