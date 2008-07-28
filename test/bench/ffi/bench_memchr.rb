require 'benchmark'
require 'ffi'
require 'java'
iter = 10000
module LibC
  # This is one of the few places where a pinned buffer can be used, since memchr
  # will not block on some outside resource
  jffi_attach(:pointer, :memchr, [ :buffer_pinned, :char, :int ])
end
if LibC.memchr("test", 't', 4).nil?
  raise ArgumentError, "FFI.memchr returned incorrect value"
end
puts "Benchmark FFI memchr(3) performance, #{iter}x"
10.times {
  puts Benchmark.measure {
    iter.times { LibC.memchr("test", 't', 4) }
  }
}
