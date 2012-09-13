#!/usr/bin/env bash

# simple script for use with 'git bisect run'
# e.g., git bisect run tool/git_bisect_run.sh -T--1.9 spec/ruby/language/defined_spec.rb

PRE_COMMAND=''
POST_COMMAND=''
COMMAND='-v' # an empty $COMMAND will put the script in interactive mode, which may be confusing
V=''

function print_usage {
	echo "usage: $0 [-c command] [-p pre] [-P post] [-v]"
}

function build {
	ant clean jar
	if [ $? -gt 0 ]; then
		exit 125
	fi	
}

function print_var {
	if [[ -n $V ]]; then
		eval "VAR=\$$1"
		echo "$1: $VAR"
	fi
}

# opts
while getopts "c:hp:P:v" opt; do
  case $opt in
    c)
      COMMAND=${OPTARG}
	  if [[ -z $COMMAND ]]; then
		  echo "COMMAND is empty. Aborting."
		  exit 1
	  fi
      ;;
	h)
	  print_usage
	  exit 0
	  ;;
    p)
      PRE_COMMAND=${OPTARG}
      ;;
    P)
      POST_COMMAND=${OPTARG}
      ;;
	v)
	  V='VERBOSE'
	  ;;
  esac
done

cd `dirname $0`/..
if [ ! -e spec/mspec ]; then
  ant fetch-stable-specs
fi

print_var 'PRE_COMMAND'
${PRE_COMMAND}

build

print_var 'COMMAND'
./bin/jruby ${COMMAND}
EXIT_STATUS=$?

print_var 'POST_COMMAND'
${POST_COMMAND}
exit ${EXIT_STATUS}

