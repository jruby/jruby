# 
# To change this template, choose Tools | Templates
# and open the template in the editor.
 
 
require 'benchmark'
require 'ffi'

module Foo
  attach_function('getpid', [ ], :int)
end

iter = 100000
puts "pid=#{Process.pid} Foo.getpid=#{Foo.getpid}"
puts "Benchmark FFI getpid performance, #{iter}x calls"
max_threads = 1

10.times {
  puts Benchmark.measure {
    iter.times { Foo.getpid }
  }
}
puts "Benchmark Process.pid performance, #{iter}x calls"
10.times {
  puts Benchmark.measure {
    iter.times { Process.pid }
  }
}
