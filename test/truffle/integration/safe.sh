#!/usr/bin/env bash

function run {
  ruby -X+T -Xtruffle.platform.safe=false "$@"
}

function safe {
  run "$@" || { echo "$@" was not safe ; exit 1; }
}

function unsafe {
  run "$@" && { echo "$@" was not unsafe ; exit ; }
}

# Things that are alway safe

safe -e "14"

# Check our safe_puts is safe

safe -e "Truffle::Primitive.safe_puts 'hello, world'"

# But we can make that unsafe as well if really don't want any output

unsafe -Xtruffle.platform.safe_puts=false -e "Truffle::Primitive.safe_puts 'hello, world'"

# Check default unsafe operations

unsafe -e "puts 'hello, world'"
unsafe -e '`echo foo`'
unsafe -e 'exit!'

# Check we can enable some unsafe operations if we want to

safe -Xtruffle.platform.safe.processes=true -e '`echo foo`'
safe -Xtruffle.platform.safe.exit=true -e 'exit'

# Check that safe_puts sanitises correctly

if [[ `run -e "Truffle::Primitive.safe_puts 'foo © bar'"` != 'foo ? bar' ]]
then
  echo safe_puts is not sanitising output
  exit 1
fi
