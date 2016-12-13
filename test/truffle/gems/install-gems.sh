#!/usr/bin/env bash

source test/truffle/common.sh.inc

set -e
set -x

jt ruby -rbundler-workarounds bin/gem install execjs -v 2.6.0
jt ruby -rbundler-workarounds bin/gem install rack -v 1.6.1
jt ruby -rbundler-workarounds bin/gem install tilt -v 2.0.1
jt ruby -rbundler-workarounds bin/gem install rack-protection -v 1.5.3
jt ruby -rbundler-workarounds bin/gem install sinatra -v 1.4.6
jt ruby -rbundler-workarounds bin/gem install asciidoctor -v 1.5.4
