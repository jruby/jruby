require 'benchmark'
require 'ffi'

module Posix
  extend JRuby::FFI::Library
  attach_function :gettimeofday, [ :buffer_out, :pointer ], :int
end
class Timeval < JRuby::FFI::Struct
  layout :tv_sec => :time_t, :tv_nsec => :ulong
end

iter = 100_000
puts "Benchmark FFI gettimeofday(2) (nil, nil) performance, #{iter}x"

10.times {
  puts Benchmark.measure {
    iter.times { Posix.gettimeofday(nil, nil) }
  }
}
puts "Benchmark FFI gettimeofday(2) (Timeval.new.pointer, nil) performance, #{iter}x"

10.times {
  puts Benchmark.measure {
    iter.times { Posix.gettimeofday(Timeval.new.pointer, nil) }
  }
}
puts "Benchmark Time.now performance, #{iter}x"
10.times {
  puts Benchmark.measure {
    iter.times { Time.now }
  }
}
