require 'benchmark'
require 'ffi'

module Posix
  extend FFI::Library
  attach_function :time, [ :pointer ], :ulong
end

iter = 1000_000
puts "Benchmark FFI time(3) performance, #{iter}x"

10.times {
  puts Benchmark.measure {
    iter.times { Posix.time(nil) }
  }
}
puts "Benchmark Time.now performance, #{iter}x"
10.times {
  puts Benchmark.measure {
    iter.times { Time.now }
  }
}
