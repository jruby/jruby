require 'benchmark'
require 'ffi'

class S < FFI::Struct
  layout :a, :int, :b, :int
end

iter = 1000_000
puts "Benchmark FFI struct field get performance, #{iter}x"

10.times {
  puts Benchmark.measure {
    s = S.new(FFI::Buffer.new(S))
    iter.times { s[:a] }
  }
}

10.times {
  puts Benchmark.measure {
    s = S.new(FFI::Buffer.new(S))
    iter.times { s['a'] }
  }
}

