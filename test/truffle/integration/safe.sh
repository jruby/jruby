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

#unsafe -e "puts 'hello, world'"
safe -e "14"
safe -e "Truffle::Primitive.safe_puts 'hello, world'"
unsafe -Xtruffle.platform.safe_puts=false -e "Truffle::Primitive.safe_puts 'hello, world'"

if [[ `run -e "Truffle::Primitive.safe_puts 'foo © bar'"` != 'foo ? bar' ]]
then
  echo safe_puts is not sanitising output
  exit 1
fi
