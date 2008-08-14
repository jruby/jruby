require 'benchmark'
require 'ffi'

iter = 100000

module Posix
  extend FFI::Library
  attach_function :getuid, [], :uid_t
end


puts "uid=#{Process.pid} Posix.getuid=#{Posix.getuid}"
puts "Benchmark FFI getuid performance, #{iter}x calls"

10.times {
  puts Benchmark.measure {
    iter.times { Posix.getuid }
  }
}
puts "Benchmark Process.uid performance, #{iter}x calls"
10.times {
  puts Benchmark.measure {
    iter.times { Process.uid }
  }
}
