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
