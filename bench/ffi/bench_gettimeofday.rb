require 'benchmark'
require 'ffi'

module Posix
  extend FFI::Library
  ffi_lib FFI::Platform::LIBC
  attach_function :gettimeofday, [ :buffer_out, :pointer ], :int
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
puts "Benchmark FFI gettimeofday(2) (Timeval.alloc_out, nil) performance, #{iter}x"

10.times {
  puts Benchmark.measure {
    iter.times { Posix.gettimeofday(Timeval.alloc_out, nil) }
  }
}
puts "Benchmark Time.now performance, #{iter}x"
10.times {
  puts Benchmark.measure {
    iter.times { Time.now }
  }
}
