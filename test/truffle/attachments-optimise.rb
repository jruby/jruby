# Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
#
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

# -J-G:+TruffleCompilationExceptionsAreFatal

def foo
  foo = 14
  foo * 2
end

Truffle::Attachments.attach __FILE__, 13 do |binding|
  binding.local_variable_set(:foo, 100)
end

begin
  loop do
    x = foo
    raise "value not correct" unless x == 200
    Truffle::Primitive.assert_constant x
    Truffle::Primitive.assert_not_compiled
  end
rescue RubyTruffleError => e
  if e.message.include? 'Truffle::Primitive.assert_not_compiled'
    puts "attachments optimising"
    exit 0
  elsif e.message.include? 'Truffle::Primitive.assert_constant'
    puts "attachments not optimising"
    exit 1
  else
    puts "some other error"
    exit 1
  end
end

exit 1
