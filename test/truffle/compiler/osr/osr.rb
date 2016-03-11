# Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
#
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

timeout = Time.now + 30

begin
  while Time.now < timeout
    Truffle::Primitive.assert_not_compiled
  end
  
  puts "while loop optimisation timed out"
  exit 1
rescue RubyTruffleError => e
  if e.message.include? 'Truffle::Primitive.assert_not_compiled'
    puts "while loop optimising"
    exit 0
  else
    puts "some other error"
    exit 1
  end
end

exit 1
