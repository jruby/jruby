# Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
#
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

def foo
  foo = 14
  foo * 2
end

set_trace_func proc { |event, file, line, id, binding, classname|
  if event == 'line' && file == __FILE__ && line == 11
    binding.local_variable_set(:foo, 100)
  end
}

begin
  loop do
    x = foo
    raise 'value not correct' unless x == 200
    Truffle::Primitive.assert_constant x
    Truffle::Primitive.assert_not_compiled
  end
rescue RubyTruffleError => e
  if e.message.include? 'Truffle::Primitive.assert_not_compiled'
    puts 'STF optimising'
    exit 0
  elsif e.message.include? 'Truffle::Primitive.assert_constant'
    puts 'STF not optimising'
    exit 1
  else
    puts 'some other error'
    exit 1
  end
end

exit 1
