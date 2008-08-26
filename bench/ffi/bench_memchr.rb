require 'benchmark'
require 'ffi'
require 'java'
iter = 10000
str = "test" * 1000
module JLibC
  extend FFI::Library
  attach_function :memchr, [ :string, :char, :int ], :pointer
end

if JLibC.memchr("test", 't', 4).nil?
  raise ArgumentError, "JRuby::FFI.memchr returned incorrect value"
end

puts "Benchmark FFI memchr(3) performance, #{iter}x"
10.times {
  puts Benchmark.measure {
    iter.times { JLibC.memchr(str, 't', 4) }
  }
}
