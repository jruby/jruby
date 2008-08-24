require 'benchmark'
require 'ffi'

iter = 1000000

module Posix
  extend FFI::Library
  if JRuby::FFI::Platform::IS_WINDOWS
    attach_function :_getpid, :getpid, [], :pid_t
  else
    attach_function :getpid, [], :pid_t
  end
end


puts "pid=#{Process.pid} Foo.getpid=#{Posix.getpid}"
puts "Benchmark FFI getpid performance, #{iter}x calls"


10.times {
  puts Benchmark.measure {
    iter.times { Posix.getpid }
  }
}
puts "Benchmark Process.pid performance, #{iter}x calls"
10.times {
  puts Benchmark.measure {
    iter.times { Process.pid }
  }
}
