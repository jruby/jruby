# 
# To change this template, choose Tools | Templates
# and open the template in the editor.
 
require 'benchmark'
require 'ffi'
require 'posix'

puts "Benchmark POSIX.chmod performance, 10000x changing mode"
POSIX = Platform::POSIX
10.times {
  puts Benchmark.measure {
    10000.times { POSIX.chmod("README", 0622) }
  }
}
puts "Benchmark JRuby File.chmod performance, 10000x changing mode"
10.times {
  puts Benchmark.measure {
    10000.times { File.chmod(0622, "README") }
  }
}
