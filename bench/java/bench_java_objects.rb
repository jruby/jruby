require 'benchmark'
require 'java'

import java.lang.System
import org.jruby.util.ByteList

puts "Measure bytelist construct (via Java integration)"
5.times { 
  puts Benchmark.measure { 
    1000000.times { 
      ByteList.new
    } 
  }
}

puts "Measure string construct (via normal Ruby)"
5.times { 
  puts Benchmark.measure { 
    1000000.times { 
      String.new
    } 
  } 
}

