require 'benchmark'
require 'ffi'

iter = ENV['ITER'] ? ENV['ITER'].to_i : 10000000

module Posix
  extend FFI::Library
  ffi_lib 'c'
  if RUBY_ENGINE == 'rbx'
    attach_function :getpid, [], :uint
  elsif FFI::Platform.windows?
    attach_function :getpid, :_getpid, [], :uint
  else
    attach_function :getpid, [], :uint, :save_errno => false
  end
end


puts "pid=#{Process.pid} Foo.getpid=#{Posix.getpid}"
puts "Benchmark FFI getpid performance, #{iter}x calls"


10.times {
  puts Benchmark.measure {
    i = 0; max = iter / 4
    while i < max
      Posix.getpid
      Posix.getpid
      Posix.getpid
      Posix.getpid
      i += 1
    end
  }
}

puts "Benchmark Process.pid performance, #{iter}x calls"
10.times {
  puts Benchmark.measure {
    i = 0; max = iter / 4
    while i < max
      Process.pid
      Process.pid
      Process.pid
      Process.pid
      i += 1
    end
  }
}
