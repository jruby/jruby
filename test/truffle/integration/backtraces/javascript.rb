# Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
#
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

require_relative 'backtraces'

js = 'application/javascript'

unless defined?(Truffle) && Truffle::Interop.mime_type_supported?(js)
  puts "JavaScript doesn't appear to be available - skipping polylgot backtrace tests"
  exit
end

def foo(_)
  raise 'foo-message'
end

Truffle::Interop.export_method :foo
Truffle::Interop.eval js, "foo = Interop.import('foo')"

Truffle::Interop.eval js, "function bar() { foo(); }"

Truffle::Interop.eval js, "Interop.export('bar', bar)"
Truffle::Interop.import_method :bar

def baz(_)
  bar(self)
end

Truffle::Interop.export_method :baz
Truffle::Interop.eval js, "baz = Interop.import('baz')"

Truffle::Interop.eval js, "function bob() { baz(); }"

Truffle::Interop.eval js, "Interop.export('bob', bob)"
Truffle::Interop.import_method :bob

expected = [
  "/backtraces.rb:96:in `foo'",
  "(eval):1",
  "/backtraces.rb:108:in `bar'",
  "/backtraces.rb:108:in `baz'",
  "(eval):1",
  "/backtraces.rb:132:in `bob'",
  "/backtraces.rb:132:in `block in <main>'",
  "/backtraces.rb:11:in `check'",
  "/backtraces.rb:131:in `<main>'"
]

check('javascript.backtrace') do
  bob(self)
end
