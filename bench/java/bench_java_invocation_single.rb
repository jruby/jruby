require 'benchmark'
require 'java'

puts "Measure bytelist length (via Java integration)"
5.times { 
  puts Benchmark.measure { 
    sb = org.jruby.util.ByteList.plain("foobar") 
    1000000.times { 
      sb.length
    } 
  }
}

puts "Measure string length (via normal Ruby)"
5.times { 
  puts Benchmark.measure { 
    str = "foobar" 
    1000000.times { 
      str.length 
    } 
  } 
}
