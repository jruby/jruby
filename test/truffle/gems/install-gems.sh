#!/usr/bin/env bash

set -e
set -x

bin/jruby bin/gem install execjs -v 2.6.0
bin/jruby bin/gem install rack -v 1.6.1
bin/jruby bin/gem install tilt -v 2.0.1
bin/jruby bin/gem install rack-protection -v 1.5.3
bin/jruby bin/gem install sinatra -v 1.4.6
bin/jruby bin/gem install asciidoctor -v 1.5.4
