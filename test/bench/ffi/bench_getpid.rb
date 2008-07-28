# 
# To change this template, choose Tools | Templates
# and open the template in the editor.
 
 
require 'benchmark'
require 'ffi'

iter = 100000

module Posix
  attach_foreign(:int, :getpid, [ ], :from => 'c')
end


puts "pid=#{Process.pid} Foo.getpid=#{Foo.getpid}"
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
