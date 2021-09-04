require 'benchmark'
require 'ffi'
iter = 10000
str = "test" * 1000
module LibC
  extend FFI::Library
  ffi_lib FFI::Platform::LIBC
  attach_function :memchr, [ :string, :char, :int ], :pointer
end

if LibC.memchr("test", 't', 4).nil?
  raise ArgumentError, "FFI memchr returned incorrect value"
end

puts "Benchmark FFI memchr(3) performance, #{iter}x"
10.times {
  puts Benchmark.measure {
    iter.times { LibC.memchr(str, 't', 4) }
  }
}
