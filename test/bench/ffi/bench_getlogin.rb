require 'benchmark'
require 'ffi'
require 'etc'

iter = 10000
module Posix
  attach_foreign(:string, :getlogin, [], :from => 'c')
end
if Posix.getlogin != Etc.getlogin
  raise ArgumentError, "FFI getlogin returned incorrect value"
end

puts "Benchmark FFI getlogin(2) performance, #{iter}x"

10.times {
  puts Benchmark.measure {
    iter.times { Posix.getlogin }
  }
}

puts "Benchmark Etc.getlogin performance, #{iter}x"
10.times {
  puts Benchmark.measure {
    iter.times { Etc.getlogin }
  }
}
