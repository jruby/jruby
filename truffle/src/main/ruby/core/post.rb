# Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
#
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

ARGV.push *Truffle.original_argv

$LOAD_PATH.push *Truffle.original_load_path

home = Truffle.jruby_home_directory_protocol
$LOAD_PATH.push home + '/lib/ruby/truffle/mri'
$LOAD_PATH.push home + '/lib/ruby/truffle/truffle'
$LOAD_PATH.push home + '/lib/ruby/truffle/rubysl/rubysl-strscan/lib'
$LOAD_PATH.push home + '/lib/ruby/truffle/rubysl/rubysl-stringio/lib'
$LOAD_PATH.push home + '/lib/ruby/truffle/rubysl/rubysl-complex/lib'
$LOAD_PATH.push home + '/lib/ruby/truffle/rubysl/rubysl-date/lib'
$LOAD_PATH.push home + '/lib/ruby/truffle/rubysl/rubysl-pathname/lib'
$LOAD_PATH.push home + '/lib/ruby/truffle/rubysl/rubysl-tempfile/lib'
$LOAD_PATH.push home + '/lib/ruby/truffle/rubysl/rubysl-socket/lib'
$LOAD_PATH.push home + '/lib/ruby/truffle/rubysl/rubysl-securerandom/lib'
$LOAD_PATH.push home + '/lib/ruby/truffle/rubysl/rubysl-timeout/lib'
$LOAD_PATH.push home + '/lib/ruby/truffle/rubysl/rubysl-webrick/lib'
$LOAD_PATH.push home + '/lib/ruby/truffle/shims'

# We defined Psych at the top level becuase several things depend on its name.
# Here we fix that up and put it back into Truffle.

Truffle::Psych = Psych

class Object
  remove_const :Psych
end
