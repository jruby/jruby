# 
# To change this template, choose Tools | Templates
# and open the template in the editor.

require 'benchmark'
require 'kernel/platform/posix'

puts "Benchmark FFI time(3) performance, 10000x"
POSIX = Platform::POSIX
20.times {
  puts Benchmark.measure {
    10000.times { POSIX.time(nil) }
  }
}
