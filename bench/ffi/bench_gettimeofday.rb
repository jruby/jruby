require 'benchmark'
require 'ffi'

module Posix
  extend FFI::Library
  attach_function :gettimeofday, [ :pointer, :pointer ], :int
end
class Timeval < FFI::Struct
  layout :tv_sec => :ulong, :tv_nsec => :ulong
end

iter = 100_000
puts "Benchmark FFI gettimeofday(2) (nil, nil) performance, #{iter}x"

10.times {
  puts Benchmark.measure {
    iter.times { Posix.gettimeofday(nil, nil) }
  }
}
puts "Benchmark FFI gettimeofday(2) (Timeval.alloc_out.pointer, nil) performance, #{iter}x"

10.times {
  puts Benchmark.measure {
    iter.times { Posix.gettimeofday(Timeval.alloc_out.pointer, nil) }
  }
}
puts "Benchmark Time.now performance, #{iter}x"
10.times {
  puts Benchmark.measure {
    iter.times { Time.now }
  }
}
