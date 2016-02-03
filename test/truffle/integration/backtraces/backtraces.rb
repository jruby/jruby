# Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
#
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

def check(expected)
  begin
    yield
  rescue Exception => exception
    actual = exception.backtrace
  end
  
  while actual.size < expected.size
    actual.push '(missing)'
  end
  
  while expected.size < actual.size
    expected.push '(missing)'
  end
  
  success = true
  
  expected.zip(actual).each do |e, a|
    unless a.end_with?(e)
      puts "Expected #{e}, actually #{a}"
      success = false
    end
  end
  
  unless success
    exit 1
  end
end

def m1(count)
  if count.zero?
    raise 'm1-message'
  else
    m1(count - 1)
  end
end

expected = [
  "/backtraces.rb:40:in `m1'",
  "/backtraces.rb:42:in `m1'",
  "/backtraces.rb:42:in `m1'",
  "/backtraces.rb:42:in `m1'",
  "/backtraces.rb:42:in `m1'",
  "/backtraces.rb:42:in `m1'",
  "/backtraces.rb:59:in `block in <main>'",
  "/backtraces.rb:11:in `check'",
  "/backtraces.rb:58:in `<main>'"
]

check(expected) do
  m1(5)
end

expected = [
  "/backtraces.rb:85:in `block (5 levels) in <main>'",
  "/backtraces.rb:77:in `m2'",
  "/backtraces.rb:84:in `block (4 levels) in <main>'",
  "/backtraces.rb:83:in `tap'",
  "/backtraces.rb:83:in `block (3 levels) in <main>'",
  "/backtraces.rb:82:in `each'",
  "/backtraces.rb:82:in `block (2 levels) in <main>'",
  "/backtraces.rb:81:in `each'",
  "/backtraces.rb:81:in `block in <main>'",
  "/backtraces.rb:11:in `check'",
  "/backtraces.rb:80:in `<main>'"
]

def m2
  yield
end

check(expected) do
  [1].each do |n|
    {a: 1}.each do |k, v|
      true.tap do |t|
        m2 do
          raise 'm2-message'
        end
      end
    end
  end
end

js = 'application/javascript'

if defined?(Truffle) && Truffle::Interop.mime_type_supported?(js)
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

  check(expected) do
    bob(self)
  end
else
  puts "JavaScript doesn't appear to be available - skipping polylgot backtrace tests"
end
