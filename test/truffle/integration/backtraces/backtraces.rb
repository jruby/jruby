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
