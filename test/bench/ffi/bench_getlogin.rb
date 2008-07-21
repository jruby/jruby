# 
# To change this template, choose Tools | Templates
# and open the template in the editor.
 
require 'benchmark'
require 'ffi'
require 'etc'

module Foo
  attach_foreign(:string, :getlogin, [])
end
if Foo.getlogin != Etc.getlogin
  raise ArgumentError, "FFI getlogin returned wrong value"
end
puts "FFI getlogin returns '#{Foo.getlogin}'"
puts "Benchmark Etc.getlogin performance, 10000x"
10.times {
  puts Benchmark.measure {
    10000.times { Etc.getlogin }
  }
}
puts "Benchmark FFI getlogin(2) performance, 10000x"

20.times {
  puts Benchmark.measure {
    10000.times { Foo.getlogin }
  }
}
