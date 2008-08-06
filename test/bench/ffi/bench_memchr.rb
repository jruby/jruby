require 'benchmark'
require 'ffi'
require 'java'
iter = 10000
str = "test" * 1000
module JLibC
  extend JRuby::FFI::Library
  attach_function :memchr, [ :string, :char, :int ], :pointer
end

module RbxLibC
  extend FFI::Library
  attach_function :memchr, [ :string, :char, :int ], :pointer
end

if JLibC.memchr("test", 't', 4).nil?
  raise ArgumentError, "JRuby::FFI.memchr returned incorrect value"
end
if RbxLibC.memchr("test", 't', 4).nil?
  raise ArgumentError, "FFI.memchr returned incorrect value"
end
puts "Benchmark FFI memchr(3) (rubinius api) performance, #{iter}x"
10.times {
  puts Benchmark.measure {
    iter.times { RbxLibC.memchr(str, 't', 4) }
  }
}
puts "Benchmark FFI memchr(3) (jruby api) performance, #{iter}x"
10.times {
  puts Benchmark.measure {
    iter.times { JLibC.memchr(str, 't', 4) }
  }
}
