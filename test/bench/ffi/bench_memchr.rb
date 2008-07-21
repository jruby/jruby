# 
# To change this template, choose Tools | Templates
# and open the template in the editor.
 
require 'benchmark'
require 'ffi'
require 'java'

module FFITest
  # memchr() doesn't make sense - the returned pointer is totally disconnected
  # to the input string, unless a direct ByteBuffer is used.
  # It is useful for testing pinned buffers though
  attach_foreign(:pointer, :memchr, [ :buffer_pinned, :char, :int ])
end
if FFITest.memchr("test", 't', 4).nil?
  raise ArgumentError, "FFI.memchr returned incorrect value"
end
puts "Benchmark FFI strlen(3) performance, 10000x"
10.times {
  puts Benchmark.measure {
    10000.times { FFITest.memchr("test", 't', 4) }
  }
}
