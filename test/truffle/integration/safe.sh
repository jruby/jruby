#!/usr/bin/env bash

function run {
  ruby -X+T -Xtruffle.platform.safe=true "$@"
}

function safe {
  run "$@" || ( echo "$@" was not safe ; exit 1 )
}

function unsafe {
  run "$@" && ( echo "$@" was not unsafe ; exit 1 )
}

safe -e "14"
safe -e "Truffle::Primitive.safe_puts 'hello, world'"
#unsafe -e "puts 'hello, world'"
unsafe -e '`echo foo`'
safe -Xtruffle.platform.safe.processes=true -e '`echo foo`'
unsafe -Xtruffle.platform.safe_puts=false -e "Truffle::Primitive.safe_puts 'hello, world'"

if [[ `run -e "Truffle::Primitive.safe_puts 'foo © bar'"` != 'foo ? bar' ]]
then
  echo safe_puts is not sanitising output
  exit 1
fi
